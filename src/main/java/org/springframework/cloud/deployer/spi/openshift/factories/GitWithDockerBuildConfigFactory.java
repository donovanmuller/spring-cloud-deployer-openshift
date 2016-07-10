package org.springframework.cloud.deployer.spi.openshift.factories;

import static java.lang.String.format;

import java.util.Map;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class GitWithDockerBuildConfigFactory extends BuildConfigFactory {

	private final KubernetesDeployerProperties properties;

	public GitWithDockerBuildConfigFactory(OpenShiftClient client,
			Map<String, String> labels, GitReference gitReference,
			KubernetesDeployerProperties properties, MavenProperties mavenProperties,
			ResourceHash resourceHash) {
		super(client, labels, gitReference, properties, mavenProperties, resourceHash);
		this.properties = properties;
	}

	protected BuildConfig buildBuildConfig(String appId, AppDeploymentRequest request,
			GitReference gitReference, Map<String, String> labels, String hash) {
		BuildConfig buildConfig = super.buildBuildConfig(appId, request, gitReference,
				labels, hash);
		//@formatter:off
        return new BuildConfigBuilder(buildConfig)
            .editSpec()
                .withNewSource()
                    .withType("Git")
                    .withNewGit()
                        .withUri(gitReference.getParsedUri())
                        .withRef(gitReference.getBranch())
                    .endGit()
                    .withContextDir(getContextDirectory(request))
                    .withNewSourceSecret(getGitSourceSecret(request))
                .endSource()
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
		return request.getDeploymentProperties().getOrDefault(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH,
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
		return request.getDeploymentProperties().getOrDefault(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_BUILD_GIT_SOURCE_SECRET,
				getEnvironmentVariable(properties.getEnvironmentVariables(),
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_BUILD_GIT_SOURCE_SECRET));
	}
}
