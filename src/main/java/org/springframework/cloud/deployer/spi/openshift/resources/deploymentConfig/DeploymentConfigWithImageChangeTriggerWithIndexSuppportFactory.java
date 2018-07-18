package org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;

import java.util.Map;

public class DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory
		extends DeploymentConfigWithIndexSuppportFactory {

	private final OpenShiftClient client;

	private final OpenShiftDeployerProperties openShiftDeployerProperties;

	public DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory(
			OpenShiftClient client,
			OpenShiftDeployerProperties openShiftDeployerProperties, Container container,
			Map<String, String> labels, ResourceRequirements resourceRequirements,
			ImagePullPolicy imagePullPolicy) {
		super(client, openShiftDeployerProperties, container, labels,
				resourceRequirements, imagePullPolicy);
		this.client = client;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		withIndexedDeployment(appId, request, (id, deploymentRequest) -> {
			// @formatter:off
				client.deploymentConfigs()
					.withName(id)
					.edit()
						.editSpec()
							.addToTriggers(buildTriggerPolicy(deploymentRequest, id, true))
						.endSpec()
				.done();
				//@formatter:on
		});
	}

	@Override
	protected DeploymentConfig build(AppDeploymentRequest request, String appId,
			Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements, ImagePullPolicy imagePullPolicy) {
		DeploymentConfig deploymentConfig = super.build(request, appId, container, labels,
				resourceRequirements, imagePullPolicy);
		//@formatter:off
        return new DeploymentConfigBuilder(deploymentConfig)
            .editSpec()
            	.addToTriggers(buildTriggerPolicy(request, appId,false))
            .endSpec()
            .build();
        //@formatter:on
	}

	private DeploymentTriggerPolicy buildTriggerPolicy(AppDeploymentRequest request,
			String appId, Boolean automatic) {
		//@formatter:off
		return new DeploymentTriggerPolicyBuilder()
			.withType("ImageChange")
				.withNewImageChangeParams()
					.withContainerNames(appId)
					.withAutomatic(automatic)
					.withNewFrom()
						.withKind("ImageStreamTag")
						.withNamespace(getImageNamespace(request,openShiftDeployerProperties))
						.withName(getIndexedImageTag(request, openShiftDeployerProperties, appId))
					.endFrom()
				.endImageChangeParams()
			.build();
		//@formatter:on
	}

}
