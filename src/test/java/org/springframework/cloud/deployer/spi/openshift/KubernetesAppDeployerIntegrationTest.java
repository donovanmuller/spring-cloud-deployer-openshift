/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.deployer.spi.openshift;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.deployed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.unknown;
import static org.springframework.cloud.deployer.spi.test.EventuallyMatcher.eventually;

import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.test.AbstractAppDeployerIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Copied and adapted
 * <a href="https://github.com/spring-cloud/spring-cloud-deployer-kubernetes">from
 * spring-cloud-deployer-kubernetes</a> to test the <code>docker:</code> resource handling.
 */
@SpringBootTest(classes = { KubernetesAutoConfiguration.class, OpenShiftAutoConfiguration.class,
		OpenShiftAppDeployerMavenIntegrationTest.Config.class })
public class KubernetesAppDeployerIntegrationTest extends AbstractAppDeployerIntegrationTests {

	@ClassRule
	public static OpenShiftTestSupport openShiftAvailable = new OpenShiftTestSupport();

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	KubernetesClient kubernetesClient;

	@Autowired
	ContainerFactory containerFactory;

	@Autowired
	KubernetesDeployerProperties kubernetesDeployerProperties;

	@Override
	protected AppDeployer appDeployer() {
		return appDeployer;
	}

	@Test
	public void testGoodDeploymentWithLoadBalancer() {
		log.info("Testing {}...", "GoodDeploymentWithLoadBalancer");
		kubernetesDeployerProperties.setCreateLoadBalancer(true);
		kubernetesDeployerProperties.setMinutesToWaitForLoadBalancer(1);

		AppDefinition definition = new AppDefinition(randomName(), null);
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);

		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = appDeployer().deploy(request);
		Timeout timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(deployed))),
				timeout.maxAttempts, timeout.pause));

		log.info("Undeploying {}...", deploymentId);
		timeout = undeploymentTimeout();
		appDeployer().undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	@Override
	public void testUnknownDeployment() {
		log.info("Testing {}...", "UnknownDeployment");
		super.testUnknownDeployment();
	}

	@Override
	public void testSimpleDeployment() {
		log.info("Testing {}...", "SimpleDeployment");
		super.testSimpleDeployment();
	}

	@Override
	public void testRedeploy() {
		log.info("Testing {}...", "Redeploy");
		super.testRedeploy();
	}

	@Override
	public void testDeployingStateCalculationAndCancel() {
		log.info("Testing {}...", "DeployingStateCalculationAndCancel");
		super.testDeployingStateCalculationAndCancel();
	}

	@Override
	public void testFailedDeployment() {
		log.info("Testing {}...", "FailedDeployment");
		super.testFailedDeployment();
	}

	@Override
	protected String randomName() {
		// Kubernetes app names must start with a letter and can only be 24 characters
		// long
		return "app-" + super.randomName().substring(0, 18);
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(36, 5000);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource("springcloud/spring-cloud-deployer-spi-test-app:latest");
	}
}
