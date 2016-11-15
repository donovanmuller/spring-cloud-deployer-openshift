package org.springframework.cloud.deployer.spi.openshift.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.client.mock.OpenShiftServer;

public class ServiceFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private ServiceFactory serviceFactory;

	@Test
	public void buildServiceWithTargetPort() {
		serviceFactory = new ServiceFactory(server.getOpenshiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first()
				.isEqualTo(new ServicePortBuilder().withNewTargetPort(8080).withPort(8080).build());
	}

	@Test
	public void buildServiceWithRandomNodePort() {
		serviceFactory = new ServiceFactory(server.getOpenshiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT, "true"));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first().isEqualTo(new ServicePortBuilder().withPort(8080).build());
	}

	@Test
	public void buildServiceWithNodePort() {
		serviceFactory = new ServiceFactory(server.getOpenshiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT, "30000"));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first()
				.isEqualTo(new ServicePortBuilder().withNodePort(30000).withPort(8080).build());
	}
}
