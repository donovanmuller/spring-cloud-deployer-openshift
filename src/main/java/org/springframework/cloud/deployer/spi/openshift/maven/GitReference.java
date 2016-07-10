package org.springframework.cloud.deployer.spi.openshift.maven;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class GitReference {

	private String uri;
	private String branch = "master";

	public GitReference(String uri) {
		this.uri = uri;
	}

	public GitReference(String uri, String branch) {
		this.uri = uri;
		this.branch = branch;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	/**
	 * Take the Maven <scm><connection>...</connection></scm> value and replace everything
	 * up until the '@' character with 'ssh://git@' so that OpenShift can clone the
	 * repository.
	 *
	 * Example:
	 *
	 * <code>
	 *   <connection>scm:git:git@github.com:spring-cloud/spring-cloud-dataflow.git</connection>
	 * </code>
	 *
	 * becomes:
	 *
	 * <code>
	 *     ssh://git@github.com:spring-cloud/spring-cloud-dataflow.git
	 * </code>
	 */
	public String getParsedUri() {
		String parsedUri = uri;
		if (StringUtils.isNotBlank(uri)) {
			parsedUri = uri.replaceFirst(".*@", "ssh://git@");
		}

		return parsedUri;
	}

	public String getBranch() {
		return branch;
	}

	public void setBranch(String branch) {
		this.branch = branch;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o)
			return true;

		if (o == null || getClass() != o.getClass())
			return false;

		final GitReference that = (GitReference) o;

		return new EqualsBuilder().append(uri, that.uri).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(uri).toHashCode();
	}
}
