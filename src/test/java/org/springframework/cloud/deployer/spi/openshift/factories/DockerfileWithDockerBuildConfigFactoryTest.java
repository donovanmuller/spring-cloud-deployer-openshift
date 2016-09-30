package org.springframework.cloud.deployer.spi.openshift.factories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.mock.OpenShiftMockServerTestBase;

public class DockerfileWithDockerBuildConfigFactoryTest
		extends OpenShiftMockServerTestBase {

	private ResourceHash resourceHash;

	private BuildConfigFactory buildConfigFactory;

	@Before
	public void setup() {
		resourceHash = mock(ResourceHash.class);
	}

	@Test
	public void buildBuildConfig() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(
				getOpenshiftClient(), null, null, null, null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source",
				request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getType()).isEqualTo("Dockerfile");
		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim())
				.startsWith("FROM java:8");
		assertThat(buildConfig.getSpec().getOutput().getTo().getName())
				.isEqualTo("testapp-source:latest");
	}

	@Test
	public void buildBuildConfigWithDockerfileFromFileSystem() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(
				getOpenshiftClient(), null, null, null, null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class),
				ImmutableMap.of(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE,
						"src/test/resources/TestDockerfile"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source",
				request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim())
				.isEqualTo("FROM test from file system");
	}

	@Test
	public void buildBuildConfigWithInlineDockerfile() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(
				getOpenshiftClient(), null, null, null, null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class),
				ImmutableMap.of(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE,
						"FROM an inline Dockerfile"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source",
				request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim())
				.isEqualTo("FROM an inline Dockerfile");
	}

	@Test
	public void buildBuildConfigWithImageTag() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(
				getOpenshiftClient(), null, null, null, null, null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class),
				ImmutableMap.of(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG,
						"dev"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source",
				request, null, null, "1");

		assertThat(buildConfig.getSpec().getOutput().getTo().getName())
				.isEqualTo("testapp-source:dev");
	}

}
