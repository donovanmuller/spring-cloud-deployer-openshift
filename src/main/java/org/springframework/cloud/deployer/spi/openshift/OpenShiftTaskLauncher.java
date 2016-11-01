package org.springframework.cloud.deployer.spi.openshift;

import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.factories.ObjectFactory;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.OpenShiftClient;

public class OpenShiftTaskLauncher extends KubernetesTaskLauncher
		implements TaskLauncher {

	private KubernetesDeployerProperties properties;
	private OpenShiftClient client;

	public OpenShiftTaskLauncher(KubernetesDeployerProperties properties,
			KubernetesClient client) {
		super(properties, client);

		this.properties = properties;
		this.client = (OpenShiftClient) client;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		logger.info(String.format("Launching task: '%s'", request.getDefinition()));

		String taskId = createDeploymentId(request);

		List<ObjectFactory> factories = populateOpenShiftObjects(request, taskId);
		factories.forEach(factory -> factory.addObject(request, taskId));
		factories.forEach(factory -> factory.applyObject(request, taskId));

		return taskId;
	}

	/**
	 * Populate the OpenShift objects that will be created/updated and applied.
	 *
	 * @param request
	 * @param appId
	 * @return list of {@link ObjectFactory}'s
	 */
	protected List<ObjectFactory> populateOpenShiftObjects(AppDeploymentRequest request,
			String appId) {
		List<ObjectFactory> factories = new ArrayList<>();

		factories.add(new ObjectFactory() {

			@Override
			public Object addObject(AppDeploymentRequest request, String appId) {
				// don't need to create anything
				return null;
			}

			@Override
			public void applyObject(AppDeploymentRequest request, String appId) {
				AppDeploymentRequest taskDeploymentRequest = new AppDeploymentRequest(
						request.getDefinition(), request.getResource(),
						request.getDeploymentProperties(),
						request.getCommandlineArguments());

				new KubernetesTaskLauncher(properties, client)
						.launch(taskDeploymentRequest);
			}
		});

		return factories;
	}

	protected OpenShiftClient getClient() {
		return client;
	}

	protected KubernetesDeployerProperties getProperties() {
		return properties;
	}
}
