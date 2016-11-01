package org.springframework.cloud.deployer.spi.openshift;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.maven.VolumeMountProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;

public class OpenShiftContainerFactory extends DefaultContainerFactory implements OpenShiftSupport {

	private static final Logger logger = LoggerFactory.getLogger(OpenShiftContainerFactory.class);

	private static String LIVENESS_ENDPOINT = "/health";
	private static String READINESS_ENDPOINT = "/info";

	private KubernetesDeployerProperties properties;
	private ConfigServicePropertySourceLocator configServicePropertySourceLocator;

	public OpenShiftContainerFactory(KubernetesDeployerProperties properties,
			ConfigServicePropertySourceLocator configServicePropertySourceLocator) {
		super(properties);
		this.properties = properties;
		this.configServicePropertySourceLocator = configServicePropertySourceLocator;
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
	 * @return a {@link Container}
	 */
	@Override
	public Container create(String appId, AppDeploymentRequest request, Integer port, Integer instanceIndex) {
		Container container;

		if (request.getResource() instanceof DockerResource) {
			container = super.create(appId, request, port, instanceIndex);
			container.setVolumeMounts(getVolumeMounts(appId, request.getDeploymentProperties()));
		}
		else {
			//@formatter:off
			ContainerBuilder containerBuilder = new ContainerBuilder();
            containerBuilder
                    .withName(appId)
                    .withImage(appId)
                    .withEnv(toEnvVars(properties.getEnvironmentVariables()))
                    .withArgs(createCommandArgs(request))
					.withVolumeMounts(getVolumeMounts(appId, request.getDeploymentProperties()));

            if (port != null) {
                containerBuilder.addNewPort()
						.withContainerPort(port)
					.endPort()
					.withReadinessProbe(
							createProbe(port,
									READINESS_ENDPOINT,
									properties.getReadinessProbeTimeout(),
									properties.getReadinessProbeDelay(),
									properties.getReadinessProbePeriod()))
					.withLivenessProbe(
							createProbe(port,
									LIVENESS_ENDPOINT,
									properties.getLivenessProbeTimeout(),
									properties.getLivenessProbeDelay(),
									properties.getLivenessProbePeriod()));
			}
			//@formatter:on

			container = containerBuilder.build();
		}

		return container;
	}

	// TODO extract the configServicePropertySourceLocator stuff into it's own class
	protected List<VolumeMount> getVolumeMounts(String appId, Map<String, String> deploymentProperties) {
		List<VolumeMount> volumeMounts = new ArrayList<>();

		ConfigurableEnvironment appEnvironment = new StandardEnvironment();
		appEnvironment.getPropertySources().addFirst(
				new MapPropertySource("deployer-override", Collections.singletonMap("spring.application.name", appId)));

		PropertySource<?> propertySource = configServicePropertySourceLocator.locate(appEnvironment);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource);

		// TODO check of properties are merged/overridden
		VolumeMountProperties volumeMountProperties = getVolumeMountProperties(deploymentProperties);
		volumeMounts.addAll(volumeMountProperties.getVolumeMounts());
		PropertiesConfigurationFactory<VolumeMountProperties> factory = new PropertiesConfigurationFactory<>(
				volumeMountProperties);
		factory.setPropertySources(propertySources);

		try {
			factory.afterPropertiesSet();
			VolumeMountProperties configVolumeProperties = factory.getObject();
			volumeMounts.addAll(configVolumeProperties.getVolumeMounts());
		}
		catch (Exception e) {
			logger.warn("Could not get app '{}' configuration from config server: '{}'", appId, e.getMessage());
		}

		return volumeMounts;
	}
}
