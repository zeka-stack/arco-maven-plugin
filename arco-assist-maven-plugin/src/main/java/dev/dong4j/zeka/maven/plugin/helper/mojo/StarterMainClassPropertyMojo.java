package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import lombok.SneakyThrows;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>Description: 在 validate 阶段将解析到的 main class 注入到 name </p>
 *
 * @author dong4j
 * @version 1.0.3
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@Mojo(name = "mainclass-property", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class StarterMainClassPropertyMojo extends ZekaMavenPluginAbstractMojo {

    /** 部署包忽略 install 命令 */
    private static final String INSTALL_SKIP = "maven.install.skip";
    /** 部署包忽略 deploy 命令 */
    private static final String DEPLOY_SKIP = "maven.deploy.skip";
    /** START_CLASS */
    private static final String START_CLASS = "start.class";

    /** 注入到 maven 环境变量的 key = start.class */
    @Parameter(required = true, defaultValue = START_CLASS)
    private String name;

    /** Skip */
    @Parameter(property = Plugins.SKIP_BUILD_MAINCLASS_PROPERTY, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /**
     * 检查当前模块是否为可部署包 (是否存在 被 @SpringBootApplication 或 @EnableAutoConfiguration 标识的类), 实现如下功能:
     * 1. 将 main class 绑定到 ${name} 配置上, 直接使用 ${name} 即可获取到 main class, 模块不需要再配置 start.class 配置;
     * 2. 检查打包插件配置是否正确;
     *
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        if (this.skip) {
            this.getLog().info("mainclass-property is skipped");
            return;
        }

        this.injectionProperties(INSTALL_SKIP,
            Plugins.TURN_OFF_PLUGIN,
            "监测到当前模块存在启动类, 自动设置: " + INSTALL_SKIP + "=true",
            "已监测到当前模块存在启动类, 不需要手动指定忽略 install 命令, 将自动忽略, 可以删除多余配置");

        this.injectionProperties(DEPLOY_SKIP,
            Plugins.TURN_OFF_PLUGIN,
            "监测到当前模块存在启动类, 自动设置: " + DEPLOY_SKIP + "=true",
            "已监测到当前模块存在启动类, 不需要手动指定忽略 deploy 命令, 将自动忽略, 可以删除多余配置");

        String startClassName = System.getProperty(this.getProject().getModel().getArtifactId() + "_START_CLASS");
        this.injectionProperties(this.name,
            startClassName,
            "监测到当前模块存在启动类, 自动设置: " + this.name + "=" + startClassName,
            "已监测到当前模块存在启动类, 不需要手动配置 " + this.name + "属性, 将自动注入, 可以删除多余配置");
    }

    /**
     * 自动注入部署包配置
     *
     * @param name        name
     * @param value       value
     * @param infoMessage info message
     * @param warnMessage warn message
     * @since 1.0.0
     */
    private void injectionProperties(String name, String value, String infoMessage, String warnMessage) {
        Model model = this.getProject().getModel();

        if (model.getProperties().get(name) != null) {
            this.getLog().info(warnMessage);
        } else {
            this.defineProperty(name, value);
            this.getLog().info(infoMessage);
        }
    }
}

