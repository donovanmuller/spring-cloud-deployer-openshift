package org.springframework.cloud.deployer.spi.openshift.maven;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.openshift.api.model.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftAppDeployer;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildStrategies;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.MavenBuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.S2iBinaryInputBuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.WatchingBuildConfigStrategy;
import org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig.DeploymentConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig.DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.imageStream.ImageStreamFactory;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MavenOpenShiftAppDeployer extends OpenShiftAppDeployer {

	private static Logger logger = LoggerFactory
			.getLogger(MavenOpenShiftAppDeployer.class);

	private OpenShiftDeployerProperties openShiftDeployerProperties;

	private MavenResourceJarExtractor mavenResourceJarExtractor;

	private MavenProperties mavenProperties;

	private ResourceHash resourceHash;

	public MavenOpenShiftAppDeployer(
			OpenShiftDeployerProperties openShiftDeployerProperties,
			KubernetesClient client, ContainerFactory containerFactory,
			MavenResourceJarExtractor mavenResourceJarExtractor,
			MavenProperties mavenProperties, ResourceHash resourceHash) {
		super(openShiftDeployerProperties, client, containerFactory);
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.mavenResourceJarExtractor = mavenResourceJarExtractor;
		this.mavenProperties = mavenProperties;
		this.resourceHash = resourceHash;
	}

	@Override
	protected List<ObjectFactory> populateOpenShiftObjectsForDeployment(
			AppDeploymentRequest request, String appId) {
		List<ObjectFactory> factories = new ArrayList<>();

		MavenResource mavenResource = (MavenResource) request.getResource();
		if (!buildExists(request, appId, mavenResource)) {
			logger.info("Building application '{}' with resource: {}", appId,
					mavenResource);

			factories.add(new ImageStreamFactory(getClient()));

			BuildStrategies buildStrategies = new BuildStrategies(mavenProperties,
					openShiftDeployerProperties, mavenResourceJarExtractor, resourceHash,
					getClient());
			BuildConfigStrategy buildStrategy = buildStrategies.chooseBuildStrategy(
					request, createIdMap(appId, request), mavenResource);
			if (buildStrategy instanceof S2iBinaryInputBuildConfigStrategy) {
				request = new AppDeploymentRequest(request.getDefinition(),
						request.getResource(),
						ImmutableMap.<String, String>builder()
								.putAll(request.getDeploymentProperties())
								.put("s2i-build", "true").build(),
						request.getCommandlineArguments());
			}

			AppDeploymentRequest deploymentRequest = new AppDeploymentRequest(
					request.getDefinition(), request.getResource(),
					request.getDeploymentProperties(), request.getCommandlineArguments());

			WatchingBuildConfigStrategy watchingBuildConfigStrategy = new WatchingBuildConfigStrategy(
					buildStrategy, getClient(), createIdMap(appId, request),
					(build, watch) -> rolloutDeployment(build, watch, appId,
							deploymentRequest));
			factories.add(watchingBuildConfigStrategy);
		}

		factories.addAll(super.populateOpenShiftObjectsForDeployment(request, appId));

		return factories;
	}

	@Override
	protected DeploymentConfigFactory getDeploymentConfigFactory(
			AppDeploymentRequest request, Map<String, String> labels,
			Container container) {
		return new DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory(
				getClient(), openShiftDeployerProperties, container, labels,
				getResourceRequirements(request), getImagePullPolicy(request));
	}

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
					.filter(build -> !build.getStatus().getPhase().equals("Failed")
							&& build.getSpec().getStrategy().getDockerStrategy() != null)
					.flatMap(build -> build.getSpec().getStrategy().getDockerStrategy()
							.getEnv().stream()
							.filter(envVar -> envVar.getName().equals(
									MavenBuildConfigFactory.SPRING_BUILD_ID_ENV_VAR)
									&& envVar.getValue().equals(
											resourceHash.hashResource(mavenResource))))
					.count() > 0;
		}

		return buildExists;
	}

	protected void rolloutDeployment(Build build, Watch watch, String appId,
			AppDeploymentRequest request) {
		if (build.getStatus().getPhase().equals("Complete")) {
			logger.info(
					String.format("Build complete: '%s'", build.getMetadata().getName()));

			withIndexedDeployment(appId, request, (id, deploymentRequest) -> {
				logger.info(String.format("Rolling out latest deployment of '%s'", id));
				getClient().deploymentConfigs().withName(id).deployLatest();
			});

			watch.close();
		}
	}

}
