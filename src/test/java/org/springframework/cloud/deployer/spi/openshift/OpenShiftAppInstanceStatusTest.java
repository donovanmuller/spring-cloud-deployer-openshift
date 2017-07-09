package org.springframework.cloud.deployer.spi.openshift;

import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.openshift.api.model.BuildBuilder;
import org.junit.Test;

import org.springframework.cloud.deployer.spi.app.DeploymentState;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenShiftAppInstanceStatusTest {

	private OpenShiftAppInstanceStatus appInstanceStatus;

	@Test
	public void getState() {
		appInstanceStatus = new OpenShiftAppInstanceStatus(
				new PodBuilder().withNewMetadata().withName("test-pod").endMetadata()
						.withNewStatus()
						.withContainerStatuses(
								new ContainerStatusBuilder().withReady(true).build())
						.withPhase("Running").endStatus().build(),
				null, new BuildBuilder().withNewStatus().withPhase("Completed")
						.endStatus().build());

		DeploymentState state = appInstanceStatus.getState();

		assertThat(state).isEqualTo(DeploymentState.deployed);
	}

	@Test
	public void getStateWithRunningBuild() {
		appInstanceStatus = new OpenShiftAppInstanceStatus(null, null, new BuildBuilder()
				.withNewStatus().withPhase("Running").endStatus().build());

		DeploymentState state = appInstanceStatus.getState();

		assertThat(state).isEqualTo(DeploymentState.deploying);
	}

}
