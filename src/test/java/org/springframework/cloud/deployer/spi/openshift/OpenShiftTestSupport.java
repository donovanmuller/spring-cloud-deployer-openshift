package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.stream.test.junit.AbstractExternalResourceTestSupport;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;

/**
 * JUnit {@link org.junit.Rule} that detects the fact that a OpenShift installation is
 * available.
 */
public class OpenShiftTestSupport
		extends AbstractExternalResourceTestSupport<KubernetesClient> {

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
		context = SpringApplication.run(Config.class);
		resource = context.getBean(KubernetesClient.class);
		resource.namespaces().list();
	}

	@Configuration
	@EnableAutoConfiguration
	@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
	@EnableConfigurationProperties({ KubernetesDeployerProperties.class,
			OpenShiftDeployerProperties.class })
	public static class Config {

		@Autowired
		private KubernetesDeployerProperties properties;

		@Bean
		@Primary
		public KubernetesClient kubernetesClient() {
			return new DefaultOpenShiftClient().inNamespace(properties.getNamespace());
		}

		@Bean
		public MavenProperties mavenProperties() {
			return new MavenProperties();
		}
	}
}
