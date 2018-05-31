package org.springframework.cloud.deployer.spi.openshift.resources.service;

import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePort;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class ServiceFactory implements ObjectFactory<Service> {

	private OpenShiftClient client;

	private Integer port;

	private Map<String, String> labels;

	public ServiceFactory(OpenShiftClient client, Integer port,
			Map<String, String> labels) {
		this.client = client;
		this.port = port;
		this.labels = labels;
	}

	@Override
	public Service addObject(AppDeploymentRequest request, String appId) {
		Service service = build(request, appId, port, labels);

		if (getExisting(appId).isPresent()) {
			// cannot patch a Service. Delete it, then recreate
			this.client.services().delete(service);
			service = this.client.services().create(service);
		}
		else {
			service = this.client.services().create(service);
		}

		return service;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// do nothing
	}

	protected Optional<Service> getExisting(String name) {
		//@formatter:off
		return Optional.ofNullable(client.services()
			.withName(name)
			.fromServer()
			.get());
		//@formatter:on
	}

	protected Service build(AppDeploymentRequest request, String appId, Integer port,
			Map<String, String> labels) {
		boolean createNodePort = StringUtils.isNotBlank(request.getDeploymentProperties()
				.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT));

		String serviceNameOrAppId = request.getDeploymentProperties().getOrDefault(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_SERVICE_NAME, appId);

		return new ServiceBuilder()
		//@formatter:off
			.withNewMetadata()
				.withName(serviceNameOrAppId)
				.withLabels(labels)
			.endMetadata()
			.withNewSpec()
				.withPorts(createNodePort ? buildServiceNodePort(request) : buildServicePort())
				.withSelector(labels)
			.endSpec()
			.build();
			//@formatter:on
	}

	private ServicePort buildServicePort() {
		return new ServicePortBuilder().withPort(port).withNewTargetPort(port).build();
	}

	private ServicePort buildServiceNodePort(AppDeploymentRequest request) {
		String createNodePort = request.getDeploymentProperties().getOrDefault(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT,
				StringUtils.EMPTY);
		return new ServicePortBuilder().withPort(port)
				.withNodePort(StringUtils.isNumeric(createNodePort)
						? Integer.parseInt(createNodePort) : null)
				.build();
	}

}
