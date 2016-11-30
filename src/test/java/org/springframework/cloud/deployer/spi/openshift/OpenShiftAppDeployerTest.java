package org.springframework.cloud.deployer.spi.openshift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

public class OpenShiftAppDeployerTest {

	@Test
	public void enableKubernetedDeployerCompatibility() {
		AppDeploymentRequest openShiftRequest = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of("spring.cloud.deployer.openshift.memory", "8Mi"));
		AppDeploymentRequest request = new OpenShiftAppDeployer(new OpenShiftDeployerProperties(), null, null)
				.enableKubernetesDeployerCompatibility(openShiftRequest);

		assertThat(request.getDeploymentProperties()).contains(
				new ImmutablePair<>("spring.cloud.deployer.kubernetes.memory", "8Mi"),
				new ImmutablePair<>("spring.cloud.deployer.openshift.memory", "8Mi"));
	}

}
