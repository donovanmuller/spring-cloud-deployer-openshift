package org.springframework.cloud.deployer.spi.openshift.factories;

import static java.lang.String.format;

import java.util.Map;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftApplicationPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.util.StringUtils;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class GitWithDockerBuildConfigFactory extends BuildConfigFactory {

	private final KubernetesDeployerProperties properties;

	public GitWithDockerBuildConfigFactory(OpenShiftClient client,
			Map<String, String> labels, GitReference gitReference,
			KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			MavenProperties mavenProperties, ResourceHash resourceHash) {
		super(client, labels, gitReference, properties, openShiftDeployerProperties,
				mavenProperties, resourceHash);
		this.properties = properties;
	}

	protected BuildConfig buildBuildConfig(String appId, AppDeploymentRequest request,
			GitReference gitReference, Map<String, String> labels, String hash) {
		BuildConfig buildConfig = super.buildBuildConfig(appId, request, gitReference,
				labels, hash);

		//@formatter:off
		buildConfig = new BuildConfigBuilder(buildConfig)
			.editSpec()
				.withNewSource()
					.withType("Git")
					.withNewGit()
						.withUri(gitReference.getParsedUri())
						.withRef(gitReference.getBranch())
					.endGit()
					.withContextDir(getContextDirectory(request))
				.endSource()
			.endSpec()
			.build();
		//@formatter:on

		String sourceSecret = getGitSourceSecret(request);
		if (StringUtils.hasText(sourceSecret)) {
			//@formatter:off
			buildConfig = new BuildConfigBuilder(buildConfig)
				.editSpec()
					.editSource()
						.withNewSourceSecret(getGitSourceSecret(request))
					.endSource()
				.endSpec()
			.build();
			//@formatter:on
		}

		//@formatter:off
        return new BuildConfigBuilder(buildConfig)
            .editSpec()
                .withNewStrategy()
                    .withType("Docker")
                    .withNewDockerStrategy()
					.endDockerStrategy()
				.endStrategy()
                .withNewOutput()
                    .withNewTo()
                        .withKind("ImageStreamTag")
                        .withName(format("%s:%s", appId, "latest"))
                    .endTo()
                .endOutput()
            .endSpec()
            .build();
        //@formatter:on
	}

	/**
	 * Get the source context directory, the path where the Dockerfile is expected.
	 * Defaults to src/main/docker
	 *
	 * @param request
	 * @return the context directory/path where the Dockerfile is expected
	 */
	protected String getContextDirectory(AppDeploymentRequest request) {
		return request.getDefinition().getProperties().getOrDefault(
				OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH,
				"src/main/docker");
	}

	/**
	 * Attempt to get the Secret from the app deployment properties or from the deployer
	 * environment variables. See
	 * https://docs.openshift.org/latest/dev_guide/builds.html#using-secrets
	 *
	 * @param request
	 * @return a Secret if there is one available
	 */
	protected String getGitSourceSecret(AppDeploymentRequest request) {
		return request.getDefinition().getProperties().getOrDefault(
				OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_SOURCE_SECRET,
				getEnvironmentVariable(properties.getEnvironmentVariables(),
						OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_SOURCE_SECRET));
	}
}
