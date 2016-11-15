package org.springframework.cloud.deployer.spi.openshift.resources.imageStream;

import java.util.Optional;

import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import io.fabric8.openshift.api.model.ImageStream;
import io.fabric8.openshift.client.OpenShiftClient;
import org.springframework.cloud.deployer.spi.openshift.resources.AbstractObjectFactory;

public class ImageStreamFactory extends AbstractObjectFactory<ImageStream> {

	private OpenShiftClient client;

	public ImageStreamFactory(OpenShiftClient client) {
		this.client = client;
	}

	@Override
	protected ImageStream createObject(AppDeploymentRequest request, String appId) {
		//@formatter:off
        return client.imageStreams()
            .createNew()
                .withNewMetadata()
                    .withName(appId)
            .endMetadata()
            .done();
        //@formatter:on
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		// do nothing
	}

	@Override
	protected Optional<ImageStream> getExisting(String name) {
		//@formatter:off
        return Optional.ofNullable(client.imageStreams()
                .withName(name)
                .fromServer()
                .get());
		//@formatter:on
	}
}
