package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockPropertySource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.VolumeMount;

public class VolumeMountConfigServerFactoryTest {

	private VolumeMountFactory volumeMountFactory;
	private ConfigServicePropertySourceLocator configServicePropertySourceLocator;

	@Before
	public void setup() {
		configServicePropertySourceLocator = new ConfigServicePropertySourceLocator(null) {

			@Override
			public PropertySource<?> locate(Environment environment) {
				return new MockPropertySource().withProperty("volumeMounts[0].name", "testVolume")
						.withProperty("volumeMounts[0].mountPath", "/mnt/test");
			}
		};
	}

	@Test
	public void addVolumeMounts() {
		volumeMountFactory = new VolumeMountConfigServerFactory(configServicePropertySourceLocator,
				new OpenShiftDeployerProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), null);

		List<VolumeMount> volumeMounts = volumeMountFactory.addObject(request, "1");

		assertThat(volumeMounts).first().isEqualTo(new VolumeMount("/mnt/test", "testVolume", null, null));
	}

	@Test
	public void addVolumeMountsWithOverride() {
		volumeMountFactory = new VolumeMountConfigServerFactory(configServicePropertySourceLocator,
				new OpenShiftDeployerProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.deployment.volumeMounts",
						"[{name: 'testVolume', mountPath: '/mnt/test/overridden'}]"));

		List<VolumeMount> volumeMounts = volumeMountFactory.addObject(request, "1");

		assertThat(volumeMounts).first().isEqualTo(new VolumeMount("/mnt/test/overridden", "testVolume", null, null));
	}

	@Test
	public void addVolumeMountsWithNoConfig() {
		volumeMountFactory = new VolumeMountConfigServerFactory(new ConfigServicePropertySourceLocator(null) {

			@Override
			public PropertySource<?> locate(Environment environment) {
				return new MockPropertySource();
			}
		}, new OpenShiftDeployerProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.deployment.volumeMounts",
						"[{name: 'testVolume', mountPath: '/mnt/test/deployer'}]"));

		List<VolumeMount> volumeMounts = volumeMountFactory.addObject(request, "1");

		assertThat(volumeMounts).first().isEqualTo(new VolumeMount("/mnt/test/deployer", "testVolume", null, null));
	}
}
