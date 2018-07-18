package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import io.fabric8.openshift.client.OpenShiftClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class MavenDockerfileWithDockerBuildConfigStrategy
		extends DockerfileWithDockerBuildConfigStrategy {

	public MavenDockerfileWithDockerBuildConfigStrategy(
			BuildConfigFactory buildConfigFactory,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			OpenShiftClient client, Map<String, String> labels) {
		super(buildConfigFactory, openShiftDeployerProperties, client, labels);
	}

	@Override
	protected String getDockerfile(AppDeploymentRequest request,
			OpenShiftDeployerProperties properties) {
		String dockerFile = request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE);
		try {
			if (StringUtils.isNotBlank(dockerFile)) {
				if (new File(dockerFile).exists()) {
					dockerFile = resourceToString(new FileSystemResource(dockerFile));
				}
			}
		}
		catch (IOException e) {
			throw new RuntimeException(
					String.format("Could not read Dockerfile at %s", dockerFile), e);
		}

		return dockerFile;
	}

	private String resourceToString(Resource resource) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
	}

}
