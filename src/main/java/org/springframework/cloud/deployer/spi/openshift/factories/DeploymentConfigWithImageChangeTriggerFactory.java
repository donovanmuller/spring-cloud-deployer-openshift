package org.springframework.cloud.deployer.spi.openshift.factories;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentConfigWithImageChangeTriggerFactory
		extends DeploymentConfigFactory {

	private final OpenShiftDeployerProperties openShiftDeployerProperties;

	public DeploymentConfigWithImageChangeTriggerFactory(OpenShiftClient client,
			OpenShiftDeployerProperties openShiftDeployerProperties, Container container,
			Map<String, String> labels, ResourceRequirements resourceRequirements) {
		super(client, container, labels, resourceRequirements);
		this.openShiftDeployerProperties = openShiftDeployerProperties;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// do nothing, successful Build will trigger Deployment
	}

	@Override
	protected DeploymentConfig build(AppDeploymentRequest request, String appId,
			Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements) {
		DeploymentConfig deploymentConfig = super.build(request, appId, container, labels,
				resourceRequirements);
		//@formatter:off
        return new DeploymentConfigBuilder(deploymentConfig)
            .editSpec()
            .addToTriggers(new DeploymentTriggerPolicyBuilder()
                .withType("ImageChange")
                .withNewImageChangeParams()
                    .withAutomatic(true)
                    .withContainerNames(appId)
                    .withNewFrom()
                        .withKind("ImageStreamTag")
                        .withName(getImageTag(request, openShiftDeployerProperties, appId))
                    .endFrom()
                .endImageChangeParams()
                .build())
            .endSpec()
            .build();
        //@formatter:on
	}
}
