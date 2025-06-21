package dev.dong4j.arco.maven.plugin.helper.deploy;

import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.MavenArtifactRepository;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Abstract class for Deploy mojo's.
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.02 11:27
 * @since 1.0.0
 */
abstract class AbstractDeployMojo extends ArcoMavenPluginAbstractMojo {

    /**
     * Flag whether Maven is currently in online/offline mode.
     */
    @Parameter(defaultValue = "${settings.offline}", readonly = true)
    private boolean offline;

    /**
     * Parameter used to control how many times a failed deployment will be retried before giving up and failing. If a
     * value outside the range 1-10 is specified it will be pulled to the nearest value within the range 1-10.
     *
     * @since 2.7
     */
    @Parameter(property = "retryFailedDeploymentCount", defaultValue = "1")
    private int retryFailedDeploymentCount;

    /**
     * Fail if offline *
     *
     * @throws MojoFailureException mojo failure exception
     * @since 1.0.0
     */
    void failIfOffline()
        throws MojoFailureException {
        if (this.offline) {
            throw new MojoFailureException("Cannot deploy artifacts when Maven is in offline mode");
        }
    }

    /**
     * Gets retry failed deployment count *
     *
     * @return the retry failed deployment count
     * @since 1.0.0
     */
    int getRetryFailedDeploymentCount() {
        return this.retryFailedDeploymentCount;
    }

    /**
     * Create deployment artifact repository
     *
     * @param id  id
     * @param url url
     * @return the artifact repository
     * @since 1.0.0
     */
    ArtifactRepository createDeploymentArtifactRepository(String id, String url) {
        return new MavenArtifactRepository(id, url, new DefaultRepositoryLayout(), new ArtifactRepositoryPolicy(),
            new ArtifactRepositoryPolicy());
    }

}
