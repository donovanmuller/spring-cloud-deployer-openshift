package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftApplicationPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftMavenDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenResourceJarExtractor;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class BuildStrategies {

	private static final Logger logger = LoggerFactory.getLogger(BuildStrategies.class);

	private MavenProperties mavenProperties;

	private OpenShiftDeployerProperties deployerProperties;

	private MavenResourceJarExtractor mavenResourceJarExtractor;

	private ResourceHash resourceHash;

	private OpenShiftClient client;

	public BuildStrategies(MavenProperties mavenProperties,
			OpenShiftDeployerProperties deployerProperties,
			MavenResourceJarExtractor mavenResourceJarExtractor,
			ResourceHash resourceHash, OpenShiftClient client) {
		this.mavenProperties = mavenProperties;
		this.deployerProperties = deployerProperties;
		this.mavenResourceJarExtractor = mavenResourceJarExtractor;
		this.resourceHash = resourceHash;
		this.client = client;
	}

	public BuildConfigStrategy chooseBuildStrategy(AppDeploymentRequest request,
			Map<String, String> labels, MavenResource mavenResource) {
		Map<String, String> applicationProperties = request.getDefinition()
				.getProperties();

		return Stream.of(
				dockerfileFromProvidedGitRepoBuildConfig(applicationProperties, labels),
				dockerfileFromRemoteGitRepoBuildConfig(
						new OpenShiftMavenDeploymentRequest(request, mavenProperties),
						mavenResource, request, labels),
				dockerfileBuildConfig(request, labels)).filter(Optional::isPresent)
				.findFirst().orElse(Optional.of(new S2iBinaryInputBuildConfigStrategy(
						deployerProperties, client, labels, mavenResource)))
				.get();
	}

	private Optional<BuildConfigStrategy> dockerfileFromProvidedGitRepoBuildConfig(
			Map<String, String> applicationProperties, Map<String, String> labels) {
		Optional<BuildConfigStrategy> buildConfigFactory = Optional.empty();

		if (applicationProperties.containsKey(
				OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY)) {
			String gitUri = applicationProperties.get(
					OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY);
			String gitReferenceProperty = applicationProperties.getOrDefault(
					OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_REF_PROPERTY,
					"master");
			GitReference gitReference = new GitReference(gitUri, gitReferenceProperty);
			MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(
					deployerProperties, resourceHash, mavenProperties);
			buildConfigFactory = Optional
					.of(new GitWithDockerBuildConfigStrategy(mavenBuildConfigFactory,
							gitReference, deployerProperties, client, labels));
		}

		return buildConfigFactory;
	}

	/**
	 * check the Maven artifact Jar for the presence of `src/main/docker/Dockerfile`, if
	 * it exists, it is an indication/assumption that the Dockerfile is present in a
	 * remote Git repository. OpenShift will use the actual remote repository as a Git
	 * Repository source.
	 */
	private Optional<BuildConfigStrategy> dockerfileFromRemoteGitRepoBuildConfig(
			OpenShiftMavenDeploymentRequest openShiftRequest, Resource mavenResource,
			AppDeploymentRequest request, Map<String, String> labels) {
		Optional<BuildConfigStrategy> buildConfigFactory = Optional.empty();

		GitReference gitReference = openShiftRequest.getGitReference();
		try {
			if (openShiftRequest.isMavenProjectExtractable() && mavenResourceJarExtractor
					.extractFile(mavenResource, dockerfileLocation(request))
					.isPresent()) {
				/**
				 * extract Git URI and ref from <scm><connection>...</connection></scm>
				 * and <scm><tag>...</tag></scm> by parsing the Maven POM and use those
				 * values (as a {@link GitReference}) with Git Repository source strategy:
				 * https://docs.openshift.org/latest/dev_guide/builds.html#source-code
				 */
				MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(
						deployerProperties, resourceHash, mavenProperties);
				buildConfigFactory = Optional
						.of(new GitWithDockerBuildConfigStrategy(mavenBuildConfigFactory,
								gitReference, deployerProperties, client, labels));
			}
		}
		catch (IOException e) {
			logger.error("Could not extract Git URI from Maven artifact", e);
		}

		return buildConfigFactory;
	}

	private Optional<BuildConfigStrategy> dockerfileBuildConfig(
			AppDeploymentRequest request, Map<String, String> labels) {
		Optional<BuildConfigStrategy> buildConfigFactory = Optional.empty();

		if (request.getDeploymentProperties().containsKey(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE)) {
			MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(
					deployerProperties, resourceHash, mavenProperties);
			buildConfigFactory = Optional
					.of(new MavenDockerfileWithDockerBuildConfigStrategy(
							mavenBuildConfigFactory, deployerProperties, client, labels));
		}

		return buildConfigFactory;
	}

	/**
	 * Get the source context directory, the path where the Dockerfile is expected.
	 * Defaults to the root directory.
	 * @param request
	 * @return the context directory/path where the Dockerfile is expected
	 */
	private String dockerfileLocation(AppDeploymentRequest request) {
		return request.getDefinition().getProperties().getOrDefault(
				OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH,
				"Dockerfile");
	}

}
