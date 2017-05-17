package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MavenBuildConfigFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private ResourceHash resourceHash;

	private BuildConfigFactory buildConfigFactory;

	@Before
	public void setup() {
		resourceHash = mock(ResourceHash.class);
	}

	@Test
	public void buildBuildRequestWithoutAuthenticatedRemoteRepository() {
		when(resourceHash.hashResource(any())).thenReturn("1");

		MavenProperties mavenProperties = new MavenProperties();
		mavenProperties.getRemoteRepositories().put("repo1",
				new MavenProperties.RemoteRepository("http://repo1/public"));
		KubernetesDeployerProperties properties = new KubernetesDeployerProperties();

		buildConfigFactory = new MavenBuildConfigFactory(properties, resourceHash, mavenProperties) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				MavenResource.parse("org.test:testapp-source:1.0-SNAPSHOT", mavenProperties));

		BuildRequest buildRequest = buildConfigFactory.buildBuildRequest(request, "testapp-source");

		assertThat(buildRequest.getMetadata().getName()).isEqualTo("testapp-source");
		assertThat(buildRequest.getEnv())
				.contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_ID_ENV_VAR, "1", null));
		assertThat(buildRequest.getEnv())
				.contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_APP_NAME_ENV_VAR, "testapp-source", null));
		assertThat(buildRequest.getEnv()).contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_RESOURCE_URL_ENV_VAR,
				"http://repo1/public/org/test/testapp-source/1.0-SNAPSHOT/testapp-source-1.0-SNAPSHOT.jar", null));
	}

	@Test
	public void buildBuildRequestWithAuthenticatedRemoteRepository() {
		when(resourceHash.hashResource(any())).thenReturn("1");

		MavenProperties mavenProperties = new MavenProperties();
		MavenProperties.Authentication authentication = new MavenProperties.Authentication();
		authentication.setUsername("admin");
		authentication.setPassword("admin");
		mavenProperties.getRemoteRepositories().put("repo1",
				new MavenProperties.RemoteRepository("http://repo1/public", authentication));
		KubernetesDeployerProperties properties = new KubernetesDeployerProperties();

		buildConfigFactory = new MavenBuildConfigFactory(properties, resourceHash, mavenProperties) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				MavenResource.parse("org.test:testapp-source:1.0-SNAPSHOT", mavenProperties));

		BuildRequest buildRequest = buildConfigFactory.buildBuildRequest(request, "testapp-source");

		assertThat(buildRequest.getEnv())
				.contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_AUTH_USERNAME_ENV_VAR, "admin", null));
		assertThat(buildRequest.getEnv())
				.contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_AUTH_PASSWORD_ENV_VAR, "admin", null));
	}

	@Test
	public void buildBuildRequestWithNexusRemoteRepository() {
		when(resourceHash.hashResource(any())).thenReturn("1");

		MavenProperties mavenProperties = new MavenProperties();
		mavenProperties.getRemoteRepositories().put("repo1",
				new MavenProperties.RemoteRepository("http://nexusRepo1/public"));
		KubernetesDeployerProperties properties = new KubernetesDeployerProperties();

		buildConfigFactory = new MavenBuildConfigFactory(properties, resourceHash, mavenProperties) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				MavenResource.parse("org.test:testapp-source:1.0-SNAPSHOT", mavenProperties));

		BuildRequest buildRequest = buildConfigFactory.buildBuildRequest(request, "testapp-source");

		assertThat(buildRequest.getEnv())
				.contains(new EnvVar(MavenBuildConfigFactory.SPRING_BUILD_RESOURCE_HOST_ENV_VAR, "nexusRepo1", null));
	}
}
