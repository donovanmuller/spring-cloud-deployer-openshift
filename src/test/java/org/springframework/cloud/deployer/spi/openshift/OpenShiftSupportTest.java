package org.springframework.cloud.deployer.spi.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class OpenShiftSupportTest implements OpenShiftSupport {

	@Test
	public void getEnvironmentVariable() {
		String[] properties = new String[] { "prop1=value1", "prop2=value2" };

		String value2 = getEnvironmentVariable(properties, "prop2");

		assertThat(value2).isEqualTo("value2");
	}

	@Test
	public void getNodeSelector() {
		ImmutableMap<String, String> properties = ImmutableMap.of(
				"spring.cloud.deployer.openshift.deployment.nodeSelector",
				"region: primary,role : node, label :test");

		Map<String, String> nodeSelectors = getNodeSelectors(properties);

		assertThat(nodeSelectors).containsAllEntriesOf(
				ImmutableMap.of("region", "primary", "role", "node", "label", "test"));
	}

	@Test
	public void getHostPathVolumes() {
		ImmutableMap<String, String> properties = ImmutableMap.of(
				"spring.cloud.deployer.openshift.deployment.hostpath.volume",
				"tmp:/tmp/container:/tmp,home:/home:/mount/home");

		Map<VolumeMount, Volume> volumes = getHostPathVolumes(properties);

		assertThat(volumes).containsAllEntriesOf(
				ImmutableMap.of(new VolumeMount("/tmp/container", "tmp", false),
					new VolumeBuilder()
						.withName("tmp")
						.withNewHostPath("/tmp")
						.build(),
					new VolumeMount("/home", "home", false),
					new VolumeBuilder()
						.withName("home")
						.withNewHostPath("/mount/home")
						.build()));
	}
}
