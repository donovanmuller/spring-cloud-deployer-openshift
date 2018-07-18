package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import java.util.function.BiConsumer;

import static org.springframework.cloud.deployer.spi.app.AppDeployer.COUNT_PROPERTY_KEY;
import static org.springframework.cloud.deployer.spi.app.AppDeployer.INDEXED_PROPERTY_KEY;

public interface DataflowSupport {

	default Integer getAppInstanceCount(AppDeploymentRequest request) {
		String countProperty = request.getDeploymentProperties().get(COUNT_PROPERTY_KEY);
		return (countProperty != null) ? Integer.parseInt(countProperty) : 1;
	}

	default boolean isIndexed(AppDeploymentRequest request) {
		String indexedProperty = request.getDeploymentProperties()
				.get(INDEXED_PROPERTY_KEY);
		return (indexedProperty != null) ? Boolean.valueOf(indexedProperty) : false;
	}

	default void withIndexedDeployment(String appId, AppDeploymentRequest request,
			BiConsumer<String, AppDeploymentRequest> consumer) {
		if (isIndexed(request)) {
			Integer count = getAppInstanceCount(request);
			for (int index = 0; index < count; index++) {
				String indexedId = appId + "-" + index;
				consumer.accept(indexedId, request);
			}
		}
		else {
			consumer.accept(appId, request);
		}
	}

}
