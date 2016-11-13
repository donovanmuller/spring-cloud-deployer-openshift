package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.fabric8.kubernetes.api.model.VolumeMount;

public class VolumeMountProperties {

	private List<VolumeMount> volumeMounts = new ArrayList<>();

	public VolumeMountProperties() {
	}

	public VolumeMountProperties(VolumeMount... volumeMounts) {
		this.volumeMounts.addAll(Arrays.asList(volumeMounts));
	}

	public List<VolumeMount> getVolumeMounts() {
		return volumeMounts;
	}

	public void setVolumeMounts(List<VolumeMount> volumeMounts) {
		this.volumeMounts = volumeMounts;
	}

	public void addVolumeMount(VolumeMount volumeMount) {
		volumeMounts.add(volumeMount);
	}
}
