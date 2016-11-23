package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.bind.PropertiesConfigurationFactory;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

import io.fabric8.kubernetes.api.model.VolumeMount;

public class VolumeMountConfigServerFactory extends VolumeMountFactory {

	private static final Logger logger = LoggerFactory.getLogger(VolumeMountConfigServerFactory.class);

	private ConfigServicePropertySourceLocator configServicePropertySourceLocator;

	public VolumeMountConfigServerFactory(ConfigServicePropertySourceLocator configServicePropertySourceLocator,
			OpenShiftDeployerProperties openShiftDeployerProperties) {
		super(openShiftDeployerProperties);
		this.configServicePropertySourceLocator = configServicePropertySourceLocator;
	}

	@Override
	public List<VolumeMount> addObject(AppDeploymentRequest request, String appId) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();
		volumeMounts.addAll(super.addObject(request, appId));
		volumeMounts.addAll(fetchVolumeMountsFromConfigServer(appId));
		return new ArrayList<>(volumeMounts);
	}

	private Set<VolumeMount> fetchVolumeMountsFromConfigServer(String appId) {
		Set<VolumeMount> volumeMounts = new LinkedHashSet<>();

		ConfigurableEnvironment appEnvironment = new StandardEnvironment();
		appEnvironment.getPropertySources().addFirst(new MapPropertySource("deployer-openshift-override",
				Collections.singletonMap("spring.application.name", appId)));

		PropertySource<?> propertySource = configServicePropertySourceLocator.locate(appEnvironment);
		MutablePropertySources propertySources = new MutablePropertySources();
		propertySources.addFirst(propertySource);

		try {
			PropertiesConfigurationFactory<VolumeMountProperties> factory = new PropertiesConfigurationFactory<>(
					new VolumeMountProperties());
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
}
