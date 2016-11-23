package org.springframework.cloud.deployer.spi.openshift;

import org.apache.maven.model.Scm;
import org.apache.maven.project.MavenProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.deployer.resource.maven.MavenProperties;
import org.springframework.cloud.deployer.spi.core.AppDeploymentRequest;
import org.springframework.cloud.deployer.spi.openshift.maven.GitReference;
import org.springframework.cloud.deployer.spi.openshift.maven.MavenResourceProjectExtractor;

public class OpenShiftMavenDeploymentRequest extends AppDeploymentRequest {

	private static final Logger logger = LoggerFactory.getLogger(OpenShiftMavenDeploymentRequest.class);

	private GitReference gitReference;

	/**
	 * The {@link org.springframework.cloud.deployer.resource.maven.MavenResource} is used to parse
	 * and build a {@link MavenProject}. This is used to get the SCM details for use with the
	 * different build strategies.
	 *
	 * @param request
	 * @param mavenResourceProjectExtractor
	 * @param mavenProperties
	 * @throws IllegalStateException if the {@link MavenProject} cannot be parsed from the
	 * {@link org.springframework.cloud.deployer.resource.maven.MavenResource}
	 */
	public OpenShiftMavenDeploymentRequest(AppDeploymentRequest request,
			MavenResourceProjectExtractor mavenResourceProjectExtractor, MavenProperties mavenProperties) {
		super(request.getDefinition(), request.getResource(), request.getDeploymentProperties(),
				request.getCommandlineArguments());

		try {
			MavenProject mavenProject = mavenResourceProjectExtractor.extractMavenProject(this.getResource(),
					mavenProperties);
			Scm scm = mavenProject.getScm();
			this.gitReference = new GitReference(scm.getConnection(), scm.getTag());
		}
		catch (Exception e) {
			logger.warn(String.format(
					"Maven project could not be extracted. Maven resource [%s] possibly has no pom extension artifact",
					getResource()), e);
		}
	}

	public boolean isMavenProjectExtractable() {
		return gitReference != null;
	}

	public OpenShiftMavenDeploymentRequest(AppDeploymentRequest request, MavenProperties mavenProperties) {
		this(request, new MavenResourceProjectExtractor(), mavenProperties);
	}

	public GitReference getGitReference() {
		return gitReference;
	}
}
