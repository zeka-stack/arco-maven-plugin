package dev.dong4j.zeka.maven.plugin.common;

import dev.dong4j.zeka.maven.plugin.common.enums.ApplicationType;
import dev.dong4j.zeka.maven.plugin.common.util.ReflectionUtils;
import java.io.File;
import java.util.Set;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

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

    /**
     * 从当前项目的依赖判断是 boot 还是 cloud 应用
     *
     * @return the application type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    protected ApplicationType deduceFromDependencies() {
        // 通过反射获取当前项目的所有依赖
        Set<Artifact> resolvedArtifacts = (Set<Artifact>) ReflectionUtils.getFieldVal(this.project,
            "resolvedArtifacts",
            false);

        boolean cloudType = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.CLOUD_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        boolean bootType = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.BOOT_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        boolean enableNacosConfig = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.NCAOS_CONFIG_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        if (cloudType && enableNacosConfig) {
            return ApplicationType.CLOUD;
        }
        return bootType ? ApplicationType.BOOT : ApplicationType.NONE;
    }
}
