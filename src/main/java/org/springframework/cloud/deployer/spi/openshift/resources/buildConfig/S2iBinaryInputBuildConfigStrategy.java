package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftAppDeployer;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;

import java.io.IOException;
import java.util.Map;

public class S2iBinaryInputBuildConfigStrategy extends BuildConfigStrategy
		implements OpenShiftSupport {

	private static Logger logger = LoggerFactory.getLogger(OpenShiftAppDeployer.class);

	private OpenShiftClient client;

	private final OpenShiftDeployerProperties openShiftDeployerProperties;

	private final MavenResource mavenResource;

	public S2iBinaryInputBuildConfigStrategy(
			OpenShiftDeployerProperties openShiftDeployerProperties,
			OpenShiftClient client, Map<String, String> labels,
			MavenResource mavenResource) {
		super(null, client, labels);
		this.client = client;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.mavenResource = mavenResource;
	}

	@Override
	protected BuildConfig buildBuildConfig(AppDeploymentRequest request, String appId,
			Map<String, String> labels) {
		//@formatter:off
		return new BuildConfigBuilder()
			.withNewMetadata()
				.withName(appId)
				.withLabels(labels)
			.endMetadata()
			.withNewSpec()
				.withNewSource()
					.withType("binary")
				.endSource()
				.withNewStrategy()
					.withNewSourceStrategy()
						.withNewFrom()
							.withKind("DockerImage")
							.withName(request.getDeploymentProperties().getOrDefault(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_S2I_BUILD_IMAGE,
						openShiftDeployerProperties.getDefaultS2iImage()))
						.endFrom()
					.endSourceStrategy()
				.endStrategy()
				.withNewOutput()
					.withNewTo()
						.withKind("ImageStreamTag")
						.withName(getImageTag(request, openShiftDeployerProperties, appId))
					.endTo()
				.endOutput()
			.endSpec()
			.build();
		//@formatter:on
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		try {
			client.buildConfigs().withName(appId).instantiateBinary()
					.asFile(mavenResource.getFilename())
					.fromFile(mavenResource.getFile());
		}
		catch (IOException e) {
			logger.error(String.format("Could not access Maven artifact: %s",
					mavenResource.getFilename()), e);
		}
	}

}
