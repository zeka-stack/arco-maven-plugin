package dev.dong4j.zeka.maven.plugin.container.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.enums.ApplicationType;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>Description: 动态生成 docker assembly.xml </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 11:53
 * @since 1.0.0
 */
@Mojo(name = "generate-docker-assembly-config", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateDockerAssemblyConfigFileMojo extends ZekaMavenPluginAbstractMojo {

    /** 默认忽略此插件 */
    @Parameter(property = Plugins.SKIP_DOCKERFILE_SCRIPT, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;
    /** Output file */
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/assembly/lib.xml")
    private File libOutputFile;
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/assembly/app.xml")
    private File appOutputFile;
    /** 自定义的打包配置 */
    @Parameter(defaultValue = "${project.basedir}/assembly/lib.xml")
    private File libAssemblyFile;
    @Parameter(defaultValue = "${project.basedir}/assembly/app.xml")
    private File appAssemblyFile;

    /** ASSEMBLY_FILE_NAME */
    public static final String LIB_ASSEMBLY_FILE_NAME = "META-INF/assembly/lib.xml";
    public static final String APP_ASSEMBLY_FILE_NAME = "META-INF/assembly/app.xml";
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
            this.getLog().info("generate-docker-assembly-config is skipped");
            return;
        }

        // 存在自定义打包配置则会被写入到 outputFile
        if (this.libAssemblyFile.exists()) {
            try {
                new FileWriter(this.libOutputFile).write(this.libAssemblyFile);
            } catch (IOException e) {
                this.getLog().error(e.getMessage(), e);
            }
        } else {
            String repackageSkip = this.project.getProperties().getProperty(Plugins.SKIP_JAR_REPACKAGE);
            String dependencesExclude;
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
            replaceMap.put(DEPENDENCES_EXCLUDES, dependencesExclude);
            new FileWriter(this.libOutputFile, replaceMap).write(LIB_ASSEMBLY_FILE_NAME);
            this.getLog().info("生成 lib.xml: " + this.libOutputFile.getPath());
        }

        if (this.appOutputFile.exists()) {
            try {
                new FileWriter(this.appAssemblyFile).write(this.appOutputFile);
            } catch (IOException e) {
                this.getLog().error(e.getMessage(), e);
            }
        } else {
            ApplicationType applicationType = this.deduceFromDependencies();
            String include = BOOT_PROPERTIES_INCLUDE;
            if (applicationType == ApplicationType.CLOUD) {
                include = CLOUD_PROPERTIES_INCLUDE;
            }
            Map<String, String> replaceMap = new HashMap<>(2);
            replaceMap.put(PROPERTIES_INCLUDE, include);
            new FileWriter(this.appOutputFile, replaceMap).write(APP_ASSEMBLY_FILE_NAME);
            this.getLog().info("生成 app.xml: " + this.appOutputFile.getPath());
        }

        this.buildContext.refresh(this.libOutputFile);
        this.buildContext.refresh(this.appOutputFile);
    }

}
