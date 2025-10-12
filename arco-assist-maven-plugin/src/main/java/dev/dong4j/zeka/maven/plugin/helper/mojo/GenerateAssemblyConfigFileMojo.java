package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Assembly打包配置文件动态生成Mojo，用于自动生成assembly.xml配置文件
 * <p>
 * 该Mojo在Maven打包阶段执行，根据项目类型和配置自动生成Assembly打包配置
 * 支持Spring Boot和Spring Cloud两种应用类型，自动适配不同的配置文件包含规则
 * 提供了灵活的依赖排除策略，能够根据是否启用JAR重打包动态调整
 * <p>
 * 主要特性：
 * - 自动检测：根据项目依赖自动识别应用类型（Boot/Cloud）
 * - 动态配置：根据不同应用类型生成不同的打包配置
 * - 依赖管理：智能处理依赖包含和排除规则
 * - 模板支持：支持从内置模板和自定义模板生成配置
 * - 条件执行：支持通过参数控制是否生成配置文件
 * <p>
 * 应用类型支持：
 * - <b>Spring Boot</b>：包含 application*.yml 配置文件
 * - <b>Spring Cloud</b>：包含 bootstrap.yml 配置文件
 * <p>
 * 依赖处理策略：
 * - 默认排除：自动排除自身和devtools依赖
 * - 智能排除：根据JAR重打包设置动态调整排除规则
 * - 自定义排除：支持从外部文件加载排除规则
 * <p>
 * 使用场景：
 * - Spring Boot/Cloud应用的自动打包配置
 * - 微服务分发包的统一构建
 * - 多环境部署包的自动生成
 * - CI/CD流水线中的标准化打包
 * - Docker镜像构建的前置步骤
 * <p>
 * 配置参数：
 * - skip：是否跳过此Mojo的执行（默认关闭）
 * - outputFile：生成的assembly.xml文件路径
 * - assemblyFile：自定义的assembly.xml模板文件
 * <p>
 * 执行配置：
 * - 默认阶段：package（打包阶段）
 * - 目标名称：generate-assembly-config
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 11:53
 * @since 1.0.0
 */
@Mojo(name = "generate-assembly-config", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateAssemblyConfigFileMojo extends ZekaMavenPluginAbstractMojo {

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
    public static final String DEFAULT_DEPENDENCES_EXCLUDES = """
        <excludes>
            <!-- 排除自己, 排除 devtools -->
            <exclude>${groupId}:${project.artifactId}</exclude>
        </excludes>""";
    /** PROPERTIES_INCLUDE */
    public static final String PROPERTIES_INCLUDE = "#{include}";

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

            String include = "*.yml";

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

}
