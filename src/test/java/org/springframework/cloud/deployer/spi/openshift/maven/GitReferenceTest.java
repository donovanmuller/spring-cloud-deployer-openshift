package org.springframework.cloud.deployer.spi.openshift.maven;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GitReferenceTest {

	@Test
	public void parseGitReference() {
		GitReference gitReference = new GitReference(
				"scm:git:git@github.com/spring-cloud/spring-cloud-deployer.git", "HEAD");

		assertThat(gitReference.getParsedUri())
				.isEqualTo("ssh://git@github.com/spring-cloud/spring-cloud-deployer.git");
		assertThat(gitReference.getBranch()).isEqualTo("HEAD");
	}
}
