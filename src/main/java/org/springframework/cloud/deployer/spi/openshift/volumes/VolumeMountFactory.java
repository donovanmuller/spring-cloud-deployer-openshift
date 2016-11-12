package org.springframework.cloud.deployer.spi.openshift.volumes;

import io.fabric8.kubernetes.api.model.VolumeMount;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

public class VolumeMountFactory {

	private static final Logger logger = LoggerFactory.getLogger(VolumeMountFactory.class);

	private ConfigServicePropertySourceLocator configServicePropertySourceLocator;

	public VolumeMountFactory(ConfigServicePropertySourceLocator configServicePropertySourceLocator) {
		this.configServicePropertySourceLocator = configServicePropertySourceLocator;
	}

	public List<VolumeMount> create(String appId, Map<String, String> deploymentProperties) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();
		VolumeMountProperties volumeMountProperties = getVolumeMountProperties(deploymentProperties);
		volumeMounts.addAll(volumeMountProperties.getVolumeMounts());
		volumeMounts.addAll(fetchVolumeMountsFromConfig(appId, volumeMountProperties));
		return new ArrayList<>(volumeMounts);
	}

	private Set<VolumeMount> fetchVolumeMountsFromConfig(String appId, VolumeMountProperties volumeMountProperties) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();

		ConfigurableEnvironment appEnvironment = new StandardEnvironment();
		appEnvironment.getPropertySources().addFirst(
			new MapPropertySource("deployer-openshift-override",
				Collections.singletonMap("spring.application.name", appId)));

		PropertySource<?> propertySource = configServicePropertySourceLocator.locate(appEnvironment);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource);

		try {
			PropertiesConfigurationFactory<VolumeMountProperties> factory =
				new PropertiesConfigurationFactory<>(volumeMountProperties);
			factory.setPropertySources(propertySources);
			factory.afterPropertiesSet();
			VolumeMountProperties configVolumeProperties = factory.getObject();
			volumeMounts.addAll(configVolumeProperties.getVolumeMounts());
		}
		catch (Exception e) {
			logger.warn("Could not get volume mounts configuration for app '{}' from config server: '{}'", appId,
				e.getMessage());
		}

		return volumeMounts;
	}

	private VolumeMountProperties getVolumeMountProperties(Map<String, String> properties) {
		VolumeMountProperties volumeMountProperties = new VolumeMountProperties();

		String volumeMounts = properties
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

		return volumeMountProperties;
	}
}
