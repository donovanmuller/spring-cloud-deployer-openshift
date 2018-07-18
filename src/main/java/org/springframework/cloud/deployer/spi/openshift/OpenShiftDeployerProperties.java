package org.springframework.cloud.deployer.spi.openshift;

import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.DeploymentConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;

@ConfigurationProperties(prefix = "spring.cloud.deployer.openshift")
public class OpenShiftDeployerProperties extends KubernetesDeployerProperties {

	/**
	 * Global override for forcing OpenShift Build's of every application
	 */
	private boolean forceBuild;

	/**
	 * See https://docs.openshift.org/latest/architecture/core_concepts/routes.html#route-
	 * hostnames
	 */
	private String defaultRoutingSubdomain = "router.default.svc.cluster.local";

	/**
	 * The default image tag to associate with {@link Build} targets and
	 * {@link DeploymentConfig}'s
	 */
	private String defaultImageTag = "latest";

	/**
	 * Delay in milliseconds to wait for resources to be undeployed.
	 */
	private long undeployDelay = 1000;

	/**
	 * When deploying Maven resource apps, use this provided default Dockerfile. Allowable
	 * values are <code>Dockerfile.artifactory</code> or <code>Dockerfile.nexus</code>.
	 * The Dockerfiles are targeted at the two most common Maven repository distributions,
	 * Nexus and Artifactory, where the API used to download the Maven artifacts is
	 * specific to each distribution. <code>Dockerfile.artifactory</code> is the default
	 * because that is the distribution used by the Spring Maven repository.
	 */
	private String defaultDockerfile = "Dockerfile.artifactory";

	/**
	 * Override an images existing registry with this registry value. This can be used if
	 * the applications image is pushed to the internal registry using the externally
	 * routable address but should be referenced using the internal registry Service.
	 */
	private String dockerRegistryOverride;

	/**
	 * This property is used in conjunction with <code>dockerRegistryOverride</code> and
	 * must be set to the namespace/project name where the server is running. I.e. if the
	 * project where the server is running is called 'my-project', then this value should
	 * be set to 'my-project'. Easiest way to set this is by using the
	 * <code>metadata.namespace</code> value if using a Template.
	 */
	private String imageProjectName;

	/**
	 * The default S2I build image to use for Maven resource. See
	 * https://github.com/fabric8io-images/s2i
	 */
	private String defaultS2iImage = "fabric8/s2i-java:2.2";

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

	public String getDefaultImageNamespace() {
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

	public String getDockerRegistryOverride() {
		return dockerRegistryOverride;
	}

	public void setDockerRegistryOverride(String dockerRegistryOverride) {
		this.dockerRegistryOverride = dockerRegistryOverride;
	}

	public String getImageProjectName() {
		return imageProjectName;
	}

	public void setImageProjectName(String imageProjectName) {
		this.imageProjectName = imageProjectName;
	}

	public String getDefaultS2iImage() {
		return defaultS2iImage;
	}

	public void setDefaultS2iImage(String defaultS2iImage) {
		this.defaultS2iImage = defaultS2iImage;
	}

}
