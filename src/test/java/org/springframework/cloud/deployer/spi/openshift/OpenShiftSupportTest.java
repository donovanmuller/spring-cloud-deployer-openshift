package org.springframework.cloud.deployer.spi.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

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
	public void toLabels() {
		ImmutableMap<String, String> properties = ImmutableMap.of(
				"spring.cloud.deployer.openshift.deployment.labels",
				"label1=value1,label2 = value2, label3 =value3");

		Map<String, String> labels = toLabels(properties);

		assertThat(labels).containsAllEntriesOf(
				ImmutableMap.of("label1", "value1", "label2", "value2", "label3", "value3"));
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
}
