package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.volumes.VolumeMountFactory;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;

public class OpenShiftContainerFactory extends DefaultContainerFactory implements OpenShiftSupport {

	private static String LIVENESS_ENDPOINT = "/health";
	private static String READINESS_ENDPOINT = "/info";

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
	 * @return a {@link Container}
	 */
	@Override
	public Container create(String appId, AppDeploymentRequest request, Integer port, Integer instanceIndex) {
		Container container;

		if (request.getResource() instanceof DockerResource) {
			container = super.create(appId, request, port, instanceIndex);
			container.setVolumeMounts(volumeMountFactory.create(appId, request.getDeploymentProperties()));
		}
		else {
			//@formatter:off
			ContainerBuilder containerBuilder = new ContainerBuilder();
            containerBuilder
                    .withName(appId)
                    .withImage(appId)
                    .withEnv(toEnvVars(properties.getEnvironmentVariables()))
                    .withArgs(createCommandArgs(request))
					.withVolumeMounts(volumeMountFactory.create(appId, request.getDeploymentProperties()));

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
}
