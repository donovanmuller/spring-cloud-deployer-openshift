package org.springframework.cloud.deployer.spi.openshift.maven;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftMavenDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftApplicationPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.DockerfileWithDockerBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.GitWithDockerBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.imageStream.ImageStreamFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.WatchingBuildConfigFactory;
import org.springframework.util.StringUtils;

import com.google.common.collect.Iterables;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.openshift.api.model.Build;

public class MavenOpenShiftTaskLauncher extends OpenShiftTaskLauncher {

	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private MavenResourceJarExtractor mavenResourceJarExtractor;
	private MavenProperties mavenProperties;
	private ResourceHash resourceHash;

	public MavenOpenShiftTaskLauncher(KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			MavenProperties mavenProperties, KubernetesClient client,
			MavenResourceJarExtractor mavenResourceJarExtractor,
			ResourceHash resourceHash) {
		super(properties, client);
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
			logger.info(String.format("Building application '%s' with resource: '%s'", appId,	mavenResource));

			factories.add(new ImageStreamFactory(getClient()));
			factories.add(chooseBuildStrategy(request, createIdMap(appId, request, null),
					mavenResource));
		}
		else {
			Build build = Iterables.getLast(getClient().builds()
					.withLabelIn(SPRING_APP_KEY, appId).list().getItems());
			DockerResource dockerResource = new DockerResource(
					build.getStatus().getOutputDockerImageReference());

			AppDeploymentRequest taskDeploymentRequest = new AppDeploymentRequest(
					request.getDefinition(), dockerResource,
					request.getDeploymentProperties(), request.getCommandlineArguments());

			factories
					.addAll(super.populateOpenShiftObjects(taskDeploymentRequest, appId));
		}

		return factories;
	}

	// TODO there is allot of duplication with
	// org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer
	// we should probably extract the common functionality
	protected WatchingBuildConfigFactory chooseBuildStrategy(AppDeploymentRequest request,
			Map<String, String> labels, MavenResource mavenResource) {
		WatchingBuildConfigFactory buildConfig;

		Map<String, String> parameters = request.getDefinition().getProperties();
		if (parameters.containsKey(
				OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY)) {
			String gitUri = parameters.get(
					OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_URI_PROPERTY);
			String gitReference = parameters.getOrDefault(
					OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_REF_PROPERTY,
					"master");
			buildConfig = new WatchingBuildConfigFactory(getClient(),
					new GitWithDockerBuildConfigFactory(getClient(), labels,
							new GitReference(gitUri, gitReference), getProperties(),
							openShiftDeployerProperties, mavenProperties, resourceHash),
					(build, watch) -> launchTask(build, watch, request));
		}
		else {
			OpenShiftMavenDeploymentRequest openShiftRequest = new OpenShiftMavenDeploymentRequest(
					request, mavenProperties);
			try {
				if (mavenResourceJarExtractor
						.extractFile(mavenResource, "src/main/docker/Dockerfile")
						.isPresent()) {
					buildConfig = new WatchingBuildConfigFactory(getClient(),
							new GitWithDockerBuildConfigFactory(getClient(), labels,
									openShiftRequest.getGitReference(), getProperties(),
									openShiftDeployerProperties, mavenProperties,
									resourceHash),
							(build, watch) -> launchTask(build, watch, request));
				}
				else {
					buildConfig = new WatchingBuildConfigFactory(getClient(),
							new DockerfileWithDockerBuildConfigFactory(getClient(),
									labels, openShiftRequest.getGitReference(),
									getProperties(), openShiftDeployerProperties,
									mavenProperties, resourceHash),
							(build, watch) -> launchTask(build, watch, request));
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
									&& envVar.getValue().equals(
											resourceHash.hashResource(mavenResource))))
					.count() > 0;
		}

		return buildExists;
	}

	protected void launchTask(Build build, Watch watch, AppDeploymentRequest request) {
		if (build.getStatus().getPhase().equals("Complete")) {
			logger.info(String.format("Build complete: '%s'", build.getMetadata().getName()));

			DockerResource dockerResource = new DockerResource(
					build.getStatus().getOutputDockerImageReference());
			AppDeploymentRequest taskDeploymentRequest = new AppDeploymentRequest(
					request.getDefinition(), dockerResource,
					request.getDeploymentProperties(), request.getCommandlineArguments());

			new KubernetesTaskLauncher(getProperties(), getClient())
					.launch(taskDeploymentRequest);

			watch.close();
		}
	}
}
