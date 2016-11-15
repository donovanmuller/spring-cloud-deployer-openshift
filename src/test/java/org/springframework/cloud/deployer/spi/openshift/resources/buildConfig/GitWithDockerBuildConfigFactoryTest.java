package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftApplicationPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.GitWithDockerBuildConfigFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.mock.OpenShiftServer;

public class GitWithDockerBuildConfigFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private ResourceHash resourceHash;

	private BuildConfigFactory buildConfigFactory;

	@Before
	public void setup() {
		resourceHash = mock(ResourceHash.class);
	}

	@Test
	public void buildBuildConfig() {
		buildConfigFactory = new GitWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null,
				new KubernetesDeployerProperties(), null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request,
				new GitReference("https://github.com/spring-cloud/spring-cloud-deployer.git", "v1.0.1.RELEASE"), null,
				"1");

		assertThat(buildConfig.getSpec().getSource().getType()).isEqualTo("Git");
		assertThat(buildConfig.getSpec().getSource().getGit().getUri())
				.isEqualTo("https://github.com/spring-cloud/spring-cloud-deployer.git");
		assertThat(buildConfig.getSpec().getSource().getGit().getRef()).isEqualTo("v1.0.1.RELEASE");
		assertThat(buildConfig.getSpec().getSource().getContextDir()).isEqualTo("src/main/docker");
		assertThat(buildConfig.getSpec().getOutput().getTo().getName()).isEqualTo("testapp-source:latest");
	}

	@Test
	public void buildBuildConfigWithContextDirectory() {
		buildConfigFactory = new GitWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null,
				new KubernetesDeployerProperties(), null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", ImmutableMap
						.of(OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH, "docker")),
				mock(Resource.class), null);

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request,
				new GitReference("https://github.com/spring-cloud/spring-cloud-deployer.git", "v1.0.1.RELEASE"), null,
				"1");

		assertThat(buildConfig.getSpec().getSource().getContextDir()).isEqualTo("docker");
	}

	@Test
	public void buildBuildConfigWithSourceSecretFromDeploymentProperty() {
		buildConfigFactory = new GitWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null,
				new KubernetesDeployerProperties(), null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", ImmutableMap.of(
						OpenShiftApplicationPropertyKeys.OPENSHIFT_BUILD_GIT_SOURCE_SECRET, "shh, its a secret")),
				mock(Resource.class), null);

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request,
				new GitReference("https://github.com/spring-cloud/spring-cloud-deployer.git", "v1.0.1.RELEASE"), null,
				"1");

		assertThat(buildConfig.getSpec().getSource().getSourceSecret().getName()).isEqualTo("shh, its a secret");
	}

	@Test
	public void buildBuildConfigWithSourceSecretFromEnvironmentProperty() {
		KubernetesDeployerProperties properties = new KubernetesDeployerProperties();
		properties.setEnvironmentVariables(
				new String[] { "spring.cloud.deployer.openshift.build.git.secret=shh, its a secret" });
		buildConfigFactory = new GitWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null, properties,
				null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request,
				new GitReference("https://github.com/spring-cloud/spring-cloud-deployer.git", "v1.0.1.RELEASE"), null,
				"1");

		assertThat(buildConfig.getSpec().getSource().getSourceSecret().getName()).isEqualTo("shh, its a secret");
	}

}
