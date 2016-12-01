package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import io.fabric8.kubernetes.client.KubernetesClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a OpenShift installation is available.
 */
public class OpenShiftTestSupport extends AbstractExternalResourceTestSupport<KubernetesClient> {

	private ConfigurableApplicationContext context;

	protected OpenShiftTestSupport() {
		super("OPENSHIFT");
	}

	@Override
	protected void cleanupResource() throws Exception {
		context.close();
	}

	@Override
	protected void obtainResource() throws Exception {
		context = new SpringApplicationBuilder()
			.web(false)
			.bannerMode(Banner.Mode.OFF)
			.sources(Config.class)
			.run();
		resource = context.getBean(KubernetesClient.class);
		resource.namespaces().list();
	}

	@TestConfiguration
	@Import(OpenShiftAutoConfiguration.class)
	@ConditionalOnProperty(value = "openshift.enabled", matchIfMissing = true)
	public static class Config {

		@Bean
		public MavenProperties mavenProperties() {
			return new MavenProperties();
		}
	}
}
