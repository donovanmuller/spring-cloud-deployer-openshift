package org.springframework.cloud.deployer.spi.openshift;

public interface OpenShiftApplicationPropertyKeys {

	/**
	 * The Git remote repository URI that will contain a Dockerfile in src/main/docker.
	 * See https://docs.openshift.org/latest/dev_guide/builds.html#source-code
	 */
	String OPENSHIFT_BUILD_GIT_URI_PROPERTY = "spring.cloud.deployer.openshift.build.git.uri";

	/**
	 * The Git branch/reference for
	 * {@link OpenShiftApplicationPropertyKeys#OPENSHIFT_BUILD_GIT_URI_PROPERTY}. See
	 * https://docs.openshift.org/latest/dev_guide/builds.html#source-code
	 */
	String OPENSHIFT_BUILD_GIT_REF_PROPERTY = "spring.cloud.deployer.openshift.build.git.ref";

	/**
	 * The location, relative to the project root, where the Dockerfile is located.
	 */
	String OPENSHIFT_BUILD_GIT_DOCKERFILE_PATH = "spring.cloud.deployer.openshift.build.git.dockerfile";

	/**
	 * If the remote Git repository requires a secret. See
	 * https://docs.openshift.org/latest/dev_guide/builds.html#using-secrets
	 */
	String OPENSHIFT_BUILD_GIT_SOURCE_SECRET = "spring.cloud.deployer.openshift.build.git.secret";

}
