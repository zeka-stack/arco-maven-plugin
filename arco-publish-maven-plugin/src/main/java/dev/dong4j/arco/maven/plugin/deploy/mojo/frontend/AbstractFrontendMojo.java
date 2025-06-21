package dev.dong4j.arco.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

import java.io.File;
import java.util.Map;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.7.0
 */
public abstract class AbstractFrontendMojo extends AbstractMojo {

    /** Execution */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    /**
     * Whether you should skip while running in the test phase (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    protected Boolean skipTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 1.4
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    protected boolean testFailureIgnore;

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    protected File workingDirectory;

    /**
     * The base directory for installing node and npm.
     */
    @Parameter(property = "installDirectory", required = false)
    protected File installDirectory;

    /**
     * Additional environment variables to pass to the build.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    /** Project */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** Repository system session */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * Determines if this execution should be skipped.
     *
     * @return the boolean
     * @since 1.7.0
     */
    private boolean skipTestPhase() {
        return this.skipTests && this.isTestingPhase();
    }

    /**
     * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
     *
     * @return the boolean
     * @since 1.7.0
     */
    private boolean isTestingPhase() {
        String phase = this.execution.getLifecyclePhase();
        return "test".equals(phase) || "integration-test".equals(phase);
    }

    /**
     * Execute
     *
     * @param factory factory
     * @throws FrontendException frontend exception
     * @since 1.7.0
     */
    protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     *
     * @return the boolean
     * @since 1.7.0
     */
    protected abstract boolean skipExecution();

    /**
     * Execute
     *
     * @throws MojoFailureException mojo failure exception
     * @since 1.7.0
     */
    @Override
    @SuppressWarnings("java:S3776")
    public void execute() throws MojoFailureException {
        if (this.testFailureIgnore && !this.isTestingPhase()) {
            this.getLog().info("testFailureIgnore property is ignored in non test phases");
        }
        if (!(this.skipTestPhase() || this.skipExecution())) {
            if (this.installDirectory == null) {
                this.installDirectory = this.workingDirectory;
            }
            try {
                this.execute(new FrontendPluginFactory(this.workingDirectory, this.installDirectory,
                    new RepositoryCacheResolver(this.repositorySystemSession)));
            } catch (TaskRunnerException e) {
                if (this.testFailureIgnore && this.isTestingPhase()) {
                    this.getLog().error("There are test failures.\nFailed to run task: " + e.getMessage(), e);
                } else {
                    throw new MojoFailureException("Failed to run task", e);
                }
            } catch (FrontendException e) {
                throw MojoUtils.toMojoFailureException(e);
            }
        } else {
            this.getLog().info("Skipping execution.");
        }
    }

}
