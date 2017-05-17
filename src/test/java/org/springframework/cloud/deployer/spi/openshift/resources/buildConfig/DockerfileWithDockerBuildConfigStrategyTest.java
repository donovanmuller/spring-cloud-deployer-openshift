package org.springframework.cloud.deployer.spi.openshift.resources.buildConfig;

import com.google.common.collect.ImmutableMap;
import io.fabric8.openshift.api.model.BuildConfig;
import io.fabric8.openshift.api.model.BuildConfigBuilder;
import io.fabric8.openshift.api.model.BuildRequest;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DockerfileWithDockerBuildConfigStrategyTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private BuildConfigStrategy buildConfigStrategy;

	@Before
	public void setup() {
		server.expect().post().withPath("/oapi/v1/namespaces/test/buildconfigs")
				.andReturn(201, new BuildConfigBuilder().withNewSpec().endSpec().build()).once();
		buildConfigStrategy = new MavenDockerfileWithDockerBuildConfigStrategy(new BuildConfigFactory() {

			@Override
			protected BuildRequest buildBuildRequest(AppDeploymentRequest request, String appId) {
				return null;
			}
		}, new OpenShiftDeployerProperties(), server.getOpenshiftClient(), null) {

			@Override
			protected String getDockerfile(AppDeploymentRequest request, OpenShiftDeployerProperties properties) {
				return "FROM java:8";
			}
		};
	}

	@Test
	public void buildBuildConfig() {
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		BuildConfig buildConfig = buildConfigStrategy.buildBuildConfig(request, "testapp-source", null);

		assertThat(buildConfig.getSpec().getSource().getType()).isEqualTo("Dockerfile");
		assertThat(buildConfig.getSpec().getStrategy().getType()).isEqualTo("Docker");
		assertThat(buildConfig.getSpec().getSource().getDockerfile().trim()).startsWith("FROM java:8");
		assertThat(buildConfig.getSpec().getOutput().getTo().getName()).isEqualTo("testapp-source:latest");
	}

	@Test
	public void buildBuildConfigWithImageTag() {
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG, "dev"));

		BuildConfig buildConfig = buildConfigStrategy.buildBuildConfig(request, "testapp-source", null);

		assertThat(buildConfig.getSpec().getOutput().getTo().getName()).isEqualTo("testapp-source:dev");
	}
}
