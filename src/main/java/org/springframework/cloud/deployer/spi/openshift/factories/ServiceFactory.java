package org.springframework.cloud.deployer.spi.openshift.factories;

import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
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
			client.services().delete(service);
			service = client.services().create(service);
		}
		else {
			service = client.services().create(service);
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
		return new ServiceBuilder()
			//@formatter:off
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withPorts(new ServicePortBuilder()
                        .withPort(port)
                        .withNewTargetPort(port)
                        .build())
                .withSelector(labels)
            .endSpec()
            .build();
            //@formatter:on
	}
}
