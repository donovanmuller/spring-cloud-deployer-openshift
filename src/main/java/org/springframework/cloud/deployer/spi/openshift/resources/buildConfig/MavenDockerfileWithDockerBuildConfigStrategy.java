package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import io.fabric8.openshift.client.OpenShiftClient;

public class MavenDockerfileWithDockerBuildConfigStrategy extends DockerfileWithDockerBuildConfigStrategy {
	public MavenDockerfileWithDockerBuildConfigStrategy(BuildConfigFactory buildConfigFactory,
			OpenShiftDeployerProperties openShiftDeployerProperties, OpenShiftClient client,
			Map<String, String> labels) {
		super(buildConfigFactory, openShiftDeployerProperties, client, labels);
	}

	@Override
	protected String getDockerfile(AppDeploymentRequest request, OpenShiftDeployerProperties properties) {
		String dockerFile = request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE);
		try {
			if (StringUtils.isNotBlank(dockerFile)) {
				if (new File(dockerFile).exists()) {
					dockerFile = resourceToString(new FileSystemResource(dockerFile));
				}
			}
			else {
				dockerFile = resourceToString(new ClassPathResource(request.getDeploymentProperties().getOrDefault(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DEFAULT_DOCKERFILE,
						properties.getDefaultDockerfile())));
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Could not read default Dockerfile.artifactory", e);
		}

		return dockerFile;
	}

	private String resourceToString(Resource resource) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
	}
}
