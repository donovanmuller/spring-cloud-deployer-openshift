package org.springframework.cloud.deployer.spi.openshift;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HostPathVolumeSource;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.openshift.client.OpenShiftClient;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.app.AppDeployer;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.kubernetes.ContainerFactory;
import org.springframework.cloud.deployer.spi.kubernetes.KubernetesDeployerProperties;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenOpenShiftAppDeployer;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenResourceJarExtractor;
import org.springframework.cloud.deployer.spi.openshift.resources.pod.OpenShiftContainerFactory;
import org.springframework.cloud.deployer.spi.openshift.resources.volumes.VolumeMountFactory;
import org.springframework.cloud.deployer.spi.test.AbstractAppDeployerIntegrationTests;
import org.springframework.cloud.deployer.spi.test.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.deployed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.failed;
import static org.springframework.cloud.deployer.spi.app.DeploymentState.unknown;
import static org.springframework.cloud.deployer.spi.test.EventuallyMatcher.eventually;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = { MavenOpenShiftAppDeployerIntegrationTest.Config.class,
		OpenShiftAutoConfiguration.class })
public class MavenOpenShiftAppDeployerIntegrationTest extends AbstractAppDeployerIntegrationTests {

	@ClassRule
	public static OpenShiftTestSupport openShiftAvailable = new OpenShiftTestSupport();

	@Rule
	public TestName name = new TestName();

	@Autowired
	private AppDeployer appDeployer;

	@Autowired
	private OpenShiftClient openShiftClient;

	@Autowired
	private MavenResourceJarExtractor mavenResourceJarExtractor;

	@Autowired
	private ResourceHash resourceHash;

	@Override
	protected AppDeployer provideAppDeployer() {
		return appDeployer;
	}

	@Autowired
	private MavenProperties mavenProperties;

	@Override
	protected Resource testApplication() {
		Properties properties = new Properties();
		try {
			properties.load(new ClassPathResource("integration-test-app.properties").getInputStream());
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to determine which version of integration-test-app to use", e);
		}
		return new MavenResource.Builder(mavenProperties).groupId("org.springframework.cloud")
				.artifactId("spring-cloud-deployer-spi-test-app").version(properties.getProperty("version"))
				.classifier("exec").extension("jar").build();
	}

	@After
	public void cleanUp() {
		openShiftClient.services().withLabel("spring-app-id").delete();
		openShiftClient.routes().withLabel("spring-app-id").delete();
		openShiftClient.deploymentConfigs().withLabel("spring-app-id").delete();
		openShiftClient.replicationControllers().withLabel("spring-app-id").delete();
		openShiftClient.pods().withLabel("spring-app-id").delete();
		openShiftClient.pods().withLabel("openshift.io/deployer-pod-for.name").delete();
		openShiftClient.buildConfigs().withLabel("spring-app-id").delete();
		openShiftClient.builds().withLabel("spring-app-id").delete();
	}

	/**
	 * The test below are copied as is from KubernetesAppDeployerIntegrationTests. See
	 * spring-cloud-deployer-kubernetes project.
	 */

	@Test
	public void testFailedDeploymentWithLoadBalancer() {
		log.info("Testing {}...", "FailedDeploymentWithLoadBalancer");
		OpenShiftDeployerProperties openShiftDeployerProperties = new OpenShiftDeployerProperties();
		openShiftDeployerProperties.setCreateLoadBalancer(true);
		openShiftDeployerProperties.setLivenessProbePeriod(10);
		openShiftDeployerProperties.setMaxTerminatedErrorRestarts(1);
		openShiftDeployerProperties.setMaxCrashLoopBackOffRestarts(1);
		ContainerFactory containerFactory = new OpenShiftContainerFactory(openShiftDeployerProperties,
				new VolumeMountFactory(new OpenShiftDeployerProperties()));
		AppDeployer lbAppDeployer = new MavenOpenShiftAppDeployer(openShiftDeployerProperties, openShiftClient,
				containerFactory, mavenResourceJarExtractor, mavenProperties, resourceHash);

		AppDefinition definition = new AppDefinition(randomName(), null);
		Resource resource = testApplication();
		Map<String, String> props = new HashMap<>();
		// setting to small memory value will cause app to fail to be deployed
		props.put("spring.cloud.deployer.kubernetes.memory", "8Mi");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, props);

		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = lbAppDeployer.deploy(request);
		Timeout timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(failed))),
				timeout.maxAttempts, timeout.pause));

		log.info("Undeploying {}...", deploymentId);
		timeout = undeploymentTimeout();
		lbAppDeployer.undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	@Test
	public void testGoodDeploymentWithLoadBalancer() {
		log.info("Testing {}...", "GoodDeploymentWithLoadBalancer");
		OpenShiftDeployerProperties openShiftDeployerProperties = new OpenShiftDeployerProperties();
		openShiftDeployerProperties.setCreateLoadBalancer(true);
		openShiftDeployerProperties.setMinutesToWaitForLoadBalancer(1);
		ContainerFactory containerFactory = new OpenShiftContainerFactory(openShiftDeployerProperties,
				new VolumeMountFactory(openShiftDeployerProperties));
		AppDeployer lbAppDeployer = new MavenOpenShiftAppDeployer(openShiftDeployerProperties, openShiftClient,
				containerFactory, mavenResourceJarExtractor, mavenProperties, resourceHash);

		AppDefinition definition = new AppDefinition(randomName(), null);
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);

		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = lbAppDeployer.deploy(request);
		Timeout timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(deployed))),
				timeout.maxAttempts, timeout.pause));

		log.info("Undeploying {}...", deploymentId);
		timeout = undeploymentTimeout();
		lbAppDeployer.undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	@Test
	public void testDeploymentWithMountedHostPathVolume() throws IOException {
		log.info("Testing {}...", "DeploymentWithMountedVolume");
		String hostPath = "/tmp/" + randomName() + '/';
		String containerPath = "/tmp/";
		String subPath = randomName();
		String mountName = "mount";
		OpenShiftDeployerProperties openShiftDeployerProperties = new OpenShiftDeployerProperties();
		//@formatter:off
		openShiftDeployerProperties.setVolumes(Collections.singletonList(
			new VolumeBuilder()
				.withHostPath(new HostPathVolumeSource(hostPath))
				.withName(mountName)
				.build()));
		//@formatter:on
		openShiftDeployerProperties
				.setVolumeMounts(Collections.singletonList(new VolumeMount(hostPath, mountName, false, null)));
		ContainerFactory containerFactory = new OpenShiftContainerFactory(new OpenShiftDeployerProperties(),
				new VolumeMountFactory(openShiftDeployerProperties));
		AppDeployer lbAppDeployer = new MavenOpenShiftAppDeployer(openShiftDeployerProperties, openShiftClient,
				containerFactory, mavenResourceJarExtractor, mavenProperties, resourceHash);

		AppDefinition definition = new AppDefinition(randomName(),
				Collections.singletonMap("logging.file", containerPath + subPath));
		Resource resource = testApplication();
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource);

		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = lbAppDeployer.deploy(request);
		Timeout timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(deployed))),
				timeout.maxAttempts, timeout.pause));

		Map<String, String> selector = Collections.singletonMap("spring-app-id", deploymentId);
		PodSpec spec = openShiftClient.pods().withLabels(selector).list().getItems().get(0).getSpec();
		assertThat(spec.getVolumes(), is(notNullValue()));
		//@formatter:off
		Volume volume = spec.getVolumes().stream()
			.filter(v -> mountName.equals(v.getName()))
			.findAny()
			.orElseThrow(() -> new AssertionError("Volume not mounted"));
		//@formatter:on
		assertThat(volume.getHostPath(), is(notNullValue()));
		assertThat(hostPath, is(volume.getHostPath().getPath()));

		log.info("Undeploying {}...", deploymentId);
		timeout = undeploymentTimeout();
		lbAppDeployer.undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	@Test
	public void testDeploymentWithGroupAndIndex() throws IOException {
		log.info("Testing {}...", "DeploymentWithWithGroupAndIndex");

		AppDefinition definition = new AppDefinition(randomName(), new HashMap<>());
		Resource resource = testApplication();
		Map<String, String> props = new HashMap<>();
		props.put(AppDeployer.GROUP_PROPERTY_KEY, "foo");
		props.put(AppDeployer.INDEXED_PROPERTY_KEY, "true");
		AppDeploymentRequest request = new AppDeploymentRequest(definition, resource, props);

		log.info("Deploying {}...", request.getDefinition().getName());
		String deploymentId = appDeployer.deploy(request);
		Timeout timeout = deploymentTimeout();
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(deployed))),
				timeout.maxAttempts, timeout.pause));

		Map<String, String> selector = Collections.singletonMap("spring-app-id", deploymentId);
		PodSpec spec = openShiftClient.pods().withLabels(selector).list().getItems().get(0).getSpec();
		Map<String, String> envVars = new HashMap<>();
		for (EnvVar e : spec.getContainers().get(0).getEnv()) {
			envVars.put(e.getName(), e.getValue());
		}
		assertThat(envVars.get("SPRING_CLOUD_APPLICATION_GROUP"), is("foo"));
		assertThat(envVars.get("SPRING_APPLICATION_INDEX"), is("0"));

		log.info("Undeploying {}...", deploymentId);
		timeout = undeploymentTimeout();
		appDeployer.undeploy(deploymentId);
		assertThat(deploymentId, eventually(hasStatusThat(Matchers.hasProperty("state", is(unknown))),
				timeout.maxAttempts, timeout.pause));
	}

	/**
	 * Don't be "too random" as the builds can be reused. This is mostly useful running test during
	 * development. I.e. builds will not constantly download the spring-cloud-deployer-spi-test-app
	 * as part of the image build.
	 */
	@Override
	protected String randomName() {
		String appId = "app-" + name.getMethodName();
		return appId.length() >= 16 ? appId.substring(0, 16) : appId;
	}

	/**
	 * Extra time to account for downloading the spring-cloud-deployer-spi-test-app exec.jar
	 */
	@Override
	protected Timeout deploymentTimeout() {
		return new Timeout(50, 5000);
	}

	@TestConfiguration
	public static class Config {

		@Bean
		@ConfigurationProperties("maven")
		public MavenProperties mavenProperties() {
			MavenProperties mavenProperties = new MavenProperties();
			mavenProperties.setRemoteRepositories(ImmutableMap.of("maven.remote-repositories.spring.url",
					new MavenProperties.RemoteRepository("http://repo.spring.io/libs-snapshot")));
			return mavenProperties;
		}

		@Bean
		public KubernetesDeployerProperties kubernetesDeployerProperties() {
			KubernetesDeployerProperties properties = new KubernetesDeployerProperties();
			properties.setRequests(new KubernetesDeployerProperties.Resources("100m", "128Mi"));
			return properties;
		}
	}
}
