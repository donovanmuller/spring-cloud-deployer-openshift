package org.springframework.cloud.deployer.spi.openshift;

import org.junit.ClassRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.test.AbstractAppDeployerIntegrationTests;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;

@SpringApplicationConfiguration(classes = { KubernetesAutoConfiguration.class,
		OpenShiftAutoConfiguration.class,
		OpenShiftAppDeployerMavenIntegrationTest.Config.class })
public class OpenShiftAppDeployerMavenIntegrationTest
		extends AbstractAppDeployerIntegrationTests {

	@ClassRule
	public static OpenShiftTestSupport openShiftAvailable = new OpenShiftTestSupport();

	@Autowired
	private AppDeployer appDeployer;

	@Override
	protected AppDeployer appDeployer() {
		return appDeployer;
	}

	/**
	 * See {@link KubernetesAppDeployerIntegrationTest#randomName()}
	 */
	@Override
	protected String randomName() {
		return "app-" + super.randomName().substring(0, 18);
	}

	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(100, 10000);
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
