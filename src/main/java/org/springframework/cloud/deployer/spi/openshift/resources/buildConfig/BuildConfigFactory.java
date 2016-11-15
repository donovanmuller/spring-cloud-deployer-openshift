package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import static java.lang.String.format;

import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftSupport;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.resources.ObjectFactory;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.util.Assert;

import com.google.common.collect.ImmutableList;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.api.model.BuildRequestBuilder;
import io.fabric8.openshift.api.model.BuildTriggerPolicyBuilder;
import io.fabric8.openshift.client.OpenShiftClient;

public abstract class BuildConfigFactory
		implements ObjectFactory<BuildConfig>, OpenShiftSupport {

	public static String SPRING_BUILD_ID_ENV_VAR = "spring_build_id";
	public static String SPRING_BUILD_APP_NAME_ENV_VAR = "app_name";
	public static String SPRING_BUILD_APP_GROUPID_ENV_VAR = "app_groupId";
	public static String SPRING_BUILD_APP_ARTIFACTID_ENV_VAR = "app_artifactId";
	public static String SPRING_BUILD_APP_VERSION_ENV_VAR = "app_version";
	public static String SPRING_BUILD_RESOURCE_URL_ENV_VAR = "app_resource_url";
	public static String SPRING_BUILD_AUTH_USERNAME_ENV_VAR = "repo_auth_username";
	public static String SPRING_BUILD_AUTH_PASSWORD_ENV_VAR = "repo_auth_password";

	private OpenShiftClient client;
	private Map<String, String> labels;
	private GitReference gitReference;
	private KubernetesDeployerProperties properties;
	private OpenShiftDeployerProperties openShiftDeployerProperties;
	private MavenProperties mavenProperties;
	private ResourceHash resourceHash;

	public BuildConfigFactory(OpenShiftClient client, Map<String, String> labels,
			GitReference gitReference, KubernetesDeployerProperties properties,
			OpenShiftDeployerProperties openShiftDeployerProperties,
			MavenProperties mavenProperties, ResourceHash resourceHash) {
		this.client = client;
		this.labels = labels;
		this.gitReference = gitReference;
		this.properties = properties;
		this.openShiftDeployerProperties = openShiftDeployerProperties;
		this.mavenProperties = mavenProperties;
		this.resourceHash = resourceHash;
	}

	@Override
	public BuildConfig addObject(AppDeploymentRequest request, String appId) {
		BuildConfig buildConfig = buildBuildConfig(appId, request, gitReference, labels,
				resourceHash.hashResource(request.getResource()));

		if (getExisting(appId).isPresent()) {
			/**
			 * patching an existing BuildConfig does not seem to work on OpenShift 1.3. An
			 * error around trying to remove a non existent key, "items", is thrown. As a
			 * workaround, we delete the BuildConfig and all associated Builds (otherwise
			 * the Build names don't increment automatically when applied)
			 */
			// buildConfig = client.buildConfigs().patch(buildConfig);
			client.buildConfigs().withName(appId).delete();
			client.builds().withLabelIn("spring-app-id", appId).delete();
			buildConfig = client.buildConfigs().create(buildConfig);
		}
		else {
			buildConfig = client.buildConfigs().create(buildConfig);
		}

		return buildConfig;
	}

	@Override
	public void applyObject(AppDeploymentRequest request, String appId) {
		client.buildConfigs().withName(appId)
				.instantiate(buildBuildRequest(request, appId));
	}

	protected Optional<BuildConfig> getExisting(String name) {
		//@formatter:off
		BuildConfig value = client.buildConfigs()
                .withName(name)
                .fromServer()
                .get();

		return Optional.ofNullable(value);
		//@formatter:on
	}

	protected BuildConfig buildBuildConfig(String appId, AppDeploymentRequest request,
			GitReference gitReference, Map<String, String> labels, String hash) {
		//@formatter:off
        return new BuildConfigBuilder()
            .withNewMetadata()
                .withName(appId)
                .withLabels(labels)
            .endMetadata()
            .withNewSpec()
                .withTriggers(ImmutableList.of(
                        new BuildTriggerPolicyBuilder()
                            .withNewImageChange()
                            .endImageChange()
                            .build()
                ))
            .endSpec()
            .build();
        //@formatter:on
	}

	protected BuildRequest buildBuildRequest(AppDeploymentRequest request, String appId) {
		MavenResource mavenResource = (MavenResource) request.getResource();
		MavenProperties.Authentication authentication = Optional.ofNullable(
				getFirstRemoteRepository(mavenProperties.getRemoteRepositories())
						.getAuth())
				.orElse(new MavenProperties.Authentication());

		//@formatter:off
		return new BuildRequestBuilder()
            .withNewMetadata()
                .withName(appId)
			.endMetadata()
            .withEnv(toEnvVars(properties.getEnvironmentVariables()))
                .addToEnv(new EnvVar(SPRING_BUILD_ID_ENV_VAR, resourceHash.hashResource(request.getResource()), null))
                .addToEnv(new EnvVar(SPRING_BUILD_APP_NAME_ENV_VAR , appId, null))
                .addToEnv(new EnvVar(SPRING_BUILD_APP_GROUPID_ENV_VAR , mavenResource.getGroupId(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_APP_ARTIFACTID_ENV_VAR, mavenResource.getArtifactId(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_APP_VERSION_ENV_VAR , mavenResource.getVersion(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_AUTH_USERNAME_ENV_VAR , authentication.getUsername(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_AUTH_PASSWORD_ENV_VAR , authentication.getPassword(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_RESOURCE_URL_ENV_VAR,
                        toRemoteUrl(mavenProperties.getRemoteRepositories(),
							(MavenResource) request.getResource()),
                        null))
            .build();
        //@formatter:on
	}

	/**
	 * Convert a remote repository URL and Maven coordinate into a valid URL. This URL
	 * will directly reference the Jar artifact to download. E.g. Remote repository URL
	 * [https://repo.spring.io/libs-snapshot-local] with Maven coordinate
	 * [org.springframework.cloud.stream.app:http-source-kafka:1.0.0.BUILD-SNAPSHOT] will
	 * be converted into the following URL:
	 * https://repo.spring.io/libs-snapshot-local/org/springframework/cloud/stream/app/log
	 * -sink-kafka/1.0.0.BUILD-SNAPSHOT/log-sink-kafka-1.0.0.BUILD-SNAPSHOT.jar
	 *
	 * @param remoteRepositories
	 * @param resource
	 * @return a valid URL referencing the Jar artifact on the remote repository
	 */
	private String toRemoteUrl(
			Map<String, MavenProperties.RemoteRepository> remoteRepositories,
			MavenResource resource) {
		String remoteRepository = getFirstRemoteRepository(remoteRepositories).getUrl();
		String artifactPath = format("%s/%s/%s/%s",
				resource.getGroupId().replaceAll("\\.", "/"), resource.getArtifactId(),
				resource.getVersion(), resource.getFilename());

		return format("%s/%s", remoteRepository, artifactPath);
	}

	/**
	 * Currently only support for a single remote repository. Or, more precisely, if there
	 * are multiple remote repositories, the first repository is chosen as the source of
	 * the Maven artifacts. I.e. what becomes the URL for the Jar file to download.
	 *
	 * @param remoteRepositories
	 * @return the first remote repository
	 */
	private MavenProperties.RemoteRepository getFirstRemoteRepository(
			Map<String, MavenProperties.RemoteRepository> remoteRepositories) {
		Assert.notEmpty(remoteRepositories, "No remote repository specified");
		return new TreeMap<>(remoteRepositories).firstEntry().getValue();
	}
}
