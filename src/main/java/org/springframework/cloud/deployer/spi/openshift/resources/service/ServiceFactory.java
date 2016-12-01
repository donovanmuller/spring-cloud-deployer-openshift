package org.springframework.cloud.deployer.spi.openshift.resources.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
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

	public ServiceFactory(OpenShiftClient client, Integer port, Map<String, String> labels) {
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

	protected Service build(AppDeploymentRequest request, String appId, Integer port, Map<String, String> labels) {
		boolean createNodePort = StringUtils.isNotBlank(
				request.getDeploymentProperties().get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT));

		return new ServiceBuilder()
			//@formatter:off
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
				.withAnnotations(determineDependencies(request))
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
		String createNodePort = request.getDeploymentProperties()
				.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT, StringUtils.EMPTY);
		return new ServicePortBuilder().withPort(port)
				.withNodePort(StringUtils.isNumeric(createNodePort) ? Integer.parseInt(createNodePort) : null).build();
	}

	private Map<String, String> determineDependencies(AppDeploymentRequest request) {
		Map<String, String> dependencies = new HashMap<>();

		ServiceDependencies serviceDependencies = new ServiceDependencies(client.services()
			.withLabel("spring-group-id",
				request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY))
			.list().getItems().stream()
			.map(service -> new ServiceDependency(service.getMetadata().getName()))
			.collect(Collectors.toList()));
		try {
			dependencies.put("service.alpha.openshift.io/dependencies",
				new ObjectMapper()
					.writeValueAsString(serviceDependencies.getDependencies()));
		} catch (JsonProcessingException e) {
			// oh well, this is a nice to have feature
		}

		/**
		 * Remove dependencies from the other apps.
		 * Apps are deployed in reverse order by the deployer, so the last app deployed in a stream
		 * is generally the app that should carry the dependency annotation. This makes most sense
		 * when the Source app is exposed as a Route.
		 *
		 * Below removes the annotation from the other services as they should no carry the depedency annotation.
		 * I.e. only one app per stream should depend on the others.
		 */
		client.services()
			.withLabel("spring-group-id",
				request.getDeploymentProperties().get(AppDeployer.GROUP_PROPERTY_KEY))
			.list().getItems()
				.forEach(service -> client.services()
					.withName(service.getMetadata().getName())
					.edit()
					.editMetadata()
						.removeFromAnnotations("service.alpha.openshift.io/dependencies")
					.endMetadata()
					.done()
				);

		return dependencies;
	}
}
