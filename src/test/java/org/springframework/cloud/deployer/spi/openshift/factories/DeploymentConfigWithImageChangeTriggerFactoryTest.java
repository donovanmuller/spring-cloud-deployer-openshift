package org.springframework.cloud.deployer.spi.openshift.factories;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.assertj.core.api.Condition;
import org.assertj.core.data.Index;
import org.junit.Test;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeploymentPropertyKeys;
import org.springframework.core.io.Resource;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.openshift.api.model.DeploymentConfig;
import io.fabric8.openshift.api.model.DeploymentTriggerImageChangeParams;
import io.fabric8.openshift.api.model.DeploymentTriggerPolicy;
import io.fabric8.openshift.client.mock.OpenShiftMockServerTestBase;

public class DeploymentConfigWithImageChangeTriggerFactoryTest
		extends OpenShiftMockServerTestBase {

	private DeploymentConfigFactory deploymentConfigFactory;

	@Test
	public void buildDeploymentConfig() {
		deploymentConfigFactory = new DeploymentConfigWithImageChangeTriggerFactory(
				getOpenshiftClient(), new OpenShiftDeployerProperties(), null, null,
				null);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class));

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request,
				"testapp-source", new Container(), null, null);

		assertThat(deploymentConfig.getSpec().getTriggers())
				.has(new Condition<DeploymentTriggerPolicy>() {

					@Override
					public boolean matches(
							final DeploymentTriggerPolicy deploymentTriggerPolicy) {
						DeploymentTriggerImageChangeParams imageChangeParams = deploymentTriggerPolicy
								.getImageChangeParams();
						return imageChangeParams.getContainerNames()
								.contains("testapp-source")
								&& imageChangeParams.getFrom().getName()
										.equals("testapp-source:latest");
					}
				}, Index.atIndex(1));
	}

	@Test
	public void buildDeploymentConfigWithImageTag() {
		deploymentConfigFactory = new DeploymentConfigWithImageChangeTriggerFactory(
				getOpenshiftClient(), new OpenShiftDeployerProperties(), null, null,
				null);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null), mock(Resource.class),
				ImmutableMap.of(
						OpenShiftDeploymentPropertyKeys.OPENSHIFT_DEPLOYMENT_IMAGE_TAG,
						"dev"));

		DeploymentConfig deploymentConfig = deploymentConfigFactory.build(request,
				"testapp-source", new Container(), null, null);

		assertThat(deploymentConfig.getSpec().getTriggers())
				.has(new Condition<DeploymentTriggerPolicy>() {

					@Override
					public boolean matches(
							final DeploymentTriggerPolicy deploymentTriggerPolicy) {
						DeploymentTriggerImageChangeParams imageChangeParams = deploymentTriggerPolicy
								.getImageChangeParams();
						return imageChangeParams.getContainerNames()
								.contains("testapp-source")
								&& imageChangeParams.getFrom().getName()
										.equals("testapp-source:dev");
					}
				}, Index.atIndex(1));
	}

}
