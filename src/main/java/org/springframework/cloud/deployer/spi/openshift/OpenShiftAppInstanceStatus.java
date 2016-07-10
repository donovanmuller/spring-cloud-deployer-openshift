package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.cloud.deployer.spi.app.DeploymentState;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAppInstanceStatus;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Build;

public class OpenShiftAppInstanceStatus extends KubernetesAppInstanceStatus {

	private Build build;

	public OpenShiftAppInstanceStatus(String appId, Pod pod,
			KubernetesDeployerProperties properties, Build build) {
		super(appId, pod, properties);
		this.build = build;
	}

	/**
	 * Active Builds are considered a {@link DeploymentState} of "deploying"
	 *
	 * @return the state of this application instance deployed in OpenShift
	 */
	@Override
	public DeploymentState getState() {
		DeploymentState state;

		if (build != null && (build.getStatus().getPhase().equals("New")
				|| build.getStatus().getPhase().equals("Pending")
				|| build.getStatus().getPhase().equals("Running"))) {
			state = DeploymentState.deploying;
		}
		else {
			state = super.getState();
		}

		return state;
	}
}
