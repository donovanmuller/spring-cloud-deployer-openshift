package org.springframework.cloud.deployer.spi.openshift.resources.service;

import java.util.Map;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.DataflowSupport;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.openshift.client.OpenShiftClient;

public class ServiceWithIndexSuppportFactory extends ServiceFactory implements DataflowSupport {

	public ServiceWithIndexSuppportFactory(OpenShiftClient client, Integer port, Map<String, String> labels) {
		super(client, port, labels);
	}

	@Override
	public Service addObject(AppDeploymentRequest request, String appId) {
		if (isIndexed(request)) {
			for (int index = 0; index < getAppInstanceCount(request); index++) {
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
	protected Service build(AppDeploymentRequest request, String appId, Integer port, Map<String, String> labels) {
		labels.replace("spring-deployment-id", appId);
		return super.build(request, appId, port, labels);
	}
}
