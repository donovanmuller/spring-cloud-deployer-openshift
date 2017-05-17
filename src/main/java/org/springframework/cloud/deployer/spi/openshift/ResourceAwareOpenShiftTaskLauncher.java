package org.springframework.cloud.deployer.spi.openshift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftTaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;

public class ResourceAwareOpenShiftTaskLauncher implements TaskLauncher {

	private static final Logger logger = LoggerFactory.getLogger(ResourceAwareOpenShiftTaskLauncher.class);

	private OpenShiftTaskLauncher openShiftTaskLauncher;
	private MavenOpenShiftTaskLauncher mavenOpenShiftTaskLauncher;

	public ResourceAwareOpenShiftTaskLauncher(OpenShiftTaskLauncher openShiftTaskLauncher,
			MavenOpenShiftTaskLauncher mavenOpenShiftTaskLauncher) {
		this.openShiftTaskLauncher = openShiftTaskLauncher;
		this.mavenOpenShiftTaskLauncher = mavenOpenShiftTaskLauncher;
	}

	@Override
	public String launch(AppDeploymentRequest request) {
		String taskId;

		try {
			if (request.getResource() instanceof MavenResource) {
				taskId = mavenOpenShiftTaskLauncher.launch(request);
			}
			else {
				taskId = openShiftTaskLauncher.launch(request);
			}
		}
		catch (Exception e) {
			logger.error(String.format("Error deploying application deployment request: %s", request), e);
			throw e;
		}

		return taskId;
	}

	@Override
	public void cancel(String taskId) {
		openShiftTaskLauncher.cancel(taskId);
	}

	@Override
	public TaskStatus status(String taskId) {
		return openShiftTaskLauncher.status(taskId);
	}

	@Override
	public void cleanup(final String taskId) {
		openShiftTaskLauncher.cancel(taskId);
	}

	@Override
	public void destroy(final String taskId) {
		openShiftTaskLauncher.destroy(taskId);
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return openShiftTaskLauncher.environmentInfo();
	}
}
