package org.springframework.cloud.deployer.spi.openshift;

import static org.springframework.cloud.deployer.spi.app.AppDeployer.COUNT_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.app.AppDeployer.INDEXED_PROPERTY_KEY;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

public interface DataflowSupport {

	default Integer getAppInstanceCount(AppDeploymentRequest request) {
		String countProperty = request.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
		return (countProperty != null) ? Integer.parseInt(countProperty) : 1;
	}

	default boolean isIndexed(AppDeploymentRequest request) {
		String indexedProperty = request.getDeploymentProperties()
			.get(INDEXED_PROPERTY_KEY);
		return  (indexedProperty != null)
			? Boolean.valueOf(indexedProperty) : false;
	}
}
