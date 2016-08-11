package org.springframework.cloud.deployer.spi.openshift.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftAppDeployer;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftMavenDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftRequestDefinitionPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.factories.BuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.DeploymentConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.DeploymentConfigWithImageChangeTriggerFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.DockerfileWithDockerBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.GitWithDockerBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.ImageStreamFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.ObjectFactory;
import org.springframework.util.StringUtils;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.client.KubernetesClient;

public class MavenOpenShiftAppDeployer extends OpenShiftAppDeployer {

	private static Logger logger = LoggerFactory
			.getLogger(MavenOpenShiftAppDeployer.class);

	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private MavenResourceJarExtractor mavenResourceJarExtractor;
	private MavenProperties mavenProperties;
	private ResourceHash resourceHash;

	public MavenOpenShiftAppDeployer(KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			KubernetesClient client, ContainerFactory containerFactory,
			MavenResourceJarExtractor mavenResourceJarExtractor,
			MavenProperties mavenProperties, ResourceHash resourceHash) {
		super(properties, openShiftDeployerProperties, client, containerFactory);
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.mavenResourceJarExtractor = mavenResourceJarExtractor;
		this.mavenProperties = mavenProperties;
		this.resourceHash = resourceHash;
	}

	@Override
	protected List<ObjectFactory> populateOpenShiftObjects(AppDeploymentRequest request,
			String appId) {
		List<ObjectFactory> factories = new ArrayList<>();

		MavenResource mavenResource = (MavenResource) request.getResource();
		if (!buildExists(request, appId, mavenResource)) {
			logger.info("Building application [{}] with resource: {}", appId,
					mavenResource);

			factories.add(new ImageStreamFactory(getClient()));
			factories.add(chooseBuildStrategy(request, createIdMap(appId, request, null),
					mavenResource));
		}

		factories.addAll(super.populateOpenShiftObjects(request, appId));

		return factories;
	}

	@Override
	protected DeploymentConfigFactory getDeploymentConfigFactory(
			AppDeploymentRequest request, Map<String, String> labels,
			Container container) {
		return new DeploymentConfigWithImageChangeTriggerFactory(getClient(),
				openShiftDeployerProperties, container, labels,
				getResourceRequirements(request));
	}

	// TODO there is allot of duplication with
	// org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftTaskLauncher
	// we should probably extract the common functionality
	protected BuildConfigFactory chooseBuildStrategy(AppDeploymentRequest request,
			Map<String, String> labels, MavenResource mavenResource) {
		BuildConfigFactory buildConfig;

		Map<String, String> parameters = request.getDefinition().getProperties();
		if (parameters.containsKey(
					OpenShiftRequestDefinitionPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY)) {
			String gitUri = parameters.get(
					OpenShiftRequestDefinitionPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY);
			String gitReference = parameters.getOrDefault(
					OpenShiftRequestDefinitionPropertyKeys.OPENSHIFT_BUILD_GIT_REF_PROPERTY,
					"master");
			buildConfig = new GitWithDockerBuildConfigFactory(getClient(), labels,
					new GitReference(gitUri, gitReference), getProperties(),
					mavenProperties, resourceHash);
		}
		else {
			OpenShiftMavenDeploymentRequest openShiftRequest = new OpenShiftMavenDeploymentRequest(
					request, mavenProperties);
			try {
				/**
				 * check the Maven artifact Jar for the presence of
				 * `src/main/docker/Dockerfile`, if it exists, it is an
				 * indication/assumption that the Dockerfile is present in a remote Git
				 * repository. OpenShift will use the actual remote repository as a Git
				 * Repository source.
				 */
				if (openShiftRequest.isMavenProjectExtractable()
						&& mavenResourceJarExtractor
								.extractFile(mavenResource, dockerfileLocation(request))
								.isPresent()) {
					/**
					 * extract Git URI and ref from
					 * <scm><connection>...</connection></scm> and
					 * <scm><tag>...</tag></scm> by parsing the Maven POM and use those
					 * values (as a {@link GitReference}) with Git Repository source
					 * strategy:
					 * https://docs.openshift.org/latest/dev_guide/builds.html#source-code
					 */
					buildConfig = new GitWithDockerBuildConfigFactory(getClient(), labels,
							openShiftRequest.getGitReference(), getProperties(),
							mavenProperties, resourceHash);
				}
				else {
					/**
					 * otherwise use the Dockerfile source strategy:
					 * https://docs.openshift.org/latest/dev_guide/builds.html#dockerfile-
					 * source
					 */
					buildConfig = new DockerfileWithDockerBuildConfigFactory(getClient(),
							labels, openShiftRequest.getGitReference(), getProperties(),
							mavenProperties, resourceHash);
				}
			}
			catch (IOException e) {
				logger.error("Could not create specified BuildConfig", e);
				throw new RuntimeException("Could not create specified BuildConfig", e);
			}
		}

		return buildConfig;
	}

	// TODO there is allot of duplication with
	// org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftTaskLauncher
	// we should probably extract the common functionality
	protected boolean buildExists(AppDeploymentRequest request, String appId,
			MavenResource mavenResource) {
		boolean buildExists;

		String forceBuild = request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_BUILD_FORCE);
		if (StringUtils.hasText(forceBuild)) {
			buildExists = !Boolean.parseBoolean(forceBuild.toLowerCase())
					|| !openShiftDeployerProperties.isForceBuild();
		}
		else {
			buildExists = getClient().builds().withLabelIn(SPRING_APP_KEY, appId).list()
					.getItems().stream()
					.filter(build -> !build.getStatus().getPhase().equals("Failed"))
					.flatMap(build -> build.getSpec().getStrategy().getDockerStrategy()
							.getEnv().stream().filter(envVar -> envVar.getName()
									.equals(BuildConfigFactory.SPRING_BUILD_ID_ENV_VAR)
									&& envVar.getValue()
											.equals(resourceHash
													.hashResource(mavenResource))))
					.count() > 0;
		}

		return buildExists;
	}

	/**
	 * Get the source context directory, the path where the Dockerfile is expected.
	 * Defaults to the root directory.
	 *
	 * @param request
	 * @return the context directory/path where the Dockerfile is expected
	 */
	protected String dockerfileLocation(AppDeploymentRequest request) {
		return request.getDefinition().getProperties().getOrDefault(
				OpenShiftRequestDefinitionPropertyKeys.OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH,
				"Dockerfile");
	}
}
