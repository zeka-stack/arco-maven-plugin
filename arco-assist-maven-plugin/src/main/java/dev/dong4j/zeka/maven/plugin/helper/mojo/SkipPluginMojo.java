package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.JavaFile;
import dev.dong4j.zeka.maven.plugin.common.JavaFileScanner;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.enums.ModuleType;
import dev.dong4j.zeka.maven.plugin.common.enums.PackageType;
import dev.dong4j.zeka.maven.plugin.common.util.PluginUtils;
import dev.dong4j.zeka.maven.plugin.helper.enums.CheckStylePlugin;
import lombok.SneakyThrows;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * Maven插件条件控制Mojo，在validate阶段根据模块类型动态禁用部分插件
 * <p>
 * 该Mojo在Maven验证阶段执行，自动检测当前模块的类型和特点
 * 根据模块类型（POM、空模块、部署模块、依赖模块）智能控制相关插件的启用和禁用
 * 优化构建效率，避免在不适合的模块中执行不必要的插件
 * <p>
 * 主要特性：
 * - 智能检测：自动检测模块类型和启动类存在
 * - 插件管理：根据模块类型动态禁用或启用插件
 * - 性能优化：避免在POM模块中执行代码质量检查
 * - 参数注入：为部署模块自动设置启动类参数
 * - 配置管理：统一管理各种构建插件的启用状态
 * <p>
 * 模块类型分类和处理策略：
 * <p>
 * <b>POM模块</b>（packaging=pom）：
 * - 禁用CheckStyle和PMD代码质量检查插件
 * - 禁用Assembly打包插件
 * - 禁用Git提交信息插件
 * <p>
 * <b>空模块</b>（无Java文件）：
 * - 同 POM模块处理策略
 * - 避免不必要的插件执行
 * <p>
 * <b>部署模块</b>（包含启动类）：
 * - 启用构建信息生成（build-info.properties）
 * - 启用Profile活跃文件创建
 * - 启用主类属性生成
 * - 启用Assembly配置文件生成
 * - 启用通用启动脚本生成
 * - 启用编译标识生成
 * - 启用Dockerfile生成
 * <p>
 * <b>依赖模块</b>（普通JAR模块）：
 * - 禁用Git提交信息插件
 * - 保持其他插件默认状态
 * <p>
 * 支持的插件类型：
 * - CheckStyle和PMD代码质量检查插件
 * - Maven Assembly打包插件
 * - Git Commit ID插件
 * - Spring Boot相关插件
 * - 自定义构建插件
 * <p>
 * 使用场景：
 * - 多模块项目中的构建优化
 * - CI/CD流水线中的性能提升
 * - 微服务项目的标准化构建
 * - 开发环境中的构建速度优化
 * - 企业级项目的插件管理
 * <p>
 * 执行配置：
 * - 默认阶段：validate（验证阶段）
 * - 目标名称：skip-plugin
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "skip-plugin", defaultPhase = LifecyclePhase.VALIDATE, threadSafe = true)
public class SkipPluginMojo extends ZekaMavenPluginAbstractMojo implements JavaFileScanner {

    /**
     * 如果是 pom 类型的模块或者没有 java 文件, 则不检查代码, 不生成 build.info.properties 文件
     *
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        ModuleType moduleType = this.setModuleType();

        if (moduleType.equals(ModuleType.POM) || moduleType.equals(ModuleType.EMPTY)) {
            this.getLog().info("当前模块 packaging 为 pom 或者不存在 java 文件, 忽略 checkstyle, pmd 插件");
            CheckStylePlugin.keys().forEach(this::removePlugin);
            // 忽略 checkstyle 插件
            this.skipPluginViCommandLine(Plugins.SKIP_CHECKSTYLE, "skip maven-checkstyle-plugin");
            // 忽略 pmd 检查
            this.skipPluginViCommandLine(Plugins.SKIP_PMD, "skip maven-pmd-plugin");
            // 忽略打包
            this.skipPluginViCommandLine(Plugins.SKIP_ASSEMBLY, "skip maven-assembly-plugin");
            // 忽略 生成 git.properties
            this.skipPluginViCommandLine(Plugins.SKIP_GITCOMMITID, "skip git-commit-id-plugin");
        } else if (moduleType.equals(ModuleType.DELOPY)) {
            // 是启动类模块则开启以下插件
            JavaFile javaFile = this.mainClass(this.project);
            System.setProperty(this.getProject().getModel().getArtifactId() + Plugins.START_CLASS_SUFFIX, javaFile.getClassName());
            // 生成 build-info.properties
            this.defineProperty(Plugins.SKIP_BUILD_INFO, Plugins.TURN_ON_PLUGIN);
            // 生成 start.class 属性
            this.defineProperty(Plugins.SKIP_BUILD_MAINCLASS_PROPERTY, Plugins.TURN_ON_PLUGIN);
            // 生成 assembly.xml 文件
            this.defineProperty(Plugins.SKIP_ASSEMBLY_CONFIG, Plugins.TURN_ON_PLUGIN);
            // 生成通用启动脚本
            this.defineProperty(Plugins.SKIP_LAUNCH_SCRIPT, Plugins.TURN_ON_PLUGIN);
            // 生成编译通过标识
            this.defineProperty(Plugins.SKIP_COMPILED_ID, Plugins.TURN_ON_PLUGIN);
            // 部署包增强 (手动开启 -Djar.repackage.skip=false)
            // this.defineProperty(Plugins.SKIP_JAR_REPACKAGE, Plugins.TURN_ON_PLUGIN);
            // 生成自解压的部署包 (手动开启 -Dmakeself.skip=false)
            // this.defineProperty(Plugins.SKIP_MAKESELF, Plugins.TURN_ON_PLUGIN);
            this.defineProperty(Plugins.SKIP_PUBLISH_SINGLE, Plugins.TURN_ON_PLUGIN);
            // 生成 dockerfile
            this.defineProperty(Plugins.SKIP_DOCKERFILE_SCRIPT, Plugins.TURN_ON_PLUGIN);
        } else {
            this.skipPluginViCommandLine(Plugins.SKIP_GITCOMMITID, "skip git-commit-id-plugin");
        }
    }

    /**
     * Sets module type *
     *
     * @return the module type
     * @since 1.0.0
     */
    private ModuleType setModuleType() {
        String packaging = this.getProject().getPackaging();

        ModuleType moduleType;
        if (PackageType.POM.name().equalsIgnoreCase(packaging)) {
            moduleType = ModuleType.POM;
        } else if (this.noJavaFile(this.getProject())) {
            moduleType = ModuleType.EMPTY;
        } else if (this.isDeployModel(this.project)) {
            moduleType = ModuleType.DELOPY;
        } else {
            moduleType = ModuleType.DEPEND;
        }

        PluginUtils.moduleType(moduleType);
        return moduleType;
    }

    /**
     * Skip plugin vi command line
     *
     * @param via     via
     * @param message message
     * @since 1.0.0
     */
    private void skipPluginViCommandLine(String via, String message) {
        // 忽略 生成 git.properties
        this.defineProperty(via, Plugins.TURN_OFF_PLUGIN);
        this.getLog().info(message);
    }

    /**
     * 移除指定的 plugin
     *
     * @param key key
     * @since 1.0.0
     */
    private void removePlugin(String key) {
        Model model = this.getProject().getModel();
        Plugin plugin = model.getBuild().getPluginsAsMap().get(key);

        if (plugin != null) {
            model.getBuild().removePlugin(plugin);
            this.getLog().info(model.getName() + " 模块 packaging = pom, 移除 " + key + " 插件");
        }
    }
}

