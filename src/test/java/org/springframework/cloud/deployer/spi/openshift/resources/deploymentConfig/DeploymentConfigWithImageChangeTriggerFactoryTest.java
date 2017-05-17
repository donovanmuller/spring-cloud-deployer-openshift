package org.springframework.cloud.deployer.spi.openshift.resources.deploymentConfig;

import java.util.HashMap;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.client.server.mock.OpenShiftServer;
import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ImagePullPolicy;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class DeploymentConfigWithImageChangeTriggerFactoryTest {

	@Rule
	public OpenShiftServer server = new OpenShiftServer();

	private DeploymentConfigFactory deploymentConfigFactory;

	@Test
	public void buildDeploymentConfig() {
		deploymentConfigFactory = new DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory(
				server.getOpenshiftClient(), new OpenShiftDeployerProperties(), null, null, null,
				ImagePullPolicy.Always);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class));

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request, "testapp-source", new Container(),
				new HashMap<>(), null, ImagePullPolicy.Always);

		assertThat(deploymentConfig.getSpec().getTriggers()).has(new Condition<DeploymentTriggerPolicy>() {

			@Override
			public boolean matches(final DeploymentTriggerPolicy deploymentTriggerPolicy) {
				DeploymentTriggerImageChangeParams imageChangeParams = deploymentTriggerPolicy.getImageChangeParams();
				return imageChangeParams.getContainerNames().contains("testapp-source")
						&& imageChangeParams.getFrom().getName().equals("testapp-source:latest");
			}
		}, Index.atIndex(1));
	}

	@Test
	public void buildDeploymentConfigWithImageTag() {
		deploymentConfigFactory = new DeploymentConfigWithImageChangeTriggerWithIndexSuppportFactory(
				server.getOpenshiftClient(), new OpenShiftDeployerProperties(), null, null, null,
				ImagePullPolicy.Always);

		AppDeploymentRequest request = new AppDeploymentRequest(new AppDefinition("testapp-source", null),
				mock(Resource.class),
				ImmutableMap.of(OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG, "dev"));

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request, "testapp-source", new Container(),
				new HashMap<>(), null, ImagePullPolicy.Always);

		assertThat(deploymentConfig.getSpec().getTriggers()).has(new Condition<DeploymentTriggerPolicy>() {

			@Override
			public boolean matches(final DeploymentTriggerPolicy deploymentTriggerPolicy) {
				DeploymentTriggerImageChangeParams imageChangeParams = deploymentTriggerPolicy.getImageChangeParams();
				return imageChangeParams.getContainerNames().contains("testapp-source")
						&& imageChangeParams.getFrom().getName().equals("testapp-source:dev");
			}
		}, Index.atIndex(1));
	}

}
