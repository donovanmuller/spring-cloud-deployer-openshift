package org.springframework.cloud.deployer.spi.openshift.factories;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

public interface ObjectFactory<T> {

	T addObject(AppDeploymentRequest request, String appId);

	void applyObject(AppDeploymentRequest request, String appId);
}
