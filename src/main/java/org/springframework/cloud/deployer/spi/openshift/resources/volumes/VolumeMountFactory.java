package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.bind.YamlConfigurationFactory;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;

import io.fabric8.kubernetes.api.model.VolumeMount;

/**
 * Use the Fabric8 {@link VolumeMount} model to allow all volume plugins currently supported. Volume
 * mount deployment properties are specified in YAML format:
 *
 * <code>
 *     spring.cloud.deployer.kubernetes.volumeMounts=[{name: 'testhostpath', mountPath: '/test/hostPath'},
 *     	{name: 'testpvc', mountPath: '/test/pvc'}, {name: 'testnfs', mountPath: '/test/nfs'}]
 * </code>
 *
 * Volume mounts can be specified as deployer properties as well as app deployment properties.
 * Deployment properties override deployer properties.
 */
public class VolumeMountFactory implements ObjectFactory<List<VolumeMount>> {

	private OpenShiftDeployerProperties properties;

	public VolumeMountFactory(OpenShiftDeployerProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<VolumeMount> addObject(AppDeploymentRequest request, String appId) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();
		volumeMounts.addAll(getVolumeMounts(request));
		return new ArrayList<>(volumeMounts);
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// this object cannot be applied by itself
	}

	private List<VolumeMount> getVolumeMounts(AppDeploymentRequest request) {
		List<VolumeMount> volumeMounts = new ArrayList<>();

		String volumeMountDeploymentProperty = request.getDeploymentProperties()
				.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_VOLUME_MOUNTS, StringUtils.EMPTY);
		if (!org.springframework.util.StringUtils.isEmpty(volumeMountDeploymentProperty)) {
			YamlConfigurationFactory<KubernetesDeployerProperties> volumeMountYamlConfigurationFactory = new YamlConfigurationFactory<>(
					KubernetesDeployerProperties.class);
			volumeMountYamlConfigurationFactory.setYaml("{ volumeMounts: " + volumeMountDeploymentProperty + " }");
			try {
				volumeMountYamlConfigurationFactory.afterPropertiesSet();
				volumeMounts.addAll(volumeMountYamlConfigurationFactory.getObject().getVolumeMounts());
			}
			catch (Exception e) {
				throw new IllegalArgumentException(
						String.format("Invalid volume mount '%s'", volumeMountDeploymentProperty), e);
			}
		}
		// only add volume mounts that have not already been added, based on the volume mount's name
		// i.e. allow provided deployment volume mounts to override deployer defined volume mounts
		volumeMounts.addAll(properties.getVolumeMounts().stream()
				.filter(volumeMount -> volumeMounts.stream()
						.noneMatch(existingVolumeMount -> existingVolumeMount.getName().equals(volumeMount.getName())))
				.collect(Collectors.toList()));

		return volumeMounts;
	}
}
