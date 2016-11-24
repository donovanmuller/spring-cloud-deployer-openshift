package org.springframework.cloud.deployer.spi.openshift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesAutoConfiguration;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftTaskLauncher;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenResourceJarExtractor;
import org.springframework.cloud.deployer.spi.openshift.resources.pod.OpenShiftContainerFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountConfigServerFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.cloud.deployer.spi.task.TaskLauncher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.openshift.client.DefaultOpenShiftClient;

/**
 * Spring Bean configuration for the OpenShift deployer.
 *
 * @author Donovan Muller
 */
@Configuration
@EnableAutoConfiguration(exclude = KubernetesAutoConfiguration.class)
@EnableConfigurationProperties(OpenShiftDeployerProperties.class)
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class OpenShiftAutoConfiguration {

	@Autowired
	private OpenShiftDeployerProperties properties;

	@Autowired
	private MavenProperties mavenProperties;

	@Bean
	public AppDeployer appDeployer(KubernetesClient kubernetesClient, ContainerFactory containerFactory,
			MavenResourceJarExtractor mavenResourceJarExtractor, ResourceHash resourceHash) {
		return new ResourceAwareOpenShiftAppDeployer(
				new OpenShiftAppDeployer(properties, kubernetesClient, containerFactory),
				new MavenOpenShiftAppDeployer(properties, kubernetesClient, containerFactory, mavenResourceJarExtractor,
						mavenProperties, resourceHash));
	}

	@Bean
	public TaskLauncher taskDeployer(KubernetesClient kubernetesClient,
			MavenResourceJarExtractor mavenResourceJarExtractor, ResourceHash resourceHash) {
		return new ResourceAwareOpenShiftTaskLauncher(new OpenShiftTaskLauncher(properties, kubernetesClient),
				new MavenOpenShiftTaskLauncher(properties, properties, mavenProperties, kubernetesClient,
						mavenResourceJarExtractor, resourceHash));
	}

	@Bean
	public KubernetesClient kubernetesClient() {
		return new DefaultOpenShiftClient().inNamespace(properties.getNamespace());
	}

	@Bean
	public ContainerFactory containerFactory(VolumeMountFactory volumeMountFactory) {
		return new OpenShiftContainerFactory(properties, volumeMountFactory);
	}

	@Bean
	public MavenResourceJarExtractor mavenResourceJarExtractor() {
		return new MavenResourceJarExtractor();
	}

	@Bean
	public ResourceHash resourceHash() {
		return new ResourceHash();
	}

	@Bean
	public VolumeMountFactory volumeMountFactory(ConfigServicePropertySourceLocator configServicePropertySourceLocator,
			OpenShiftDeployerProperties openShiftDeployerProperties) {
		return new VolumeMountConfigServerFactory(configServicePropertySourceLocator, openShiftDeployerProperties);
	}
}
