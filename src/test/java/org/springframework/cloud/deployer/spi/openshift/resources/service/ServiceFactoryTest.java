package org.springframework.cloud.deployer.spi.openshift.resources.service;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.client.server.mock.OpenShiftMockServer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class ServiceFactoryTest {

	public OpenShiftMockServer server = new OpenShiftMockServer();

	private ServiceFactory serviceFactory;

	@Before
	public void setup() {
		server.init();
		server.expect().withPath("/api/v1/namespaces/test/services?labelSelector=spring.cloud.deployer.group")
				.andReturn(200, new ServiceListBuilder().build()).times(2);
	}

	@Test
	public void buildServiceWithTargetPort() {
		server.expect().get().withPath("/api/v1/namespaces/test/services?" + "labelSelector=spring-group-id")
				.andReturn(200, new ServiceListBuilder().build()).times(2);

		serviceFactory = new ServiceFactory(server.createOpenShiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first()
				.isEqualTo(new ServicePortBuilder().withNewTargetPort(8080).withPort(8080).build());
	}

	@Test
	public void buildServiceWithRandomNodePort() {
		server.expect().get().withPath("/api/v1/namespaces/test/services?" + "labelSelector=spring-group-id")
				.andReturn(200, new ServiceListBuilder().build()).times(2);

		serviceFactory = new ServiceFactory(server.createOpenShiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT, "true"));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first().isEqualTo(new ServicePortBuilder().withPort(8080).build());
	}

	@Test
	public void buildServiceWithNodePort() {
		server.expect().get().withPath("/api/v1/namespaces/test/services?" + "labelSelector=spring-group-id")
				.andReturn(200, new ServiceListBuilder().build()).times(2);

		serviceFactory = new ServiceFactory(server.createOpenShiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_NODE_PORT, "30000"));

		Service service = serviceFactory.build(request, "testapp-source", 7777, null);

		assertThat(service.getSpec().getPorts()).first()
				.isEqualTo(new ServicePortBuilder().withNodePort(30000).withPort(8080).build());
	}
}
