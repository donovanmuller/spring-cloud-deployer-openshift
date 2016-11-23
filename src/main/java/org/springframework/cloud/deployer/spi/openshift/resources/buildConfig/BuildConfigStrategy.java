package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.OpenShiftClient;

public abstract class BuildConfigStrategy implements ObjectFactory<BuildConfig> {

	private OpenShiftClient client;
	private BuildConfigFactory buildConfigFactory;
	private Map<String, String> labels;

	protected BuildConfigStrategy(BuildConfigFactory buildConfigFactory, OpenShiftClient client,
			Map<String, String> labels) {
		this.buildConfigFactory = buildConfigFactory;
		this.client = client;
		this.labels = labels;
	}

	@Override
	public BuildConfig addObject(AppDeploymentRequest request, String appId) {
		BuildConfig buildConfig = buildBuildConfig(request, appId, labels);

		if (getExisting(appId).isPresent()) {
			/**
			 * Replacing a BuildConfig doesn't currently work because of "already modified" issues.
			 * Need to investigate if there is a clean way around it. For now, delete and
			 * recreate...
			 */
			// buildConfig = client.buildConfigs().createOrReplace(buildConfig);
			client.buildConfigs().withName(appId).delete();
			client.builds().withLabelIn("spring-app-id", appId).delete();
			buildConfig = client.buildConfigs().create(buildConfig);
		}
		else {
			buildConfig = client.buildConfigs().create(buildConfig);
		}

		return buildConfig;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		client.buildConfigs().withName(appId).instantiate(buildConfigFactory.buildBuildRequest(request, appId));
	}

	protected abstract BuildConfig buildBuildConfig(AppDeploymentRequest request, String appId,
			Map<String, String> labels);

	protected Optional<BuildConfig> getExisting(String name) {
		//@formatter:off
		BuildConfig value = client.buildConfigs()
                .withName(name)
                .fromServer()
                .get();

		return Optional.ofNullable(value);
		//@formatter:on
	}
}
