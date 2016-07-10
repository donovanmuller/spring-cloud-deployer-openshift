package org.springframework.cloud.deployer.spi.openshift;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.cloud.deployer.spi.app.DeploymentState;

import io.fabric8.kubernetes.api.model.ContainerStatusBuilder;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.openshift.api.model.BuildBuilder;

public class OpenShiftAppInstanceStatusTest {

	private OpenShiftAppInstanceStatus appInstanceStatus;

	@Test
	public void getState() {
		appInstanceStatus = new OpenShiftAppInstanceStatus("testapp-source",
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
		appInstanceStatus = new OpenShiftAppInstanceStatus("testapp-source", null, null,
				new BuildBuilder().withNewStatus().withPhase("Running").endStatus()
						.build());

		DeploymentState state = appInstanceStatus.getState();

		assertThat(state).isEqualTo(DeploymentState.deploying);
	}

}
