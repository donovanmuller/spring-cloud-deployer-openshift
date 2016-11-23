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

import io.fabric8.kubernetes.api.model.Volume;

/**
 * Use the Fabric8 {@link Volume} model to allow all volume plugins currently supported. Volume
 * deployment properties are specified in YAML format:
 *
 * <code>
 *     spring.cloud.deployer.openshift.volumes=[{name: testhostpath, hostPath: { path: '/test/override/hostPath' }},
 *     	{name: 'testpvc', persistentVolumeClaim: { claimName: 'testClaim', readOnly: 'true' }},
 *     	{name: 'testnfs', nfs: { server: '10.0.0.1:111', path: '/test/nfs' }}]
 * </code>
 *
 * Volumes can be specified as deployer properties as well as app deployment properties. Deployment
 * properties override deployer properties.
 */
public class VolumeFactory implements ObjectFactory<List<Volume>> {

	private OpenShiftDeployerProperties properties;

	public VolumeFactory(OpenShiftDeployerProperties properties) {
		this.properties = properties;
	}

	@Override
	public List<Volume> addObject(AppDeploymentRequest request, String appId) {
		Set<Volume> volumes = new LinkedHashSet<>();
		volumes.addAll(getVolumes(request));
		return new ArrayList<>(volumes);
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// this object cannot be applied by itself
	}

	private List<Volume> getVolumes(AppDeploymentRequest request) {
		List<Volume> volumes = new ArrayList<>();

		String volumeDeploymentProperty = request.getDeploymentProperties()
				.getOrDefault(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_VOLUMES, StringUtils.EMPTY);
		if (!org.springframework.util.StringUtils.isEmpty(volumeDeploymentProperty)) {
			YamlConfigurationFactory<KubernetesDeployerProperties> volumeYamlConfigurationFactory = new YamlConfigurationFactory<>(
					KubernetesDeployerProperties.class);
			volumeYamlConfigurationFactory.setYaml("{ volumes: " + volumeDeploymentProperty + " }");
			try {
				volumeYamlConfigurationFactory.afterPropertiesSet();
				volumes.addAll(volumeYamlConfigurationFactory.getObject().getVolumes());
			}
			catch (Exception e) {
				throw new IllegalArgumentException(String.format("Invalid volume '%s'", volumeDeploymentProperty), e);
			}
		}
		// only add volumes that have not already been added, based on the volume's name
		// i.e. allow provided deployment volumes to override deployer defined volumes
		volumes.addAll(properties.getVolumes().stream()
				.filter(volume -> volumes.stream()
						.noneMatch(existingVolume -> existingVolume.getName().equals(volume.getName())))
				.collect(Collectors.toList()));

		return volumes;
	}
}
