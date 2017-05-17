package org.springframework.cloud.deployer.spi.openshift.maven;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;

public class MavenResourceJarExtractorTest {

	private MavenResourceJarExtractor mavenResourceJarExtractor = new MavenResourceJarExtractor();

	@Test
	public void extractFile() throws IOException {
		Optional<Resource> resource = mavenResourceJarExtractor.extractFile(
				new ClassPathResource("test.war"), "on/the/path/to/war/test-file.txt");

		assertThat(resource).isPresent();
		assertThat(resource.get().getInputStream()).hasSameContentAs(
				new ByteArrayInputStream("Free me from my life of .war!".getBytes()));
	}

	@Test
	public void dontExtractNonExistentFile() throws IOException {
		Optional<Resource> resource = mavenResourceJarExtractor
				.extractFile(new ClassPathResource("test.war"), "existence.false");

		assertThat(resource).isNotPresent();
	}
}
