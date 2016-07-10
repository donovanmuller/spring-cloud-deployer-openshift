package org.springframework.cloud.deployer.spi.openshift;

import static com.google.common.hash.Hashing.sha1;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.Resource;

import com.google.common.io.Files;

public class ResourceHash {

	/**
	 * Generates a SHA-1 hash of the provided {@link Resource}
	 *
	 * @param resource
	 * @return a hash of the {@link Resource}
	 */
	public String hashResource(Resource resource) {
		try {
			File file = resource.getFile();
			String hash = Files.hash(file, sha1()).toString();

			return hash;
		}
		catch (IOException e) {
			throw new RuntimeException("Could not read resource to hash", e);
		}
	}
}
