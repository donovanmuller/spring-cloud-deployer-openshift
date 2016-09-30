package org.springframework.cloud.deployer.spi.openshift.factories;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.DataflowSupport;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.client.OpenShiftClient;

public class DeploymentConfigWithIndexSuppportFactory extends DeploymentConfigFactory
		implements DataflowSupport {

	public DeploymentConfigWithIndexSuppportFactory(OpenShiftClient client,
			OpenShiftDeployerProperties openShiftDeployerProperties, Container container,
			Map<String, String> labels, ResourceRequirements resourceRequirements) {
		super(client, openShiftDeployerProperties, container, labels,
				resourceRequirements);
	}

	@Override
	public DeploymentConfig addObject(AppDeploymentRequest request, String appId) {
		Integer count = getAppInstanceCount(request);
		if (isIndexed(request)) {
			for (int index = 0; index < count; index++) {
				String indexedId = appId + "-" + index;
				super.addObject(request, indexedId);
			}
		}
		else {
			super.addObject(request, appId);
		}

		return null;
	}

	@Override
	protected DeploymentConfig build(AppDeploymentRequest request, String appId,
			Container container, Map<String, String> labels,
			ResourceRequirements resourceRequirements) {
		// TODO don't add '-0' to un-indexed streams
		labels.replace("spring-deployment-id", appId);
		container.setName(appId);
		container.setImage(getImage(request, appId));

		Optional<EnvVar> instanceIndexEnvVar = container.getEnv().stream()
			.filter(envVar -> envVar.getName()
				.equals(AppDeployer.INSTANCE_INDEX_PROPERTY_KEY))
			.findFirst();
		String instanceIndex = NumberUtils
			.isNumber(StringUtils.substringAfterLast(appId, "-"))
			? StringUtils.substringAfterLast(appId, "-")
			: Integer.valueOf(0).toString();
		if (instanceIndexEnvVar.isPresent()) {
			instanceIndexEnvVar.get().setValue(instanceIndex);
		} else {
			container.getEnv().add(new EnvVar(AppDeployer.INSTANCE_INDEX_PROPERTY_KEY,
				instanceIndex, null));
		}

		return super.build(request, appId, container, labels, resourceRequirements);
	}

	protected Integer getReplicas(AppDeploymentRequest request) {
		return isIndexed(request) ? 1 : getAppInstanceCount(request);
	}

}
