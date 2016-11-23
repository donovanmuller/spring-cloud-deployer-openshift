package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import java.util.Map;

import io.fabric8.openshift.api.model.BuildStrategy;
import io.fabric8.openshift.client.DefaultOpenShiftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;

import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.openshift.api.model.Build;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.OpenShiftClient;

/**
 * Provides the ability to watch an object. Whilst watching this object, the events for that object
 * are passed to a callback method. It is the responsibility of the callback implementer to call the
 * <code>watch.close()</code>
 */
public class WatchingBuildConfigStrategy extends BuildConfigStrategy {

	private static Logger logger = LoggerFactory.getLogger(WatchingBuildConfigStrategy.class);

	/**
	 * See
	 * {@link org.springframework.cloud.deployer.spi.kubernetes.AbstractKubernetesDeployer#SPRING_APP_KEY}
	 */
	private static String SPRING_APP_KEY = "spring-app-id";

	private OpenShiftClient client;
	private BuildConfigStrategy buildConfigStrategy;
	private OnCompletedCallback<Build> callback;

	private Watch watch;

	public WatchingBuildConfigStrategy(BuildConfigStrategy buildConfigStrategy, OpenShiftClient client,
									   Map<String, String> labels, OnCompletedCallback<Build> callback) {
		super(null, new DefaultOpenShiftClient().inNamespace(client.getNamespace()), labels);
		this.buildConfigStrategy = buildConfigStrategy;
		this.client = new DefaultOpenShiftClient().inNamespace(client.getNamespace());
		this.callback = callback;
	}

	@Override
	public BuildConfig addObject(AppDeploymentRequest request, String appId) {
		return super.addObject(request, appId);
	}

	@Override
	protected BuildConfig buildBuildConfig(AppDeploymentRequest request, String appId,
			Map<String, String> labels) {
		return buildConfigStrategy.buildBuildConfig(request, appId, labels);
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		buildConfigStrategy.applyObject(request, appId);
		
		watch = client.builds().withLabelIn(SPRING_APP_KEY, appId).watch(new Watcher<Build>() {

			@Override
			public void eventReceived(Action action, Build resource) {
				logger.trace("Received event '{}' for build: '{}'", action, resource);

				callback.callback(resource, watch);
			}

			@Override
			public void onClose(KubernetesClientException cause) {
				logger.trace(String.format("Closing watcher for build: '%s'", appId));
			}
		});
	}

	@FunctionalInterface
	public interface OnCompletedCallback<R extends Build> {

		void callback(R r, Watch watch);
	}
}
