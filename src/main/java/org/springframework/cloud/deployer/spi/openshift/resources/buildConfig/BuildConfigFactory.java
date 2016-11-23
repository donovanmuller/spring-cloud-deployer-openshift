package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;

import com.google.common.collect.ImmutableList;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.api.model.BuildTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public abstract class BuildConfigFactory implements OpenShiftSupport {

	public static String SPRING_BUILD_ID_ENV_VAR = "spring_build_id";
	public static String SPRING_BUILD_APP_NAME_ENV_VAR = "app_name";

//	public BuildConfigFactory(OpenShiftClient client, Map<String, String> labels) {
//	}

	protected BuildConfig buildBuildConfig(AppDeploymentRequest request, String appId, Map<String, String> labels) {
		//@formatter:off
        return new BuildConfigBuilder()
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withTriggers(ImmutableList.of(
                        new BuildTriggerPolicyBuilder()
                            .withNewImageChange()
                            .endImageChange()
                            .build()
                ))
            .endSpec()
            .build();
        //@formatter:on
	}

	protected abstract BuildRequest buildBuildRequest(AppDeploymentRequest request, String appId);
}
