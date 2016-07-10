package org.springframework.cloud.deployer.spi.openshift;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.junit.Test;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;
import org.springframework.cloud.deployer.spi.core.AppDefinition;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenResourceProjectExtractor;

public class OpenShiftMavenDeploymentRequestTest {

	@Test
	public void contructOpenShiftMavenDeploymentRequest() throws Exception {
		MavenResourceProjectExtractor mavenResourceProjectExtractor = mock(
				MavenResourceProjectExtractor.class);
		MavenProject mavenProject = new MavenProject();
		Scm scm = new Scm();
		scm.setConnection("ssh://git@github.com/spring-cloud/spring-cloud-deployer.git");
		scm.setTag("HEAD");
		mavenProject.setScm(scm);
		when(mavenResourceProjectExtractor.extractMavenProject(any(), any()))
				.thenReturn(mavenProject);

		AppDeploymentRequest request = new AppDeploymentRequest(
				new AppDefinition("testapp-source", null),
				MavenResource.parse("org.test:testapp-source:1.0-SNAPSHOT", null));

		OpenShiftMavenDeploymentRequest openShiftMavenDeploymentRequest = new OpenShiftMavenDeploymentRequest(
				request, mavenResourceProjectExtractor, new MavenProperties());

		assertThat(openShiftMavenDeploymentRequest.getGitReference())
				.isEqualTo(new GitReference(
						"ssh://git@github.com/spring-cloud/spring-cloud-deployer.git",
						"HEAD"));
	}
}
