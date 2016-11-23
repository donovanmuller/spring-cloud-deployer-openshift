package org.springframework.cloud.deployer.spi.openshift.resources.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServiceListBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.openshift.client.mock.OpenShiftServer;

public class ServiceFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private ServiceFactory serviceFactory;

	@Before
	public void setup() {
		server.expect().withPath("/api/v1/namespaces/test/services?labelSelector=spring.cloud.deployer.group")
				.andReturn(200, new ServiceListBuilder().build()).once();
	}

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

	@Test
	public void buildServiceWithServicesGrouped() {
		//@formatter:off
		server.expect().get().withPath("/api/v1/namespaces/test/services?" +
				"labelSelector=spring.cloud.deployer.group%3Dtest-stream")
			.andReturn(200, new ServiceListBuilder()
				.withItems(new ServiceBuilder()
					.withNewMetadata()
						.withName("test-source")
					.endMetadata()
					.build(), new ServiceBuilder()
					.withNewMetadata()
						.withName("test-processor")
					.endMetadata()
					.build())
				.build())
			.once();
		//@formatter:on
		serviceFactory = new ServiceFactory(server.getOpenshiftClient(), 8080, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-sink", null),
				mock(Resource.class), ImmutableMap.of(AppDeployer.GROUP_PROPERTY_KEY, "test-stream"));

		Service service = serviceFactory.build(request, "testapp-sink", 7777, null);

		assertThat(service.getMetadata().getAnnotations()).containsEntry("service.alpha.openshift.io/dependencies",
				"[{'name': 'test-source','kind': 'Service'}]");
	}
}
