package dev.dong4j.zeka.maven.plugin.common;

import lombok.Getter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.3
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
public abstract class ZekaMavenPluginAbstractMojo extends AbstractMojo {
    /** 当前处理的项目 */
    @Getter
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;
    /** Session */
    @Getter
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    public MavenSession session;
    /** Build directory */
    @Getter
    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    public String buildDirectory;
    /** Basedir */
    @Getter
    @Parameter(readonly = true, defaultValue = "${project.basedir}")
    public String basedir;
    /** Target file */
    @Getter
    @Parameter(defaultValue = "${project.build.directory}")
    public File targetFile;
    /** Build context */
    @Component
    public BuildContext buildContext;
    /** Maven ProjectHelper. */
    @Component
    public MavenProjectHelper projectHelper;

    /**
     * 设置 properties
     *
     * @param name  name
     * @param value value
     * @since 1.0.0
     */
    public void defineProperty(String name, String value) {
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("define property " + name + " = \"" + value + "\"");
        }

        this.project.getProperties().put(name, value);
    }

    /**
     * Execute
     *
     * @throws MojoExecutionException mojo execution exception
     * @throws MojoFailureException   mojo failure exception
     * @since 1.5.0
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }
}
