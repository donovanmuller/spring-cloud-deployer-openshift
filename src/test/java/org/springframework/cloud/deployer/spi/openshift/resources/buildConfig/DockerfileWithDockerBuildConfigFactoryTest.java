package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.ResourceHash;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.BuildConfigFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.buildConfig.DockerfileWithDockerBuildConfigFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.client.mock.OpenShiftServer;

public class DockerfileWithDockerBuildConfigFactoryTest {

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
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null, null,
				new OpenShiftDeployerProperties(), null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getType()).isEqualTo("Dockerfile");
		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim()).startsWith("FROM java:8");
		assertThat(buildConfig.getSpec().getOutput().getTo().getName()).isEqualTo("testapp-source:latest");
	}

	@Test
	public void buildBuildConfigWithDockerfileFromFileSystem() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null, null,
				new OpenShiftDeployerProperties(), null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE,
						"src/test/resources/TestDockerfile"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim()).isEqualTo("FROM test from file system");
	}

	@Test
	public void buildBuildConfigWithInlineDockerfile() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null, null,
				new OpenShiftDeployerProperties(), null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_DOCKERFILE,
						"FROM an inline Dockerfile"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request, null, null, "1");

		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim()).isEqualTo("FROM an inline Dockerfile");
	}

	@Test
	public void buildBuildConfigWithImageTag() {
		buildConfigFactory = new DockerfileWithDockerBuildConfigFactory(server.getOpenshiftClient(), null, null, null,
				new OpenShiftDeployerProperties(), null, resourceHash) {
		};

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG, "dev"));

		BuildConfig buildConfig = buildConfigFactory.buildBuildConfig("testapp-source", request, null, null, "1");

		assertThat(buildConfig.getSpec().getOutput().getTo().getName()).isEqualTo("testapp-source:dev");
	}

}
