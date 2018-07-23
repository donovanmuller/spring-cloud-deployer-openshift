package org.springframework.cloud.deployer.spi.openshift;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.Job;
import io.fabric8.openshift.client.OpenShiftClient;
import org.hamcrest.Matchers;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.resources.pod.OpenShiftContainerFactory;
import org.springframework.cloud.deployer.spi.task.LaunchState;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.test.AbstractTaskLauncherIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.cloud.deployer.spi.test.EventuallyMatcher.eventually;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = {
		MavenOpenShiftTaskLauncherWithJobIntegrationTest.Config.class,
		OpenShiftAutoConfiguration.class })
@TestPropertySource(properties = { "spring.cloud.deployer.openshift.create-job=true" })
public class MavenOpenShiftTaskLauncherWithJobIntegrationTest
		extends AbstractTaskLauncherIntegrationTests {

	@ClassRule
	public static OpenShiftTestSupport openShiftTestSupport = new OpenShiftTestSupport();

	@Autowired
	private ResourceAwareOpenShiftTaskLauncher taskLauncher;

	@Override
	protected TaskLauncher provideTaskLauncher() {
		return taskLauncher;
	}

	@Autowired
	private OpenShiftClient openShiftClient;

	@Autowired
	private OpenShiftContainerFactory containerFactory;

	@Override
	protected String randomName() {
		// Kubernetes app names must start with a letter and can only be 24 characters
		return "task-" + UUID.randomUUID().toString().substring(0, 18);
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(20, 5000);
	}

	@Override
	protected Resource testApplication() {
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

	@Test
	@Override
	@Ignore("Currently reported as completed instead of cancelled")
	public void testSimpleCancel() throws InterruptedException {
		super.testSimpleCancel();
	}

	@Test
	public void testJobAnnotations() {
		log.info("Testing {}...", "JobAnnotations");

		OpenShiftDeployerProperties deployerProperties = new OpenShiftDeployerProperties();
		deployerProperties.setCreateJob(true);

		OpenShiftTaskLauncher taskLauncher = new OpenShiftTaskLauncher(deployerProperties,
				openShiftClient, containerFactory);

		AppDefinition definition = new AppDefinition(randomName(), null);
		Resource resource = testApplication();

		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource,
				Collections.singletonMap(
						"spring.cloud.deployer.kubernetes.jobAnnotations",
						"key1:val1,key2:val2,key3:val31:val32"));

		log.info("Launching {}...", request.getDefinition().getName());

		String launchId = taskLauncher.launch(request);
		Timeout timeout = deploymentTimeout();

		assertThat(launchId,
				eventually(
						hasStatusThat(Matchers.hasProperty("state",
								Matchers.is(LaunchState.launching))),
						timeout.maxAttempts, timeout.pause));

		String taskName = request.getDefinition().getName();

		log.info("Checking Job spec annotations of {}...", taskName);

		List<Job> jobs = openShiftClient.extensions().jobs()
				.withLabel("task-name", taskName).list().getItems();

		assertThat(jobs.size(), is(1));

		Map<String, String> annotations = jobs.get(0).getMetadata().getAnnotations();
		assertFalse(annotations.isEmpty());
		assertTrue(annotations.size() == 3);
		assertTrue(annotations.containsKey("key1"));
		assertTrue(annotations.get("key1").equals("val1"));
		assertTrue(annotations.containsKey("key2"));
		assertTrue(annotations.get("key2").equals("val2"));
		assertTrue(annotations.containsKey("key3"));
		assertTrue(annotations.get("key3").equals("val31:val32"));

		log.info("Destroying {}...", taskName);

		timeout = undeploymentTimeout();
		taskLauncher.destroy(taskName);

		assertThat(taskName, eventually(
				hasStatusThat(
						Matchers.hasProperty("state", Matchers.is(LaunchState.unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	@Configuration
	public static class Config {

		@Bean
		@ConfigurationProperties("maven")
		public MavenProperties mavenProperties() {
			MavenProperties mavenProperties = new MavenProperties();
			mavenProperties.setRemoteRepositories(
					ImmutableMap.of("maven.remote-repositories.spring.url",
							new MavenProperties.RemoteRepository(
									"http://repo.spring.io/libs-snapshot")));
			return mavenProperties;
		}

	}

}
