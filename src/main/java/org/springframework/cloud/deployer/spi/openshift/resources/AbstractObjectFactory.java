package org.springframework.cloud.deployer.spi.openshift.resources;

import java.util.Optional;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

public abstract class AbstractObjectFactory<T> implements ObjectFactory {

	@Override
	public T addObject(AppDeploymentRequest request, String appId) {
		return getExisting(appId).orElseGet(() -> createObject(request, appId));
	}

	protected abstract T createObject(AppDeploymentRequest request, String appId);

	protected abstract Optional<T> getExisting(String name);
}
