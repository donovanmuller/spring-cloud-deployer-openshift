package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public abstract class DockerfileWithDockerBuildConfigStrategy extends BuildConfigStrategy {

	private BuildConfigFactory buildConfigFactory;
	private OpenShiftDeployerProperties openShiftDeployerProperties;

	public DockerfileWithDockerBuildConfigStrategy(BuildConfigFactory buildConfigFactory,
			OpenShiftDeployerProperties openShiftDeployerProperties, OpenShiftClient client,
			Map<String, String> labels) {
		super(buildConfigFactory, client, labels);
		this.buildConfigFactory = buildConfigFactory;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
	}

	@Override
	protected BuildConfig buildBuildConfig(AppDeploymentRequest request, String appId, Map<String, String> labels) {
		//@formatter:off
        return new BuildConfigBuilder(buildConfigFactory.buildBuildConfig(request, appId, labels))
                .editSpec()
                .withNewSource()
					.withType("Dockerfile")
                    .withDockerfile(getDockerfile(request, openShiftDeployerProperties))
                .endSource()
                .withNewStrategy()
                    .withType("Docker")
                    .withNewDockerStrategy()
					.endDockerStrategy()
                .endStrategy()
                .withNewOutput()
                    .withNewTo()
                        .withKind("ImageStreamTag")
                        .withName(buildConfigFactory.getImageTag(request, openShiftDeployerProperties, appId))
                    .endTo()
                .endOutput()
            .endSpec()
            .build();
        //@formatter:on
	}

	/**
	 * Determine the Dockerfile (see https://docs.docker.com/engine/reference/builder/) source. The
	 * following sources are considered, in this order:
	 * <p>
	 * <ul>
	 * <li>spring.cloud.deployer.openshift.build.dockerfile deployment property. This can be an
	 * inline Dockerfile definition or a path to a Dockerfile on the file system</li>
	 * <li>The default Dockerfile bundled with the OpenShift deployer. See
	 * src/main/resources/Dockerfile</li>
	 * </ul>
	 *
	 * @param request
	 * @return an inline Dockerfile definition
	 */
	protected abstract String getDockerfile(AppDeploymentRequest request, OpenShiftDeployerProperties properties);
}
