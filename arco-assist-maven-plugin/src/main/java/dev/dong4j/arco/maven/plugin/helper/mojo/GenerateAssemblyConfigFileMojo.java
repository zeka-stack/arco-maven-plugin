package dev.dong4j.arco.maven.plugin.helper.mojo;

import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.FileWriter;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import dev.dong4j.arco.maven.plugin.common.enums.ApplicationType;
import dev.dong4j.arco.maven.plugin.common.util.FileUtils;
import dev.dong4j.arco.maven.plugin.common.util.ReflectionUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * <p>Description: 动态生成 assembly.xml </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 11:53
 * @since 1.0.0
 */
@Mojo(name = "generate-assembly-config", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateAssemblyConfigFileMojo extends ArcoMavenPluginAbstractMojo {

    /** 默认忽略此插件 */
    @Parameter(property = Plugins.SKIP_ASSEMBLY_CONFIG, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;
    /** Output file */
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/assembly/assembly.xml")
    private File outputFile;
    /** 自定义的打包配置 */
    @Parameter(defaultValue = "${project.basedir}/assembly/assembly.xml")
    private File assemblyFile;

    /** ASSEMBLY_FILE_NAME */
    public static final String ASSEMBLY_FILE_NAME = "META-INF/assembly/assembly.xml";
    /** ASSEMBLY_EXCLUDES_FILE_NAME */
    public static final String ASSEMBLY_EXCLUDES_FILE_NAME = "META-INF/assembly/excludes.xml";
    /** DEPENDENCES_EXCLUDES */
    public static final String DEPENDENCES_EXCLUDES = "#{excludes}";
    /** DEFAULT_DEPENDENCES_EXCLUDES */
    public static final String DEFAULT_DEPENDENCES_EXCLUDES = "<excludes>\n" +
        "    <!-- 排除自己, 排除 devtools -->\n" +
        "    <exclude>${groupId}:${project.artifactId}</exclude>\n" +
        "</excludes>";
    /** PROPERTIES_INCLUDE */
    public static final String PROPERTIES_INCLUDE = "#{include}";
    /** BOOT_PROPERTIES_INCLUDE */
    public static final String BOOT_PROPERTIES_INCLUDE = "application*.yml";
    /** CLOUD_PROPERTIES_INCLUDE */
    public static final String CLOUD_PROPERTIES_INCLUDE = "bootstrap.yml";

    /**
     * Execute *
     *
     * @since 1.0.0
     */
    @Override
    public void execute() {

        if (this.skip) {
            this.getLog().info("generate-assembly-config is skipped");
            return;
        }

        // 存在自定义打包配置则会被写入到 outputFile
        if (this.assemblyFile.exists()) {
            try {
                new FileWriter(this.outputFile).write(this.assemblyFile);
            } catch (IOException e) {
                this.getLog().error(e.getMessage(), e);
            }
        } else {
            ApplicationType applicationType = this.deduceFromDependencies();

            String include = BOOT_PROPERTIES_INCLUDE;
            if (applicationType == ApplicationType.CLOUD) {
                include = CLOUD_PROPERTIES_INCLUDE;
            }

            String dependencesExclude;

            String repackageSkip = this.project.getProperties().getProperty(Plugins.SKIP_JAR_REPACKAGE);

            if (repackageSkip == null) {
                // 未通过 JVM 手动设置, 默认就跳过
                dependencesExclude = DEFAULT_DEPENDENCES_EXCLUDES;
            } else if (!Boolean.parseBoolean(repackageSkip)) {
                // 如果手动设置开启, skip 应该为 false
                dependencesExclude = FileUtils.readToString(ASSEMBLY_EXCLUDES_FILE_NAME);
            } else {
                // 如果设置为 true
                dependencesExclude = DEFAULT_DEPENDENCES_EXCLUDES;
            }

            Map<String, String> replaceMap = new HashMap<>(2);
            replaceMap.put(PROPERTIES_INCLUDE, include);
            replaceMap.put(DEPENDENCES_EXCLUDES, dependencesExclude);
            new FileWriter(this.outputFile, replaceMap).write(ASSEMBLY_FILE_NAME);
        }

        this.buildContext.refresh(this.outputFile);
    }


    /**
     * 从当前项目的依赖判断是 boot 还是 cloud 应用
     *
     * @return the application type
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    private ApplicationType deduceFromDependencies() {
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
