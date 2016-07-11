package org.springframework.cloud.deployer.spi.openshift;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAppDeployer;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.factories.DeploymentConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.RouteFactory;
import org.springframework.cloud.deployer.spi.openshift.factories.ServiceFactory;
import org.springframework.util.StringUtils;

import com.google.common.collect.Iterables;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftAppDeployer extends KubernetesAppDeployer implements AppDeployer {

	private static Logger logger = LoggerFactory.getLogger(OpenShiftAppDeployer.class);

	private ContainerFactory containerFactory;
	private KubernetesDeployerProperties properties;
	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private OpenShiftClient client;

	public OpenShiftAppDeployer(KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			KubernetesClient client, ContainerFactory containerFactory) {
		super(properties, client);

		this.properties = properties;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.client = (OpenShiftClient) client;
		this.containerFactory = containerFactory;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		logger.info("Deploying application: {}", request.getDefinition());

		String appId = createDeploymentId(request);

		if (!status(appId).getState().equals(DeploymentState.unknown)) {
			throw new IllegalStateException(
					String.format("App '%s' is already deployed", appId));
		}

		List<ObjectFactory> factories = populateOpenShiftObjects(request, appId);
		factories.forEach(factory -> factory.addObject(request, appId));
		factories.forEach(factory -> factory.applyObject(request, appId));

		return appId;
	}

	@Override
	public void undeploy(String appId) {
		logger.info("Undeploying application: {}", appId);

		// don't delete BuildConfig/Builds
		client.services().withLabelIn(SPRING_APP_KEY, appId).delete();
		client.routes().withLabelIn(SPRING_APP_KEY, appId).delete();
		client.replicationControllers().withLabelIn(SPRING_APP_KEY, appId).delete();
		client.deploymentConfigs().withLabelIn(SPRING_APP_KEY, appId).delete();
		client.pods().withLabelIn(SPRING_APP_KEY, appId).delete();
	}

	/**
	 * An {@link OpenShiftAppInstanceStatus} includes the Build phases in addition to the
	 * implementation in
	 * {@link org.springframework.cloud.deployer.spi.kubernetes.AbstractKubernetesDeployer#buildAppStatus}
	 */
	@Override
	protected AppStatus buildAppStatus(KubernetesDeployerProperties properties,
			String appId, PodList list) {
		AppStatus.Builder statusBuilder = AppStatus.of(appId);

		List<Build> builds = client.builds().withLabelIn(SPRING_APP_KEY, appId).list()
				.getItems();
		Build build = (builds.isEmpty()) ? null : Iterables.getLast(builds);

		if (list == null) {
			statusBuilder
					.with(new OpenShiftAppInstanceStatus(appId, null, properties, build));
		}
		else {
			for (Pod pod : list.getItems()) {
				statusBuilder.with(
						new OpenShiftAppInstanceStatus(appId, pod, properties, build));
			}
		}

		return statusBuilder.build();
	}

	/**
	 * Populate the OpenShift objects that will be created/updated and applied.
	 *
	 * @param request
	 * @param appId
	 * @return list of {@link ObjectFactory}'s
	 */
	protected List<ObjectFactory> populateOpenShiftObjects(AppDeploymentRequest request,
			String appId) {
		List<ObjectFactory> factories = new ArrayList<>();

		Map<String, String> labels = createIdMap(appId, request, null);
		int externalPort = configureExternalPort(request);

		Container container = getContainerFactory().create(createDeploymentId(request),
				request, externalPort, null);

		factories.add(getDeploymentConfigFactory(request, labels, container));
		factories.add(new ServiceFactory(getClient(), externalPort, labels));

		if (createRoute(request)) {
			factories.add(new RouteFactory(getClient(), getOpenShiftDeployerProperties(),
					externalPort, labels));
		}

		return factories;
	}

	protected DeploymentConfigFactory getDeploymentConfigFactory(
			AppDeploymentRequest request, Map<String, String> labels,
			Container container) {
		return new DeploymentConfigFactory(getClient(), container, labels,
				getResourceRequirements(request));
	}

	/**
	 * Create an OpenShift Route if either of these two deployment properties are
	 * specified:
	 *
	 * <ul>
	 * <li>spring.cloud.deployer.kubernetes.createLoadBalancer</li>
	 * <li>spring.cloud.deployer.openshift.createRoute</li>
	 * </ul>
	 *
	 * @param request
	 * @return true if the Route object should be created
	 */
	protected boolean createRoute(AppDeploymentRequest request) {
		boolean createRoute = false;
		String createRouteProperty = request.getDeploymentProperties().getOrDefault(
				OpenShiftDeploymentPropertyKeys.KUBERNETES_CREATE_LOAD_BALANCER,
				request.getDeploymentProperties()
						.get(OpenShiftDeploymentPropertyKeys.OPENSHIFT_CREATE_ROUTE));
		if (StringUtils.isEmpty(createRouteProperty)) {
			createRoute = properties.isCreateLoadBalancer();
		}
		else {
			if (Boolean.parseBoolean(createRouteProperty.toLowerCase())) {
				createRoute = true;
			}
		}

		return createRoute;
	}

	protected ResourceRequirements getResourceRequirements(AppDeploymentRequest request) {
		return new ResourceRequirements(deduceResourceLimits(properties, request), null);
	}

	protected OpenShiftClient getClient() {
		return client;
	}

	protected KubernetesDeployerProperties getProperties() {
		return properties;
	}

	protected OpenShiftDeployerProperties getOpenShiftDeployerProperties() {
		return openShiftDeployerProperties;
	}

	protected ContainerFactory getContainerFactory() {
		return containerFactory;
	}
}
