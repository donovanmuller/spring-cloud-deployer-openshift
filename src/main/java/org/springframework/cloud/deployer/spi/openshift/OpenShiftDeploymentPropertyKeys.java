package org.springframework.cloud.deployer.spi.openshift;

public interface OpenShiftDeploymentPropertyKeys {

	/**
	 * If true, will force an OpenShift {@link io.fabric8.openshift.api.model.Build} for
	 * application and ignore existing Builds
	 */
	String OPENSHIFT_BUILD_FORCE = "spring.cloud.deployer.openshift.forceBuild";

	/**
	 * OpenShift {@link io.fabric8.kubernetes.api.model.ServiceAccount} that containers
	 * should run under. See
	 * https://docs.openshift.org/latest/dev_guide/service_accounts.html
	 */
	String OPENSHIFT_DEPLOYMENT_SERVICE_ACCOUNT = "spring.cloud.deployer.openshift.deployment.service.account";

	/**
	 * The Docker image tag used when creating a
	 * {@link io.fabric8.openshift.api.model.DeploymentConfig} with a
	 * {@link io.fabric8.openshift.api.model.ImageChangeTrigger} trigger.
	 */
	String OPENSHIFT_DEPLOYMENT_IMAGE_TAG = "spring.cloud.deployer.openshift.image.tag";

	/**
	 * An inline Dockerfile that will be used as the build input. This Dockerfile will
	 * override all other Dockerfile usage strategies. See
	 * https://docs.openshift.org/latest/dev_guide/builds.html#dockerfile-source
	 */
	String OPENSHIFT_DEPLOYMENT_DOCKERFILE = "spring.cloud.deployer.openshift.deployment.dockerfile";

	/**
	 * An optional nodeSelector to indicate where to schedule pods. All of the following
	 * are valid:
	 * <code>spring.cloud.deployer.openshift.deployment.nodeSelector=region: primary,role:processor,label : something</code>
	 * See https://docs.openshift.org/latest/dev_guide/deployments.html#assigning-pods-to-
	 * specific-nodes
	 */
	String OPENSHIFT_DEPLOYMENT_NODE_SELECTOR = "spring.cloud.deployer.openshift.deployment.nodeSelector";

	/**
	 * An optional host value for a {@link io.fabric8.openshift.api.model.Route}. This
	 * value will override the default of combining the appId, namespace/project and
	 * default routing subdomain (see
	 * {@link OpenShiftDeployerProperties#defaultRoutingSubdomain}). See
	 * https://docs.openshift.org/latest/architecture/core_concepts/routes.html#route-
	 * hostnames
	 */
	String OPENSHIFT_DEPLOYMENT_ROUTE_HOSTNAME = "spring.cloud.deployer.openshift.deployment.route.host";

	/**
	 * An optional volumeMount/volume mapping for the hostPath volume plugin.
	 * See https://docs.openshift.org/latest/admin_guide/manage_scc.html#use-the-hostpath-volume-plugin
	 * Mappings are specified as follows:
	 * <code>spring.cloud.deployer.openshift.deployment.hostpath.volume=volumeName:mountPath:hostPath</code>
	 */
	String OPENSHIFT_DEPLOYMENT_HOSTPATH_VOLUME = "spring.cloud.deployer.openshift.deployment.hostpath.volume";

	/**
	 * The below two properties are equal (a Kubernetes "LoadBalancer" is equivalent to a
	 * Route in OpenShift - see
	 * {@link org.springframework.cloud.deployer.spi.kubernetes.KubernetesAppDeployer}) in
	 * meaning. Whether or not to create a OpenShift Route. See
	 * https://docs.openshift.org/latest/dev_guide/routes.html
	 */
	String KUBERNETES_CREATE_LOAD_BALANCER = "spring.cloud.deployer.kubernetes.createLoadBalancer";
	String OPENSHIFT_CREATE_ROUTE = "spring.cloud.deployer.openshift.createRoute";
}
