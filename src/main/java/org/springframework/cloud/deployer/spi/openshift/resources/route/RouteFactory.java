package org.springframework.cloud.deployer.spi.openshift.resources.route;

import static java.lang.String.format;

import java.util.Map;
import java.util.Optional;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;

import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public class RouteFactory implements ObjectFactory<Route> {

	private OpenShiftClient client;
	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private Integer port;
	private Map<String, String> labels;

	public RouteFactory(OpenShiftClient client, OpenShiftDeployerProperties openShiftDeployerProperties, Integer port,
			Map<String, String> labels) {
		this.client = client;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.port = port;
		this.labels = labels;
	}

	@Override
	public Route addObject(AppDeploymentRequest request, String appId) {
		Route route = build(request, appId, port, labels);

		if (getExisting(appId).isPresent()) {
			route = this.client.routes().createOrReplace(route);
		}
		else {
			route = this.client.routes().create(route);
		}

		return route;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// do nothing
	}

	protected Optional<Route> getExisting(String name) {
		return Optional.ofNullable(client.routes().withName(name).fromServer().get());
	}

	protected Route build(AppDeploymentRequest request, String appId, Integer port, Map<String, String> labels) {
		//@formatter:off
        return new RouteBuilder()
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withHost(buildHost(request, appId))
                .withNewTo()
                    .withName(appId)
                    .withKind("Service")
                .endTo()
                .withNewPort()
                    .withNewTargetPort(port)
                .endPort()
            .endSpec()
            .build();
        //@formatter:on
	}

	/**
	 * Builds the <code>host</code> value for a Route. If there is a
	 * <code>spring.cloud.deployer.openshift.deployment.route.hostname</code> deployment variable
	 * with a value, this will be used as the <code>host</code> value for the Route (see
	 * https://docs.openshift.org/latest/architecture/core_concepts/routes.html#route- hostnames)
	 * Alternatively, the <code>host</code> value is built up using:
	 *
	 * <ul>
	 * <li>application Id</li>
	 * <li>the namespace currently connected too</li>
	 * <li>the configured default routing subdomain - see
	 * https://docs.openshift.org/latest/install_config/install/deploy_router.html#
	 * customizing-the-default-routing-subdomain</li>
	 * </ul>
	 *
	 * See https://docs.openshift.org/latest/dev_guide/routes.html
	 *
	 * @param request
	 * @param appId
	 * @return host value for the Route
	 */
	protected String buildHost(AppDeploymentRequest request, String appId) {
		return request.getDeploymentProperties()
				.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_ROUTE_HOSTNAME, format("%s.%s.%s",
						appId, client.getNamespace(), openShiftDeployerProperties.getDefaultRoutingSubdomain()));
	}
}
