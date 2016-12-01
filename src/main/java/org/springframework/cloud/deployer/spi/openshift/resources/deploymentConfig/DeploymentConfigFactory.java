package org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.openshift.DataflowSupport;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeFactory;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentConfigFactory implements ObjectFactory<DeploymentConfig>, OpenShiftSupport, DataflowSupport {

	public static final String SPRING_DEPLOYMENT_TIMESTAMP = "spring-cloud-deployer/redeploy-timestamp";

	private OpenShiftClient client;
	private Container container;
	private Map<String, String> labels;
	private ResourceRequirements resourceRequirements;
	private ImagePullPolicy imagePullPolicy;
	private VolumeFactory volumeFactory;

	public DeploymentConfigFactory(OpenShiftClient client, Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements, ImagePullPolicy imagePullPolicy, VolumeFactory volumeFactory) {
		this.client = client;
		this.container = container;
		this.labels = labels;
		this.resourceRequirements = resourceRequirements;
		this.imagePullPolicy = imagePullPolicy;
		this.volumeFactory = volumeFactory;
	}

	@Override
	public DeploymentConfig addObject(AppDeploymentRequest request, String appId) {
		DeploymentConfig deploymentConfig = build(request, appId, container, labels, resourceRequirements,
				imagePullPolicy);

		if (getExisting(appId).isPresent()) {
			deploymentConfig = this.client.deploymentConfigs().createOrReplace(deploymentConfig);
		}
		else {
			deploymentConfig = this.client.deploymentConfigs().create(deploymentConfig);
		}

		return deploymentConfig;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// if there are no builds in progress
		if (client.builds().withLabels(labels).list().getItems().stream().noneMatch(build -> {
			String phase = build.getStatus().getPhase();
			return phase.equals("New") || phase.equals("Pending") || phase.equals("Running") || phase.equals("Failed");
		})) {
			// TODO when
			// https://github.com/fabric8io/kubernetes-client/issues/507#issuecomment-246272404
			// is implemented, rather kick off another deployment
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
		return Optional.ofNullable(client.deploymentConfigs().withName(name).fromServer().get());
	}

	protected DeploymentConfig build(AppDeploymentRequest request, String appId, Container container,
			Map<String, String> labels, ResourceRequirements resourceRequirements, ImagePullPolicy imagePullPolicy) {
		container.setResources(resourceRequirements);
		container.setImagePullPolicy(imagePullPolicy.name());

		//@formatter:off
        return new DeploymentConfigBuilder()
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withTriggers(ImmutableList.of(
                	new DeploymentTriggerPolicyBuilder()
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
						// only add volumes with corresponding volume mounts
						.withVolumes(volumeFactory.addObject(request, appId).stream()
							.filter(volume -> container.getVolumeMounts().stream()
									.anyMatch(volumeMount -> volumeMount.getName().equals(volume.getName())))
							.collect(Collectors.toList()))
					.endSpec()
                .endTemplate()
            .endSpec()
            .build();
        //@formatter:on
	}

	protected Integer getReplicas(AppDeploymentRequest request) {
		return getAppInstanceCount(request);
	}
}
