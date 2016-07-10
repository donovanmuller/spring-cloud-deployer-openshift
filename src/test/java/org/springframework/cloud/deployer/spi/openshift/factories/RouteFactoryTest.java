package org.springframework.cloud.deployer.spi.openshift.factories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.client.mock.OpenShiftMockServerTestBase;

public class RouteFactoryTest extends OpenShiftMockServerTestBase {

	private RouteFactory routeFactory;

	@Test
	public void buildRoute() {
		routeFactory = new RouteFactory(getOpenshiftClient(),
				new OpenShiftDeployerProperties(), 7777, null);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class));

		Route route = routeFactory.build(request, "testapp-source", 7777, null);

		assertThat(route.getSpec().getHost())
				.isEqualTo("testapp-source.test.router.default.svc.cluster.local");
	}

	@Test
	public void buildRouteWithCustomRoutingSubdomain() {
		OpenShiftDeployerProperties openShiftDeployerProperties = new OpenShiftDeployerProperties();
		openShiftDeployerProperties.setDefaultRoutingSubdomain("ose.test.com");
		routeFactory = new RouteFactory(getOpenshiftClient(), openShiftDeployerProperties,
				7777, null);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class));

		Route route = routeFactory.build(request, "testapp-source", 7777, null);

		assertThat(route.getSpec().getHost())
				.isEqualTo("testapp-source.test.ose.test.com");
	}

	@Test
	public void buildRouteWithHostname() {
		routeFactory = new RouteFactory(getOpenshiftClient(),
				new OpenShiftDeployerProperties(), 7777, null);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class),
				ImmutableMap.of(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_ROUTE_HOSTNAME,
						"www.test.com"));

		Route route = routeFactory.build(request, "testapp-source", 7777, null);

		assertThat(route.getSpec().getHost()).isEqualTo("www.test.com");
	}

}
