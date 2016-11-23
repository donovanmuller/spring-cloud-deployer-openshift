package org.springframework.cloud.deployer.spi.openshift.resources.volumes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;

public class VolumeFactoryTest {

	private VolumeFactory volumeFactory;

	@Test
	public void addVolumeMounts() {
		OpenShiftDeployerProperties properties = new OpenShiftDeployerProperties();
		properties.setVolumes(ImmutableList
				.of(new VolumeBuilder().withName("testpvc").withNewPersistentVolumeClaim("testClaim", true).build()));
		volumeFactory = new VolumeFactory(properties);
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				//@formatter:off
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.deployment.volumes",
					"["
						+ "{name: testhostpath, hostPath: { path: '/test/override/hostPath' }},"
						+ "{name: 'testnfs', nfs: { server: '192.168.1.1:111', path: '/test/override/nfs' }} "
					+ "]"));
				//@formatter:on

		List<Volume> volumes = volumeFactory.addObject(request, "1");

		assertThat(volumes).containsOnly(
				new VolumeBuilder().withName("testhostpath").withNewHostPath("/test/override/hostPath").build(),
				new VolumeBuilder().withName("testpvc").withNewPersistentVolumeClaim("testClaim", true).build(),
				new VolumeBuilder().withName("testnfs").withNewNfs("/test/override/nfs", null, "192.168.1.1:111")
						.build());
	}
}
