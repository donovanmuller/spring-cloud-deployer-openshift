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

import java.io.IOException;
import java.util.Properties;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.ImmutableMap;

@SpringApplicationConfiguration(classes = { KubernetesAutoConfiguration.class,
		OpenShiftAutoConfiguration.class,
		OpenShiftTaskLauncherMavenIntegrationTest.Config.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class OpenShiftTaskLauncherMavenIntegrationTest
		extends KubernetesTaskLauncherIntegrationTest {

	@ClassRule
	public static OpenShiftTestSupport openShiftTestSupport = new OpenShiftTestSupport();

	@Test
	public void testSimpleLaunch() {
		super.testSimpleLaunch();
	}

	@Test
	public void testReLaunch() {
		super.testReLaunch();
	}

	@Test
	public void testCommandLineArgs() {
		super.testCommandLineArgs();
	}

	@Override
	protected Resource integrationTestTask() {
		Properties properties = new Properties();
		try {
			properties.load(new ClassPathResource("integration-test-app.properties")
					.getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException(
					"Failed to determine which version of integration-test-app to use",
					e);
		}
		return new MavenResource.Builder().groupId("org.springframework.cloud")
				.artifactId("spring-cloud-deployer-spi-test-app").classifier("exec")
				.version(properties.getProperty("version")).extension("jar").build();
	}

	@Override
	protected Timeout launchTimeout() {
		return new Timeout(36, 10000);
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
