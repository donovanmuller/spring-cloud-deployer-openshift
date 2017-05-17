package org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.Map;

import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeFactory;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.openshift.api.model.BuildBuilder;
import io.fabric8.openshift.api.model.BuildListBuilder;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentConfigBuilder;

public class DeploymentConfigFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private DeploymentConfigFactory deploymentConfigFactory;

	@Test
	public void buildDeploymentConfig() {
		deploymentConfigFactory = new DeploymentConfigFactory(server.getOpenshiftClient(), null, null, null,
				ImagePullPolicy.Always, new VolumeFactory(new OpenShiftDeployerProperties()));

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request, "testapp-source", new Container(),
				null, null, ImagePullPolicy.Always);

		assertThat(deploymentConfig.getMetadata().getName()).isEqualTo("testapp-source");
		assertThat(deploymentConfig.getSpec().getReplicas()).isEqualTo(1);
		assertThat(deploymentConfig.getSpec().getTemplate().getSpec().getServiceAccount()).isEmpty();
		assertThat(deploymentConfig.getSpec().getTemplate().getSpec().getNodeSelector()).isEmpty();
	}

	@Test
	public void buildDeploymentConfigWithServiceAccountAndNodeSelector() {
		deploymentConfigFactory = new DeploymentConfigFactory(server.getOpenshiftClient(), null, null, null,
				ImagePullPolicy.Always, new VolumeFactory(new OpenShiftDeployerProperties()));

		Map<String, String> deploymentProperties = ImmutableMap.of(
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_SERVICE_ACCOUNT, "test-sa",
				OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_NODE_SELECTOR, "region: test");
		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), deploymentProperties);

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request, "testapp-source", new Container(),
				null, null, ImagePullPolicy.Always);

		assertThat(deploymentConfig.getMetadata().getName()).isEqualTo("testapp-source");
		assertThat(deploymentConfig.getSpec().getTemplate().getSpec().getServiceAccount()).isEqualTo("test-sa");
		assertThat(deploymentConfig.getSpec().getTemplate().getSpec().getNodeSelector()).containsEntry("region",
				"test");
	}

	@Test
	public void applyDeploymentConfigWhenNoActiveBuilds() {
		server.expect().get().withPath("/oapi/v1/namespaces/test/builds").andReturn(200, new BuildListBuilder()
				.withItems(new BuildBuilder().withNewStatus().withPhase("Completed").endStatus().build()).build())
				.once();

		server.expect().withPath("/oapi/v1/namespaces/test/deploymentconfigs/testapp-source")
				.andReturn(200, new DeploymentConfigBuilder().withNewMetadata().endMetadata().build()).times(3);

		deploymentConfigFactory = new DeploymentConfigFactory(server.getOpenshiftClient(), null, Collections.EMPTY_MAP,
				null, ImagePullPolicy.Always, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), null);

		deploymentConfigFactory.applyObject(request, "testapp-source");
	}

	@Test
	public void applyDeploymentConfigWhenActiveBuilds() {
		server.expect().get().withPath("/oapi/v1/namespaces/test/builds")
				.andReturn(200, new BuildListBuilder()
						.withItems(new BuildBuilder().withNewStatus().withPhase("Running").endStatus().build()).build())
				.once();

		deploymentConfigFactory = new DeploymentConfigFactory(server.getOpenshiftClient(), null, Collections.EMPTY_MAP,
				null, ImagePullPolicy.Always, null);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class), null);

		deploymentConfigFactory.applyObject(request, "testapp-source");
	}

}
