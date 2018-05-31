package org.springframework.cloud.deployer.spi.openshift;

import java.util.UUID;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.deployer.resource.docker.DockerResource;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.cloud.deployer.spi.test.AbstractTaskLauncherIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Copied <a href="https://github.com/spring-cloud/spring-cloud-deployer-kubernetes">from
 * spring-cloud-deployer-kubernetes</a> to test the <code>docker:</code> resource
 * handling.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = { OpenShiftTaskLauncherIntegrationTest.Config.class,
		OpenShiftAutoConfiguration.class })
public class OpenShiftTaskLauncherIntegrationTest
		extends AbstractTaskLauncherIntegrationTests {

	@ClassRule
	public static OpenShiftTestSupport openShiftTestSupport = new OpenShiftTestSupport();

	@Autowired
	private TaskLauncher taskLauncher;

	@Override
	protected TaskLauncher provideTaskLauncher() {
		return taskLauncher;
	}

	@Override
	protected TaskLauncher taskLauncher() {
		return this.taskLauncher;
	}

	@Test
	@Override
	@Ignore("Currently reported as completed instead of cancelled")
	public void testSimpleCancel() throws InterruptedException {
		super.testSimpleCancel();
	}

	@Override
	protected String randomName() {
		// Kubernetes app names must start with a letter and can only be 24 characters
		return "task-" + UUID.randomUUID().toString().substring(0, 18);
	}

	@Override
	protected Resource testApplication() {
		return new DockerResource(
				"springcloud/spring-cloud-deployer-spi-test-app:latest");
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(36, 5000);
	}

}
