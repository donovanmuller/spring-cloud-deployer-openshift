package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.VolumeMount;

public class VolumeMountFactoryTest {

	private VolumeMountFactory volumeMountFactory;

	@Test
	public void addVolumeMounts() {
		volumeMountFactory = new VolumeMountFactory(new OpenShiftDeployerProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.deployment.volumeMounts",
						"[{name: 'testVolume', mountPath: '/mnt/test'}]"));

		List<VolumeMount> volumeMounts = volumeMountFactory.addObject(request, "1");

		assertThat(volumeMounts).first().isEqualTo(new VolumeMount("/mnt/test", "testVolume", null, null));
	}

	@Test
	public void addVolumeMountsAsReadOnly() {
		volumeMountFactory = new VolumeMountFactory(new OpenShiftDeployerProperties());
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.deployment.volumeMounts",
						"[{name: 'testVolume', mountPath: '/mnt/test', readOnly: 'true'}]"));

		List<VolumeMount> volumeMounts = volumeMountFactory.addObject(request, "1");

		assertThat(volumeMounts).first().isEqualTo(new VolumeMount("/mnt/test", "testVolume", true, null));
	}
}
