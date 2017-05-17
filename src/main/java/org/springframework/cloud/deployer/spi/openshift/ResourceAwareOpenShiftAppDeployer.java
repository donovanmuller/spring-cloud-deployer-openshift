package org.springframework.cloud.deployer.spi.openshift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.app.AppStatus;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.core.RuntimeEnvironmentInfo;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer;

public class ResourceAwareOpenShiftAppDeployer implements AppDeployer {

	private static final Logger logger = LoggerFactory
			.getLogger(ResourceAwareOpenShiftAppDeployer.class);

	private OpenShiftAppDeployer openShiftAppDeployer;
	private MavenOpenShiftAppDeployer mavenOpenShiftAppDeployer;

	public ResourceAwareOpenShiftAppDeployer(OpenShiftAppDeployer openShiftAppDeployer,
			MavenOpenShiftAppDeployer mavenOpenShiftAppDeployer) {
		this.openShiftAppDeployer = openShiftAppDeployer;
		this.mavenOpenShiftAppDeployer = mavenOpenShiftAppDeployer;
	}

	@Override
	public String deploy(AppDeploymentRequest request) {
		String appId;

		try {
			if (request.getResource() instanceof MavenResource) {
				appId = mavenOpenShiftAppDeployer.deploy(request);
			}
			else {
				appId = openShiftAppDeployer.deploy(request);
			}
		}
		catch (Exception e) {
			logger.error(String.format(
					"Error deploying application deployment request: %s", request), e);
			throw e;
		}

		return appId;
	}

	@Override
	public void undeploy(String appId) {
		openShiftAppDeployer.undeploy(appId);
	}

	@Override
	public AppStatus status(String appId) {
		return openShiftAppDeployer.status(appId);
	}

	@Override
	public RuntimeEnvironmentInfo environmentInfo() {
		return openShiftAppDeployer.environmentInfo();
	}
}
