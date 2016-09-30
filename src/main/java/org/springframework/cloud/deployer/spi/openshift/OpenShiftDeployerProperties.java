package org.springframework.cloud.deployer.spi.openshift;

import io.fabric8.kubernetes.api.model.Volume;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "spring.cloud.deployer.openshift")
public class OpenShiftDeployerProperties {

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
	 * The default image tag to associate with
	 * {@link io.fabric8.openshift.api.model.Build} targets and
	 * {@link io.fabric8.openshift.api.model.DeploymentConfig}'s
	 */
	private String defaultImageTag = "latest";

	/**
	 * The {@link io.fabric8.kubernetes.api.model.Volume}s that this deployer server supports.
	 * The {@link io.fabric8.kubernetes.api.model.VolumeMount}s will
	 * be defined via application specific properties.
	 */
	private List<Volume> volumes = new ArrayList<>();

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

	public List<Volume> getVolumes() {
		return volumes;
	}

	public void setVolumes(final List<Volume> volumes) {
		this.volumes = volumes;
	}
}
