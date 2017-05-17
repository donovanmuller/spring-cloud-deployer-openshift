package org.springframework.cloud.deployer.spi.openshift.resources.pod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.OpenShiftDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.fabric8.kubernetes.api.model.Container;

public class OpenShiftContainerFactoryTest {

	@Test
	public void createWithMavenResource() throws JsonProcessingException {
		OpenShiftDeployerProperties properties = new OpenShiftDeployerProperties();
		OpenShiftContainerFactory containerFactory = new OpenShiftContainerFactory(properties,
				new VolumeMountFactory(properties));

		AppDefinition definition = new AppDefinition("app-test", null);
		Resource resource = getResource();

		AppDeploymentRequest appDeploymentRequestShell = new AppDeploymentRequest(definition, resource, null);
		Container container = containerFactory.create("app-test", appDeploymentRequestShell, null, null, false);

		assertNotNull(container);
		assertThat(container.getImage()).isEqualTo("app-test");
	}

	private Resource getResource() {
		return new MavenResource.Builder().groupId("test.com").artifactId("test").version("1.0-SNAPSHOT").build();
	}
}
