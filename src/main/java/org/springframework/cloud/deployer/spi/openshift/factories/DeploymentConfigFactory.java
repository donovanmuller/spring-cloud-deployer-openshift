package org.springframework.cloud.deployer.spi.openshift.factories;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentConfigFactory
		implements ObjectFactory<DeploymentConfig>, OpenShiftSupport {

	public static final String SPRING_DEPLOYMENT_TIMESTAMP = "spring-cloud-deployer/redeploy-timestamp";

	private OpenShiftClient client;
	private Container container;
	private Map<String, String> labels;
	private ResourceRequirements resourceRequirements;

	public DeploymentConfigFactory(OpenShiftClient client, Container container,
			Map<String, String> labels, final ResourceRequirements resourceRequirements) {
		this.client = client;
		this.container = container;
		this.labels = labels;
		this.resourceRequirements = resourceRequirements;
	}

	@Override
	public DeploymentConfig addObject(AppDeploymentRequest request, String appId) {
		DeploymentConfig deploymentConfig = build(request, appId, container, labels,
				resourceRequirements);

		if (getExisting(appId).isPresent()) {
			client.deploymentConfigs().withName(appId).delete();
			deploymentConfig = client.deploymentConfigs().create(deploymentConfig);
		}
		else {
			deploymentConfig = client.deploymentConfigs().create(deploymentConfig);
		}

		return deploymentConfig;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// if there are no builds in progress
		if (client.builds().withLabels(labels).list().getItems().stream()
				.noneMatch(build -> {
					String phase = build.getStatus().getPhase();
					return phase.equals("New") || phase.equals("Pending")
							|| phase.equals("Running") || phase.equals("Failed");
				})) {
			//@formatter:off
            client.deploymentConfigs()
                    .withName(appId)
                    .edit()
                        .editMetadata()
                            .addToAnnotations(SPRING_DEPLOYMENT_TIMESTAMP,
								String.valueOf(System.currentTimeMillis()))
                        .endMetadata()
                    .done();
            //@formatter:on
		}
	}

	protected Optional<DeploymentConfig> getExisting(String name) {
		return Optional
				.ofNullable(client.deploymentConfigs().withName(name).fromServer().get());
	}

	protected DeploymentConfig build(AppDeploymentRequest request, String appId,
			Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements) {
		//@formatter:off
        return new DeploymentConfigBuilder()
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withTriggers(ImmutableList.of(new DeploymentTriggerPolicyBuilder()
                        .withType("ConfigChange")
                        .build()))
                .withNewStrategy()
                    .withType("Rolling")
					.withResources(resourceRequirements)
                .endStrategy()
                .withReplicas(getReplicas(request))
                .withSelector(labels)
                .withNewTemplate()
					.withNewMetadata()
						.withLabels(labels)
					.endMetadata()
                    .withNewSpec()
                        .withContainers(container)
                        .withRestartPolicy("Always")
                        .withServiceAccount(request.getDeploymentProperties()
                                .getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_SERVICE_ACCOUNT,
                                        StringUtils.EMPTY))
						.withNodeSelector(getNodeSelectors(request.getDeploymentProperties()))
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
        //@formatter:on
	}

	protected Integer getReplicas(AppDeploymentRequest request) {
		return Integer.valueOf(request.getDeploymentProperties()
				.getOrDefault(AppDeployer.COUNT_PROPERTY_KEY, "1"));
	}
}
