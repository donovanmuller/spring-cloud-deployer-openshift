package org.springframework.cloud.deployer.spi.openshift.resources.pod;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.DefaultContainerFactory;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.core.io.AbstractResource;

import io.fabric8.kubernetes.api.model.Container;

public class OpenShiftContainerFactory extends DefaultContainerFactory
		implements OpenShiftSupport {

	private OpenShiftDeployerProperties properties;
	private VolumeMountFactory volumeMountFactory;

	public OpenShiftContainerFactory(OpenShiftDeployerProperties properties,
			VolumeMountFactory volumeMountFactory) {
		super(properties);
		this.properties = properties;
		this.volumeMountFactory = volumeMountFactory;
	}

	@Override
	public Container create(String appId, AppDeploymentRequest request, Integer port,
			Integer instanceIndex, boolean hostNetwork) {
		Container container;

		if (request.getResource() instanceof DockerResource) {
			try {
				container = super.create(appId, new AppDeploymentRequest(
						request.getDefinition(),
						new OverridableDockerResource(request.getResource().getURI(),
								properties.getDockerRegistryOverride(),
								properties.getImageProjectName()),
						request.getDeploymentProperties(),
						request.getCommandlineArguments()), port, null, false);
			}
			catch (IOException e) {
				throw new IllegalArgumentException(
						"Unable to get URI for " + request.getResource(), e);
			}
		}
		else {
			container = super.create(appId,
					new AppDeploymentRequest(request.getDefinition(),
							new AppIdResource(appId), request.getDeploymentProperties(),
							request.getCommandlineArguments()),
					port, null, false);
		}

		// use the VolumeMountFactory to resolve VolumeMounts because it has richer
		// support for things like using a Spring Cloud config server to resolve
		// VolumeMounts
		container.setVolumeMounts(volumeMountFactory.addObject(request, appId));

		return container;
	}

	/**
	 * This allows the Kubernetes {@link DefaultContainerFactory} to be reused but still
	 * supporting BuildConfig used by the
	 * {@link org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer}.
	 * This resource allows the <code>appId</code> to be set as the Pod image, which is
	 * required for OpenShift to use the built image from the ImageStream after a
	 * successful Build.
	 */
	private class AppIdResource extends AbstractResource {

		private final String appId;

		AppIdResource(String appId) {
			this.appId = appId;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public URI getURI() throws IOException {
			try {
				return new URIBuilder("docker:" + appId).build();
			}
			catch (URISyntaxException e) {
				throw new IllegalStateException(
						"Could not create masked URI for Maven build", e);
			}
		}
	}

	/**
	 * This allows the Kubernetes {@link DefaultContainerFactory} to be reused but still
	 * supporting BuildConfig used by the
	 * {@link org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer}.
	 * This resource allows the <code>appId</code> to be set as the Pod image, which is
	 * required for OpenShift to use the built image from the ImageStream after a
	 * successful Build.
	 */
	private class OverridableDockerResource extends DockerResource {

		private final Logger log = LoggerFactory
				.getLogger(OverridableDockerResource.class);

		private final String dockerRegistry;
		private final String imageProjectName;

		public OverridableDockerResource(String imageName, String dockerRegistry,
				String imageProjectName) {
			super(imageName);
			this.dockerRegistry = dockerRegistry;
			this.imageProjectName = imageProjectName;
		}

		public OverridableDockerResource(URI uri, String dockerRegistry,
				String imageProjectName) {
			super(uri);
			this.dockerRegistry = dockerRegistry;
			this.imageProjectName = imageProjectName;
		}

		@Override
		public String getDescription() {
			return null;
		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public URI getURI() throws IOException {
			try {
				if (StringUtils.isNotBlank(dockerRegistry)) {
					log.debug("Overriding docker registry with '{}' and project '{}'",
							dockerRegistry, imageProjectName);
					return new URIBuilder(super.getURI().toString()
							.replaceFirst("docker:(.*?)/",
									String.format("docker:%s/", dockerRegistry))
							.replaceFirst("/(.*?)/",
									String.format("/%s/", imageProjectName))).build();
				}
				else {
					return super.getURI();
				}
			}
			catch (URISyntaxException e) {
				throw new IllegalStateException(
						"Could not create masked URI for Maven build", e);
			}
		}
	}
}
