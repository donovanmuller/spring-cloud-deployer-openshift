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
import static org.springframework.cloud.deployer.spi.task.LaunchState.complete;
import static org.springframework.cloud.deployer.spi.test.EventuallyMatcher.eventually;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.task.TaskStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * Copied <a href="https://github.com/spring-cloud/spring-cloud-deployer-kubernetes">from
 * spring-cloud-deployer-kubernetes</a> to test the <code>docker:</code> resource
 * handling.
 */
@SpringApplicationConfiguration(classes = { KubernetesAutoConfiguration.class,
		OpenShiftAutoConfiguration.class,
		KubernetesTaskLauncherIntegrationTest.Config.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class KubernetesTaskLauncherIntegrationTest {

	protected static final Logger logger = LoggerFactory
			.getLogger(KubernetesTaskLauncherIntegrationTest.class);

	@ClassRule
	public static OpenShiftTestSupport openShiftTestSupport = new OpenShiftTestSupport();

	@Autowired
	TaskLauncher taskLauncher;

	@Autowired
	KubernetesClient kubernetesClient;

	@Autowired
	ContainerFactory containerFactory;

	@Test
	public void testSimpleLaunch() {
		logger.info("Testing {}...", "SimpleLaunch");
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		properties.put("exitCode", "0");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		logger.info("Launching {}...", request.getDefinition().getName());
		String deploymentId = taskLauncher.launch(request);
		logger.info("Launched {} ", deploymentId);

		Timeout timeout = launchTimeout();
		assertThat(deploymentId,
				eventually(hasStatusThat(Matchers.hasProperty("state", is(complete))),
						timeout.maxAttempts, timeout.pause));
	}

	@Test
	public void testReLaunch() {
		logger.info("Testing {}...", "ReLaunch");
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		properties.put("exitCode", "0");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);
		logger.info("Launching {}...", request.getDefinition().getName());
		String deploymentId = taskLauncher.launch(request);
		logger.info("Launched {} ", deploymentId);
		Timeout timeout = launchTimeout();
		assertThat(deploymentId,
				eventually(hasStatusThat(Matchers.hasProperty("state", is(complete))),
						timeout.maxAttempts, timeout.pause));

		deploymentId = taskLauncher.launch(request);
		logger.info("Re-launched {} ", deploymentId);
		assertThat(deploymentId,
				eventually(hasStatusThat(Matchers.hasProperty("state", is(complete))),
						timeout.maxAttempts, timeout.pause));
	}

	@Test
	public void testCommandLineArgs() {
		logger.info("Testing {}...", "CommandLineArgs");
		Map<String, String> properties = new HashMap<>();
		properties.put("killDelay", "1000");
		AppDefinition definition = new AppDefinition(this.randomName(), properties);
		Resource resource = integrationTestTask();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource,
				Collections.emptyMap(), Collections.singletonList("--exitCode=0"));
		logger.info("Launching {}...", request.getDefinition().getName());
		String deploymentId = taskLauncher.launch(request);
		logger.info("Launched {} ", deploymentId);

		Timeout timeout = launchTimeout();
		assertThat(deploymentId,
				eventually(hasStatusThat(Matchers.hasProperty("state", is(complete))),
						timeout.maxAttempts, timeout.pause));
	}

	protected String randomName() {
		// Kubernetes app names must start with a letter and can only be 24 characters
		return "job-" + UUID.randomUUID().toString().substring(0, 18);
	}

	protected Resource integrationTestTask() {
		return new DockerResource(
				"springcloud/spring-cloud-deployer-spi-test-app:latest");
	}

	protected Timeout launchTimeout() {
		return new Timeout(20, 5000);
	}

	protected Matcher<String> hasStatusThat(final Matcher<TaskStatus> statusMatcher) {
		return new BaseMatcher() {
			private TaskStatus status;

			public boolean matches(Object item) {
				this.status = KubernetesTaskLauncherIntegrationTest.this.taskLauncher
						.status((String) item);
				return statusMatcher.matches(this.status);
			}

			public void describeMismatch(Object item, Description mismatchDescription) {
				mismatchDescription.appendText("status of ").appendValue(item)
						.appendText(" ");
				statusMatcher.describeMismatch(this.status, mismatchDescription);
			}

			public void describeTo(Description description) {
				statusMatcher.describeTo(description);
			}
		};
	}

	protected static class Timeout {
		public final int maxAttempts;
		public final int pause;

		public Timeout(int maxAttempts, int pause) {
			this.maxAttempts = maxAttempts;
			this.pause = pause;
		}
	}

	@Configuration
	public static class Config {

		@Bean
		public MavenProperties mavenProperties() {
			MavenProperties mavenProperties = new MavenProperties();
			mavenProperties.setRemoteRepositories(
					ImmutableMap.of("maven.remote-repositories.spring.url",
							new MavenProperties.RemoteRepository(
									"http://repo.spring.io/snapshots")));
			return mavenProperties;
		}
	}
}
