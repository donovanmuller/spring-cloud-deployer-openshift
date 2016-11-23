package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;

@ConfigurationProperties(prefix = "spring.cloud.deployer.openshift")
public class OpenShiftDeployerProperties extends KubernetesDeployerProperties {

	/**
	 * Global override for forcing OpenShift Build's of every application
	 */
	private boolean forceBuild;

	/**
	 * See https://docs.openshift.org/latest/architecture/core_concepts/routes.html#route- hostnames
	 */
	private String defaultRoutingSubdomain = "router.default.svc.cluster.local";

	/**
	 * The default image tag to associate with {@link io.fabric8.openshift.api.model.Build} targets
	 * and {@link io.fabric8.openshift.api.model.DeploymentConfig}'s
	 */
	private String defaultImageTag = "latest";

	/**
	 * Delay in milliseconds to wait for resources to be undeployed.
	 */
	private long undeployDelay = 1000;

	public boolean isForceBuild() {
		return forceBuild;
	}

	public void setForceBuild(boolean forceBuild) {
		this.forceBuild = forceBuild;
	}

	public String getDefaultRoutingSubdomain() {
		return defaultRoutingSubdomain;
	}

	public void setDefaultRoutingSubdomain(String defaultRoutingSubdomain) {
		this.defaultRoutingSubdomain = defaultRoutingSubdomain;
	}

	public String getDefaultImageTag() {
		return defaultImageTag;
	}

	public void setDefaultImageTag(final String defaultImageTag) {
		this.defaultImageTag = defaultImageTag;
	}

	public long getUndeployDelay() {
		return undeployDelay;
	}

	public void setUndeployDelay(final long undeployDelay) {
		this.undeployDelay = undeployDelay;
	}
}
