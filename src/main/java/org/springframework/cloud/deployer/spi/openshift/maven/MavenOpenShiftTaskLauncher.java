package org.springframework.cloud.deployer.spi.openshift.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.EntryPointStyle;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftApplicationPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftMavenDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.GitWithDockerBuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.MavenBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.MavenDockerfileWithDockerBuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.WatchingBuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.imageStream.ImageStreamFactory;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.DefaultOpenShiftClient;

public class MavenOpenShiftTaskLauncher extends OpenShiftTaskLauncher {

	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private MavenResourceJarExtractor mavenResourceJarExtractor;
	private MavenProperties mavenProperties;
	private ResourceHash resourceHash;

	public MavenOpenShiftTaskLauncher(KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties, MavenProperties mavenProperties,
			KubernetesClient client, MavenResourceJarExtractor mavenResourceJarExtractor, ResourceHash resourceHash) {
		super(properties, client);
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.mavenResourceJarExtractor = mavenResourceJarExtractor;
		this.mavenProperties = mavenProperties;
		this.resourceHash = resourceHash;
	}

	@Override
	protected List<ObjectFactory> populateOpenShiftObjects(AppDeploymentRequest request, String taskId) {
		List<ObjectFactory> factories = new ArrayList<>();

		MavenResource mavenResource = (MavenResource) request.getResource();
		// because of random task names, there will never be an existing corresponding build
		// so we should always kick off a new build
		if (!buildExists(request, taskId, mavenResource)) {
			logger.info(String.format("Building application '%s' with resource: '%s'", taskId, mavenResource));

			factories.add(new ImageStreamFactory(getClient()));
			factories.add(chooseBuildStrategy(taskId, request, createIdMap(taskId, request, null), mavenResource));
		}

		return factories;
	}

	// TODO there is allot of duplication with
	// org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer
	// we should probably extract the common functionality
	protected WatchingBuildConfigStrategy chooseBuildStrategy(String taskId, AppDeploymentRequest request,
			Map<String, String> labels, MavenResource mavenResource) {
		BuildConfigStrategy buildConfigStrategy;
		WatchingBuildConfigStrategy buildConfig;

		Map<String, String> parameters = request.getDefinition().getProperties();
		if (parameters.containsKey(OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY)) {
			String gitUri = parameters.get(OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY);
			String gitReferenceProperty = parameters
					.getOrDefault(OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_REF_PROPERTY, "master");
			GitReference gitReference = new GitReference(gitUri, gitReferenceProperty);
			MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(getProperties(), resourceHash,
					mavenProperties);
			buildConfigStrategy = new GitWithDockerBuildConfigStrategy(mavenBuildConfigFactory, gitReference,
					getProperties(), getClient(), labels);
			buildConfig = new WatchingBuildConfigStrategy(buildConfigStrategy, getClient(), labels,
					(build, watch) -> launchTask(build, watch, taskId, request));
		}
		else {
			OpenShiftMavenDeploymentRequest openShiftRequest = new OpenShiftMavenDeploymentRequest(request,
					mavenProperties);
			try {
				GitReference gitReference = openShiftRequest.getGitReference();
				if (mavenResourceJarExtractor.extractFile(mavenResource, "src/main/docker/Dockerfile").isPresent()) {
					MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(getProperties(),
							resourceHash, mavenProperties);
					buildConfigStrategy = new GitWithDockerBuildConfigStrategy(mavenBuildConfigFactory, gitReference,
							getProperties(), getClient(), labels);
					buildConfig = new WatchingBuildConfigStrategy(buildConfigStrategy, getClient(), labels,
							(build, watch) -> launchTask(build, watch, taskId, request));
				}
				else {
					MavenBuildConfigFactory mavenBuildConfigFactory = new MavenBuildConfigFactory(getProperties(),
							resourceHash, mavenProperties);
					buildConfigStrategy = new MavenDockerfileWithDockerBuildConfigStrategy(mavenBuildConfigFactory,
							openShiftDeployerProperties, getClient(), labels);
					buildConfig = new WatchingBuildConfigStrategy(buildConfigStrategy, getClient(), labels,
							(build, watch) -> launchTask(build, watch, taskId, request));
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
	// org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer
	// we should probably extract the common functionality
	protected boolean buildExists(AppDeploymentRequest request, String appId, MavenResource mavenResource) {
		boolean buildExists;

		String forceBuild = request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_BUILD_FORCE);
		if (StringUtils.isAlpha(forceBuild)) {
			buildExists = !Boolean.parseBoolean(forceBuild.toLowerCase())
					|| !openShiftDeployerProperties.isForceBuild();
		}
		else {
			buildExists = getClient().builds().withLabelIn(SPRING_APP_KEY, appId).list().getItems().stream()
					.filter(build -> !build.getStatus().getPhase().equals("Failed"))
					.flatMap(build -> build.getSpec().getStrategy().getDockerStrategy().getEnv().stream()
							.filter(envVar -> envVar.getName().equals(MavenBuildConfigFactory.SPRING_BUILD_ID_ENV_VAR)
									&& envVar.getValue().equals(resourceHash.hashResource(mavenResource))))
					.count() > 0;
		}

		return buildExists;
	}

	protected void launchTask(Build build, Watch watch, String taskId, AppDeploymentRequest request) {
		if (build.getStatus().getPhase().equals("Complete")) {
			logger.info(String.format("Build complete: '%s'", build.getMetadata().getName()));

			DockerResource dockerResource = new DockerResource(build.getStatus().getOutputDockerImageReference());
			AppDeploymentRequest taskDeploymentRequest = new AppDeploymentRequest(request.getDefinition(),
					dockerResource, request.getDeploymentProperties(), request.getCommandlineArguments());

			new KubernetesTaskLauncher(getProperties(),
					new DefaultOpenShiftClient().inNamespace(getClient().getNamespace())) {

				/**
				 * Reuse the taskId created in the {@link OpenShiftTaskLauncher}, otherwise the
				 * {@link KubernetesTaskLauncher} will generate a new taskId for the actual task Pod
				 * which does not match the one returned from launch.
				 */
				@Override
				protected String createDeploymentId(AppDeploymentRequest request) {
					return taskId;
				}
			}.launch(applyDefaultEntryPoint(taskDeploymentRequest));

			watch.close();
		}
	}

	/**
	 * If there is no explicit {@link EntryPointStyle} provided, then use
	 * {@link EntryPointStyle#boot} because the default Dockerfiles uses the shell ENTRYPOINT form
	 * See https://docs.docker.com/engine/reference/builder/#/shell-form-entrypoint-example.
	 *
	 * We should use the {@link EntryPointStyle#shell} style but unfortunately because of
	 * https://github.com/spring-cloud/spring-cloud-stream/issues/459 we cannot :(
	 */
	private AppDeploymentRequest applyDefaultEntryPoint(AppDeploymentRequest request) {
		Map<String, String> deploymentProperties = new HashMap<>(request.getDeploymentProperties());
		deploymentProperties.putIfAbsent("spring.cloud.deployer.kubernetes.entryPointStyle",
			EntryPointStyle.boot.name());
		return new AppDeploymentRequest(request.getDefinition(), request.getResource(), deploymentProperties,
			request.getCommandlineArguments());
	}
}
