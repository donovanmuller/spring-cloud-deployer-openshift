package org.springframework.cloud.deployer.spi.openshift.factories;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class DockerfileWithDockerBuildConfigFactory extends BuildConfigFactory {

	public DockerfileWithDockerBuildConfigFactory(OpenShiftClient client,
			Map<String, String> labels, GitReference gitReference,
			KubernetesDeployerProperties properties, MavenProperties mavenProperties,
			ResourceHash resourceHash) {
		super(client, labels, gitReference, properties, mavenProperties, resourceHash);
	}

	@Override
	protected BuildConfig buildBuildConfig(String appId, AppDeploymentRequest request,
			GitReference gitReference, Map<String, String> labels, String hash) {
		BuildConfig buildConfig = super.buildBuildConfig(appId, request, gitReference,
				labels, hash);
		//@formatter:off
        return new BuildConfigBuilder(buildConfig)
                .editSpec()
                .withNewSource()
					.withType("Dockerfile")
                    .withDockerfile(getDockerfile(request))
                .endSource()
                .withNewStrategy()
                    .withType("Docker")
                    .withNewDockerStrategy()
					.endDockerStrategy()
                .endStrategy()
                .withNewOutput()
                    .withNewTo()
                        .withKind("ImageStreamTag")
                        .withName(format("%s:%s", appId, request.getDeploymentProperties()
							.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG,
								"latest")))
                    .endTo()
                .endOutput()
            .endSpec()
            .build();
        //@formatter:on
	}

	/**
	 * Determine the Dockerfile (see https://docs.docker.com/engine/reference/builder/)
	 * source. The following sources are considered, in this order:
	 *
	 * <ul>
	 * <li>spring.cloud.deployer.openshift.build.dockerfile deployment property. This can
	 * be an inline Dockerfile definition or a path to a Dockerfile on the file system
	 * </li>
	 * <li>The default Dockerfile bundled with the OpenShift deployer. See
	 * src/main/rsources/Dockerfile</li>
	 * </ul>
	 *
	 * @param request
	 * @return an inline Dockerfile definition
	 */
	protected String getDockerfile(AppDeploymentRequest request) {
		String dockerFile = request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE);
		try {
			if (StringUtils.isNotBlank(dockerFile)) {
				if (new File(dockerFile).exists()) {
					dockerFile = resourceToString(new FileSystemResource(dockerFile));
				}
			}
			else {
				dockerFile = resourceToString(new ClassPathResource("Dockerfile"));
			}
		}
		catch (IOException e) {
			throw new RuntimeException("Could not read default Dockerfile", e);
		}

		return dockerFile;
	}

	private String resourceToString(Resource resource) throws IOException {
		return new String(FileCopyUtils.copyToByteArray(resource.getInputStream()));
	}
}
