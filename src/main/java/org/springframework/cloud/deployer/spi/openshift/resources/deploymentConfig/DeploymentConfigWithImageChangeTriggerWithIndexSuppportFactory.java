package org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory
		extends DeploymentConfigWithIndexSuppportFactory {

	private final OpenShiftDeployerProperties openShiftDeployerProperties;

	public DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory(OpenShiftClient client,
			OpenShiftDeployerProperties openShiftDeployerProperties, Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements, ImagePullPolicy imagePullPolicy) {
		super(client, openShiftDeployerProperties, container, labels, resourceRequirements, imagePullPolicy);
		this.openShiftDeployerProperties = openShiftDeployerProperties;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// do nothing, successful Build will trigger Deployment
	}

	@Override
	protected DeploymentConfig build(AppDeploymentRequest request, String appId, Container container,
			Map<String, String> labels, ResourceRequirements resourceRequirements, ImagePullPolicy imagePullPolicy) {
		DeploymentConfig deploymentConfig = super.build(request, appId, container, labels, resourceRequirements,
				imagePullPolicy);
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
                        .withNamespace(getImageNamespace(request,openShiftDeployerProperties))
                        .withName(getIndexedImageTag(request, openShiftDeployerProperties, appId))
                    .endFrom()
                .endImageChangeParams()
                .build())
            .endSpec()
            .build();
        //@formatter:on
	}
}
