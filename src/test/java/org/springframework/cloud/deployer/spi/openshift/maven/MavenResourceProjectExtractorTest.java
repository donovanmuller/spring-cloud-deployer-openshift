package org.springframework.cloud.deployer.spi.openshift.maven;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.maven.project.MavenProject;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.resource.maven.MavenResource;

public class MavenResourceProjectExtractorTest {

	private MavenResourceProjectExtractor mavenResourceProjectExtractor = new MavenResourceProjectExtractor();

	@Test
	@Ignore("The artifact is not resolvable on the Travis build. Need to fix.")
	public void extractMavenProject() throws Exception {
		copy("src/test/resources/test-app-1.0-SNAPSHOT.pom",
				"target/.m2/repository/org/test/test-app/1.0-SNAPSHOT");
		copy("src/test/resources/test-app-1.0-SNAPSHOT.jar",
				"target/.m2/repository/org/test/test-app/1.0-SNAPSHOT");
		System.setProperty("user.home", new File("target").getAbsolutePath());
		MavenProject mavenProject = mavenResourceProjectExtractor.extractMavenProject(
				MavenResource.parse("org.test:test-app:1.0-SNAPSHOT"),
				new MavenProperties());

		assertNotNull(mavenProject);
	}

	private void copy(String from, String to) throws IOException {
		Path source = new File(from).toPath().toAbsolutePath();
		Path destination = new File(to).toPath().toAbsolutePath();

		if (!Files.exists(destination)) {
			Files.createDirectories(destination);
		}

		Files.copy(source,
				new File(destination.toAbsolutePath().toString(),
						source.getFileName().toString()).toPath(),
				StandardCopyOption.REPLACE_EXISTING);
	}
}
