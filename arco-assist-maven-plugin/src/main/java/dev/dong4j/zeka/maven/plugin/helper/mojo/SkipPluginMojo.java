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
 * <p>Description: 在 validate 阶段根据当前模块类型禁用部分插件 </p>
 *
 * @author dong4j
 * @version 1.0.3
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
            System.setProperty(this.getProject().getModel().getArtifactId() + "_START_CLASS", javaFile.getClassName());
            // 生成 build-info.properties
            this.defineProperty(Plugins.SKIP_BUILD_INFO, Plugins.TURN_ON_PLUGIN);
            // 创建 profiles 文件
            this.defineProperty(Plugins.SKIP_BUILD_ACTIVE_FILE, Plugins.TURN_ON_PLUGIN);
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
        } else {
            this.skipPluginViCommandLine(Plugins.SKIP_GITCOMMITID, "skip git-commit-id-plugin");
        }
    }

    /**
     * Sets module type *
     *
     * @return the module type
     * @since 1.5.0
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
     * @since 1.5.0
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

