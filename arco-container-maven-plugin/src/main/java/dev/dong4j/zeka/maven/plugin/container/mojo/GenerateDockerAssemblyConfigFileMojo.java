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
 * Docker Assembly配置文件生成Mojo，用于为Docker容器化部署生成专用的Assembly配置
 * <p>
 * 该Mojo在Maven打包阶段执行，主要为Docker容器化部署生成专门的Assembly配置文件
 * 支持分层构建的lib.xml和app.xml配置，优化Docker镜像大小和构建效率
 * 针对Spring Boot和Spring Cloud应用提供不同的配置策略
 * <p>
 * 主要特性：
 * - 分层构建：生成分离的lib.xml和app.xml配置文件
 * - 智能检测：自动识别应用类型（Boot/Cloud）并选择合适配置
 * - 依赖优化：智能处理依赖排除和重打包规则
 * - 自定义支持：支持从自定义模板生成配置文件
 * - 模板替换：支持动态内容替换和参数化配置
 * <p>
 * 生成的文件类型：
 * <p>
 * <b>lib.xml</b> - 依赖库分层配置：
 * - 用于打包所有第三方依赖库
 * - 支持依赖排除和过滤规则
 * - 与JAR重打包配置联动
 * - 适合作为Docker基础层使用
 * <p>
 * <b>app.xml</b> - 应用程序分层配置：
 * - 用于打包应用程序本身和配置文件
 * - 根据应用类型选择不同的配置文件包含规则
 * - 支持Spring Boot和Cloud的不同配置策略
 * - 适合作为Docker应用层使用
 * <p>
 * 应用类型检测和配置：
 * - <b>Spring Boot应用</b>：包含 application*.yml 配置文件
 * - <b>Spring Cloud应用</b>：包含 bootstrap.yml 配置文件
 * <p>
 * 依赖处理策略：
 * - <b>默认排除</b>：自动排除当前项目和devtools依赖
 * - <b>重打包模式</b>：根据jar.repackage.skip设置动态调整排除规则
 * - <b>自定义排除</b>：支持从外部文件加载排除规则
 * <p>
 * Docker分层构建优势：
 * - <b>层缓存优化</b>：依赖库和应用程序分层，提高重建效率
 * - <b>镜像大小优化</b>：减少不必要的层传输和存储开销
 * - <b>部署灵活性</b>：支持热更新和分层部署
 * - <b>安全性提升</b>：减少攻击面和安全漏洞
 * <p>
 * 使用场景：
 * - Spring Boot/Cloud应用的Docker容器化部署
 * - 微服务架构的标准化镜像构建
 * - CI/CD流水线中的自动化打包
 * - Kubernetes环境的优化部署
 * - 多环境部署的镜像分层管理
 * <p>
 * 配置参数：
 * - skip：是否跳过该Mojo的执行（默认关闭）
 * - libOutputFile：依赖库Assembly配置文件输出路径
 * - appOutputFile：应用程序Assembly配置文件输出路径
 * - libAssemblyFile：自定义依赖库Assembly模板文件
 * - appAssemblyFile：自定义应用Assembly模板文件
 * <p>
 * 执行配置：
 * - 默认阶段：package（打包阶段）
 * - 目标名称：generate-docker-assembly-config
 * - 线程安全：支持
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
    public static final String DEFAULT_DEPENDENCES_EXCLUDES = """
        <excludes>
            <!-- 排除自己, 排除 devtools -->
            <exclude>${groupId}:${project.artifactId}</exclude>
        </excludes>""";
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
    @SuppressWarnings("D")
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
