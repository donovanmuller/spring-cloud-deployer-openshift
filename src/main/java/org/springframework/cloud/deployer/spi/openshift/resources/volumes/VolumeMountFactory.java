package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import static java.lang.String.format;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.factories.ObjectFactory;
import org.springframework.util.Assert;

import io.fabric8.kubernetes.api.model.VolumeMount;

/**
 * Use the Fabric8 {@link VolumeMount} model to allow all volume plugins currently supported. Volume
 * mounts can be configured using deployment properties. The structure of the volume mount
 * configuration is determined by the volume plugin used. Below are a few examples:
 *
 * <code>
 *     TODO
 * </code>
 */
public class VolumeMountFactory implements ObjectFactory<List<VolumeMount>> {

	@Override
	public List<VolumeMount> addObject(AppDeploymentRequest request, String appId) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();
		volumeMounts.addAll(getVolumeMountsFromDeployment(request));
		return new ArrayList<>(volumeMounts);
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// this object cannot be applied by itself
	}

	/**
	 * Get the {@link OpenShiftDeploymentPropertyKeys#OPENSHIFT_DEPLOYMENT_VOLUME_MOUNTS} deployment
	 * proeprty from the {@link AppDeploymentRequest}. This covers both common deployer properties
	 * set a deployer server level as well as deployment properties specified (on stream etc.) on
	 * deployment.
	 *
	 * @param request
	 * @return the configured volume mounts
	 */
	private List<VolumeMount> getVolumeMountsFromDeployment(AppDeploymentRequest request) {
		VolumeMountProperties volumeMountProperties = new VolumeMountProperties();

		String volumeMounts = request.getDeploymentProperties()
				.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_VOLUME_MOUNTS, StringUtils.EMPTY);

		if (StringUtils.isNotBlank(volumeMounts)) {
			String[] volumePairs = volumeMounts.split(",");
			for (String volumePair : volumePairs) {
				String[] volume = volumePair.split(":");
				Assert.isTrue(volume.length <= 3, format("Invalid volume mount: '{}'", volumePair));

				volumeMountProperties
						.addVolumeMount(new VolumeMount(volume[1], volume[0],
								Boolean.valueOf(StringUtils.defaultIfBlank(
										volume.length == 3 ? volume[2] : StringUtils.EMPTY, Boolean.FALSE.toString())),
								null));
			}
		}

		return volumeMountProperties.getVolumeMounts();
	}
}
