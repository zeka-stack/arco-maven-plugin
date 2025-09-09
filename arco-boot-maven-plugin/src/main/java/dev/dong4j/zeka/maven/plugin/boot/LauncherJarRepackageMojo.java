package dev.dong4j.zeka.maven.plugin.boot;

import dev.dong4j.zeka.maven.plugin.boot.boost.BootSlotter;
import dev.dong4j.zeka.maven.plugin.boot.boost.Slotter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.enums.ModuleType;
import dev.dong4j.zeka.maven.plugin.common.util.PluginUtils;
import java.io.File;
import java.io.IOException;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

/**
 * JAR增强重打包Mojo，用于将普通JAR重新打包为支持插件化的增强启动JAR
 * <p>
 * 该Mojo在Maven打包阶段执行，对生成的JAR文件进行增强处理
 * 重写MANIFEST.MF文件中的Main-Class和Start-Class属性
 * 使其支持BootLauncher的插件化启动机制，实现热更新和插件加载
 * <p>
 * 主要特性：
 * - JAR增强：将普通Spring Boot JAR增强为支持插件的启动包
 * - 清单重写：修改MANIFEST.MF文件，指向BootLauncher启动器
 * - 目录创建：自动创建patch和plugin目录结构
 * - 条件执行：仅对部署模块执行增强操作
 * - 文件保护：自动备份原始JAR文件
 * <p>
 * 工作流程：
 * 1. <b>模块类型检查</b>：检查当前模块是否为部署模块
 * 2. <b>目录准备</b>：在构建目录下创建patch和plugin目录
 * 3. <b>文件备份</b>：将原始JAR文件重命名为.original后缀
 * 4. <b>JAR增强</b>：使用BootSlotter对JAR文件进行增强处理
 * 5. <b>生成增强JAR</b>：生成支持插件化的增强JAR文件
 * <p>
 * MANIFEST.MF修改内容：
 * - <b>Main-Class</b>：修改为 BootLauncher 的全限定名
 * - <b>Start-Class</b>：设置为原始的应用主类
 * - <b>Class-Path</b>：添加对patch和plugin目录的引用
 * <p>
 * 生成的目录结构：
 * <pre>
 * target/
 * ├── myapp.jar                # 增强后的JAR文件
 * ├── myapp.jar.original       # 原始JAR文件备份
 * ├── patch/                   # 补丁目录（空）
 * └── plugin/                  # 插件目录（空）
 * </pre>
 * <p>
 * 使用场景：
 * - 生产环境的热更新部署
 * - 插件化微服务架构
 * - 快速迭代和灰度发布
 * - A/B测试和功能开关
 * - 多租户SaaS应用的定制化
 * <p>
 * 配置参数：
 * - skip：是否跳过JAR重打包（默认关闭）
 * - sourceDir：原始JAR文件所在目录（默认为${project.build.directory}）
 * - sourceJar：原始JAR文件名称（默认为${project.build.finalName}.jar）
 * <p>
 * 执行配置：
 * - 默认阶段：package（打包阶段）
 * - 目标名称：jar-repackage
 * - 线程安全：支持
 * <p>
 * 注意事项：
 * - 仅对被识别为部署模块的项目有效
 * - 需要在spring-boot-maven-plugin之后执行
 * - 原始JAR文件会被重命名为.original后缀
 * - 生成的增强JAR可直接使用java -jar启动
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.29 23:32
 * @since 1.0.0
 */
@Mojo(name = "jar-repackage", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class LauncherJarRepackageMojo extends ZekaMavenPluginAbstractMojo {
    /** PATCH */
    public static final String PATCH = "patch";
    /** PLUGIN */
    public static final String PLUGIN = "plugin";
    /** 原本 JAR 所在文件夹 */
    @Parameter(property = "sourceDir", required = true, defaultValue = "${project.build.directory}")
    private File sourceDir;
    /** 原本 JAR 名称 */
    @Parameter(property = "sourceJar", required = true, defaultValue = "${project.build.finalName}.jar")
    private String sourceJar;
    /** Set this to 'true' to bypass artifact deploy */
    @Parameter(property = Plugins.SKIP_JAR_REPACKAGE, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /**
     * Execute *
     *
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {

        ModuleType moduleType = PluginUtils.moduleType();

        if (this.skip || !moduleType.equals(ModuleType.DELOPY)) {
            this.getLog().info("arco-boot-maven-plugin is skipped");
            return;
        }

        this.buildPathAndPluginDir();

        this.getLog().info("repackage " + this.sourceJar);

        try {
            File src = new File(this.sourceDir, this.sourceJar);
            File originalFile = new File(this.sourceDir, this.sourceJar + ".original");
            // 重命名
            this.renameFile(src, originalFile);
            File dest = new File(this.sourceDir, this.sourceJar);
            Slotter slotter = new BootSlotter();
            slotter.slot(originalFile, dest);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    /**
     * Build path and plugin dir
     *
     * @since 1.0.0
     */
    private void buildPathAndPluginDir() {
        this.mkdir(PATCH);
        this.mkdir(PLUGIN);
    }

    /**
     * Rename file *
     *
     * @param file file
     * @param dest dest
     * @since 1.0.0
     */
    private void renameFile(@NotNull File file, File dest) {
        if (!file.renameTo(dest)) {
            throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest + "'");
        }
    }

    /**
     * Mkdir *
     *
     * @param patch patch
     * @since 1.0.0
     */
    private void mkdir(String patch) {
        File file = new File(this.sourceDir.getAbsolutePath()
            + File.separator
            + patch);

        if (!file.exists() && file.mkdirs()) {
            this.getLog().debug("创建 " + file.getName());
        }
    }
}
