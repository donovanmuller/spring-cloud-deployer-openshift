package org.springframework.cloud.deployer.spi.openshift.maven;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.zeroturnaround.zip.ZipUtil;

public class MavenResourceJarExtractor {

	private static Logger log = LoggerFactory.getLogger(MavenResourceJarExtractor.class);

	/**
	 * Extract a single file in the specified {@link Resource} that must be a zip archive.
	 * This single file will be represented as a {@link Resource}. The reference to the
	 * file in the archive must be <b>the absolute path</b> to the file. I.e.
	 * <code>/the/path/to/the/file.txt</code>, where <code>file.txt</code> is the file to
	 * extract.
	 *
	 * @param resource
	 * @param file
	 * @return a {@link Resource} representing the extracted file
	 * @throws IOException
	 */
	public Optional<Resource> extractFile(Resource resource, String file)
			throws IOException {
		log.debug("Extracting [{}] from: [{}]", file, resource.getFile());

		Optional<Resource> extractedResource = Optional.empty();

		byte[] unpackedEntry = ZipUtil.unpackEntry(resource.getFile(), file);
		if (unpackedEntry != null) {
			extractedResource = Optional.of(new ByteArrayResource(unpackedEntry));
		}

		return extractedResource;
	}
}
