package org.springframework.cloud.deployer.spi.openshift.resources.pod;

import static org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_CONTAINER_COMMAND;
import static org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_CONTAINER_ENTRYPOINT_STYLE;
import static org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_CONTAINER_PORTS;
import static org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_ENVIRONMENT_VARIABLES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.EntryPointStyle;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.cloud.deployer.spi.util.CommandLineTokenizer;
import org.springframework.util.Assert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;

public class OpenShiftContainerFactory extends DefaultContainerFactory implements OpenShiftSupport {

	private KubernetesDeployerProperties properties;
	private VolumeMountFactory volumeMountFactory;

	public OpenShiftContainerFactory(KubernetesDeployerProperties properties, VolumeMountFactory volumeMountFactory) {
		super(properties);
		this.properties = properties;
		this.volumeMountFactory = volumeMountFactory;
	}

	/**
	 * Containers for {@link DockerResource}'s are delegated to the {@link DefaultContainerFactory},
	 * any other container, like a
	 * {@link org.springframework.cloud.deployer.resource.maven.MavenResource} for example, are
	 * created with the image as the application Id because the Docker image would have been built
	 * and pushed to the OpenShift internal registry. See
	 * https://docs.openshift.org/latest/architecture/infrastructure_components/
	 * image_registry.html#integrated-openshift-registry
	 *
	 * @param appId
	 * @param request
	 * @param port
	 * @param instanceIndex the app instance index. Used with <code>app.xxx.count</code>
	 * @return a {@link Container}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Container create(String appId, AppDeploymentRequest request, Integer port, Integer instanceIndex) {
		Container container;

		if (request.getResource() instanceof DockerResource) {
			container = super.create(appId, request, port, instanceIndex);
			container.setVolumeMounts(volumeMountFactory.addObject(request, appId));
		}
		else {
			EntryPointStyle entryPointStyle = determineEntryPointStyle(properties, request);
			// TODO needs some cleanup for handling entrypoints environment variable handling
			List<EnvVar> environmentVariables = toEnvVars(properties.getEnvironmentVariables(),
					getAppEnvironmentVariables(request));
			environmentVariables
					.addAll(createEnvironmentVariablesForEntryPoint(request, entryPointStyle, environmentVariables));

			//@formatter:off
			ContainerBuilder containerBuilder = new ContainerBuilder();
			containerBuilder
                    .withName(appId)
                    .withImage(appId)
                    .withEnv(environmentVariables)
                    .withArgs(createCommandArgsForEntryPoint(request, entryPointStyle))
					.withVolumeMounts(volumeMountFactory.addObject(request, appId));
            if (port != null) {
                containerBuilder.addNewPort()
						.withContainerPort(port)
					.endPort()
					.withReadinessProbe(
							createProbe(port,
									properties.getReadinessProbePath(),
									properties.getReadinessProbeTimeout(),
									properties.getReadinessProbeDelay(),
									properties.getReadinessProbePeriod()))
					.withLivenessProbe(
							createProbe(port,
									properties.getLivenessProbePath(),
									properties.getLivenessProbeTimeout(),
									properties.getLivenessProbeDelay(),
									properties.getLivenessProbePeriod()));
			}
			//@formatter:on

			// Add additional specified ports. Further work is needed to add probe customization for
			// each port.
			List<Integer> additionalPorts = getContainerPorts(request);
			if (!additionalPorts.isEmpty()) {
				for (Integer containerPort : additionalPorts) {
					containerBuilder.addNewPort().withContainerPort(containerPort).endPort();
				}
			}

			// Override the containers default entry point with one specified during the app
			// deployment
			List<String> containerCommand = getContainerCommand(request);
			if (!containerCommand.isEmpty()) {
				containerBuilder.withCommand(containerCommand);
			}

			container = containerBuilder.build();
		}

		return container;
	}

	private List<String> createCommandArgsForEntryPoint(AppDeploymentRequest request, EntryPointStyle entryPointStyle) {
		List<String> appArgs = new ArrayList<>();
		switch (entryPointStyle) {
		case exec:
			appArgs = createCommandArgs(request);
			break;
		}

		return appArgs;
	}

	private List<EnvVar> createEnvironmentVariablesForEntryPoint(AppDeploymentRequest request,
			EntryPointStyle entryPointStyle, List<EnvVar> environmentVariables) {
		switch (entryPointStyle) {
		case boot:
			if (environmentVariables.stream().anyMatch(envVar -> envVar.getName().equals("SPRING_APPLICATION_JSON"))) {
				throw new IllegalStateException(
						"You can't use boot entry point style and also set SPRING_APPLICATION_JSON for the app");
			}
			try {
				environmentVariables.add(new EnvVar("SPRING_APPLICATION_JSON",
						new ObjectMapper().writeValueAsString(request.getDefinition().getProperties()), null));
			}
			catch (JsonProcessingException e) {
				throw new IllegalStateException("Unable to create SPRING_APPLICATION_JSON", e);
			}
			break;
		case shell:
			for (String key : request.getDefinition().getProperties().keySet()) {
				String envVar = key.replace('.', '_').toUpperCase();
				environmentVariables.add(new EnvVar(envVar, request.getDefinition().getProperties().get(key), null));
			}
			break;
		}

		return environmentVariables;
	}

	private Map<String, String> getAppEnvironmentVariables(AppDeploymentRequest request) {
		Map<String, String> appEnvVarMap = new HashMap<>();
		String appEnvVar = request.getDeploymentProperties().getOrDefault(OPENSHIFT_DEPLOYMENT_ENVIRONMENT_VARIABLES,
				StringUtils.EMPTY);
		if (StringUtils.isNotBlank(appEnvVar)) {
			String[] appEnvVars = appEnvVar.split(",");
			for (String envVar : appEnvVars) {
				String[] strings = envVar.split("=", 2);
				Assert.isTrue(strings.length == 2, "Invalid environment variable declared: " + envVar);
				appEnvVarMap.put(strings[0], strings[1]);
			}
		}
		return appEnvVarMap;
	}

	private List<String> getContainerCommand(AppDeploymentRequest request) {
		String containerCommand = request.getDeploymentProperties().getOrDefault(OPENSHIFT_DEPLOYMENT_CONTAINER_COMMAND,
				StringUtils.EMPTY);
		return new CommandLineTokenizer(containerCommand).getArgs();
	}

	private List<Integer> getContainerPorts(AppDeploymentRequest request) {
		List<Integer> containerPortList = new ArrayList<>();
		String containerPorts = request.getDeploymentProperties().get(OPENSHIFT_DEPLOYMENT_CONTAINER_PORTS);
		if (containerPorts != null) {
			String[] containerPortSplit = containerPorts.split(",");
			for (String containerPort : containerPortSplit) {
				Integer port = Integer.parseInt(containerPort.trim());
				containerPortList.add(port);
			}
		}
		return containerPortList;
	}

	private EntryPointStyle determineEntryPointStyle(KubernetesDeployerProperties properties,
			AppDeploymentRequest request) {
		EntryPointStyle entryPointStyle = null;
		String deployProperty = request.getDeploymentProperties()
				.getOrDefault(OPENSHIFT_DEPLOYMENT_CONTAINER_ENTRYPOINT_STYLE, properties.getEntryPointStyle().name());
		if (deployProperty != null) {
			entryPointStyle = EntryPointStyle.valueOf(deployProperty.toLowerCase());
		}
		return entryPointStyle;
	}
}
