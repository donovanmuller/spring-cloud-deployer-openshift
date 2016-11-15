package org.springframework.cloud.deployer.spi.openshift.resources.pod;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.core.io.Resource;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.Container;

public class OpenShiftContainerFactoryTest {

	@Test
	public void createWithEntryPointStyle() throws JsonProcessingException {
		KubernetesDeployerProperties kubernetesDeployerProperties = new KubernetesDeployerProperties();
		OpenShiftContainerFactory containerFactory = new OpenShiftContainerFactory(kubernetesDeployerProperties,
				new VolumeMountFactory());

		Map<String, String> appProps = new HashMap<>();
		appProps.put("foo.bar.baz", "test");
		AppDefinition definition = new AppDefinition("app-test", appProps);
		Resource resource = getResource();
		Map<String, String> props = new HashMap<>();

		props.put("spring.cloud.deployer.openshift.entryPointStyle", "shell");
		AppDeploymentRequest appDeploymentRequestShell = new AppDeploymentRequest(definition, resource, props);
		Container containerShell = containerFactory.create("app-test", appDeploymentRequestShell, null, null);
		assertNotNull(containerShell);
		assertTrue(containerShell.getEnv().get(0).getName().equals("FOO_BAR_BAZ"));
		assertTrue(containerShell.getArgs().size() == 0);

		props.clear();
		AppDeploymentRequest appDeploymentRequestExecDefault = new AppDeploymentRequest(definition, resource, props);
		Container containerExecDefault = containerFactory.create("app-test", appDeploymentRequestExecDefault, null,
				null);
		assertNotNull(containerExecDefault);
		assertTrue(containerExecDefault.getEnv().size() == 0);
		assertTrue(containerExecDefault.getArgs().get(0).equals("--foo.bar.baz=test"));

		props.put("spring.cloud.deployer.openshift.entryPointStyle", "exec");
		AppDeploymentRequest appDeploymentRequestExec = new AppDeploymentRequest(definition, resource, props);
		Container containerExec = containerFactory.create("app-test", appDeploymentRequestExec, null, null);
		assertNotNull(containerExec);
		assertTrue(containerExec.getEnv().size() == 0);
		assertTrue(containerExec.getArgs().get(0).equals("--foo.bar.baz=test"));

		props.put("spring.cloud.deployer.openshift.entryPointStyle", "boot");
		AppDeploymentRequest appDeploymentRequestBoot = new AppDeploymentRequest(definition, resource, props);
		Container containerBoot = containerFactory.create("app-test", appDeploymentRequestBoot, null, null);
		assertNotNull(containerBoot);
		assertTrue(containerBoot.getEnv().get(0).getName().equals("SPRING_APPLICATION_JSON"));
		assertTrue(containerBoot.getEnv().get(0).getValue().equals(new ObjectMapper().writeValueAsString(appProps)));
		assertTrue(containerBoot.getArgs().size() == 0);
	}

	private Resource getResource() {
		return new MavenResource.Builder().groupId("test.com").artifactId("test").version("1.0-SNAPSHOT").build();
	}
}
