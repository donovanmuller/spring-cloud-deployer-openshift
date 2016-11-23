package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import static java.lang.String.format;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.util.Assert;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.api.model.BuildRequestBuilder;

public class MavenBuildConfigFactory extends BuildConfigFactory {

	public static String SPRING_BUILD_APP_GROUPID_ENV_VAR = "app_groupId";
	public static String SPRING_BUILD_APP_ARTIFACTID_ENV_VAR = "app_artifactId";
	public static String SPRING_BUILD_APP_VERSION_ENV_VAR = "app_version";
	public static String SPRING_BUILD_RESOURCE_HOST_ENV_VAR = "app_resource_host";
	public static String SPRING_BUILD_RESOURCE_URL_ENV_VAR = "app_resource_url";
	public static String SPRING_BUILD_AUTH_USERNAME_ENV_VAR = "repo_auth_username";
	public static String SPRING_BUILD_AUTH_PASSWORD_ENV_VAR = "repo_auth_password";

	private KubernetesDeployerProperties properties;
	private MavenProperties mavenProperties;
	private ResourceHash resourceHash;

	public MavenBuildConfigFactory(KubernetesDeployerProperties properties, ResourceHash resourceHash,
			MavenProperties mavenProperties) {
		this.properties = properties;
		this.mavenProperties = mavenProperties;
		this.resourceHash = resourceHash;
	}

	@Override
	protected BuildRequest buildBuildRequest(AppDeploymentRequest request, String appId) {
		MavenResource mavenResource = (MavenResource) request.getResource();
		MavenProperties.Authentication authentication = Optional
				.ofNullable(getFirstRemoteRepository(mavenProperties.getRemoteRepositories()).getAuth())
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
                .addToEnv(new EnvVar(SPRING_BUILD_APP_VERSION_ENV_VAR, mavenResource.getVersion(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_AUTH_USERNAME_ENV_VAR, authentication.getUsername(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_AUTH_PASSWORD_ENV_VAR, authentication.getPassword(), null))
                .addToEnv(new EnvVar(SPRING_BUILD_RESOURCE_HOST_ENV_VAR, toHost(mavenProperties.getRemoteRepositories()), null))
                .addToEnv(new EnvVar(SPRING_BUILD_RESOURCE_URL_ENV_VAR,
                        toRemoteUrl(mavenProperties.getRemoteRepositories(),
							(MavenResource) request.getResource()),
                        null))
            .build();
        //@formatter:on
	}

	/**
	 * Convert a remote repository URL and Maven coordinate into a valid URL. This URL will directly
	 * reference the Jar artifact to download. E.g. Remote repository URL
	 * [https://repo.spring.io/libs-snapshot-local] with Maven coordinate
	 * [org.springframework.cloud.stream.app:http-source-kafka:1.0.0.BUILD-SNAPSHOT] will be
	 * converted into the following URL:
	 * https://repo.spring.io/libs-snapshot-local/org/springframework/cloud/stream/app/log
	 * -sink-kafka/1.0.0.BUILD-SNAPSHOT/log-sink-kafka-1.0.0.BUILD-SNAPSHOT.jar
	 *
	 * @param remoteRepositories
	 * @param resource
	 * @return a valid URL referencing the Jar artifact on the remote repository
	 */
	private String toRemoteUrl(Map<String, MavenProperties.RemoteRepository> remoteRepositories,
			MavenResource resource) {
		String remoteRepository = getFirstRemoteRepository(remoteRepositories).getUrl();
		String artifactPath = format("%s/%s/%s/%s", resource.getGroupId().replaceAll("\\.", "/"),
				resource.getArtifactId(), resource.getVersion(), resource.getFilename());

		return format("%s/%s", remoteRepository, artifactPath);
	}

	private String toHost(Map<String, MavenProperties.RemoteRepository> remoteRepositories) {
		try {
			return new URL(getFirstRemoteRepository(remoteRepositories).getUrl()).getHost();
		}
		catch (MalformedURLException e) {
			throw new IllegalArgumentException("Remote Maven URL is invalid", e);
		}
	}

	/**
	 * Currently only support for a single remote repository. Or, more precisely, if there are
	 * multiple remote repositories, the first repository is chosen as the source of the Maven
	 * artifacts. I.e. what becomes the URL for the Jar file to download.
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
