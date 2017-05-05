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

	/**
	 * When deploying Maven resource apps, use this provided default Dockerfile. Allowable values
	 * are <code>Dockerfile.artifactory</code> or <code>Dockerfile.nexus</code>. The Dockerfiles are
	 * targeted at the two most common Maven repository distributions, Nexus and Artifactory, where
	 * the API used to download the Maven artifacts is specific to each distribution.
	 * <code>Dockerfile.artifactory</code> is the default because that is the distribution used by
	 * the Spring Maven repository.
	 */
	private String defaultDockerfile = "Dockerfile.artifactory";

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

	public String getDefaultImageNamespace(){
		return getNamespace();
	}

	public long getUndeployDelay() {
		return undeployDelay;
	}

	public void setUndeployDelay(final long undeployDelay) {
		this.undeployDelay = undeployDelay;
	}

	public String getDefaultDockerfile() {
		return defaultDockerfile;
	}

	public void setDefaultDockerfile(final String defaultDockerfile) {
		this.defaultDockerfile = defaultDockerfile;
	}
}
