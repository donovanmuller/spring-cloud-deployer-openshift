package org.springframework.cloud.deployer.spi.openshift.maven;

import io.fabric8.kubernetes.api.model.VolumeMount;

import java.util.ArrayList;
import java.util.List;

public class VolumeMountProperties {

	private List<VolumeMount> volumeMounts = new ArrayList<>();

	public List<VolumeMount> getVolumeMounts() {
		return volumeMounts;
	}

	public void setVolumeMounts(final List<VolumeMount> volumeMounts) {
		this.volumeMounts = volumeMounts;
	}
}
