package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import lombok.SneakyThrows;
import org.apache.maven.model.Model;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 启动类主类属性管理Mojo，在validate阶段将解析到的主类注入到Maven属性
 * <p>
 * 该Mojo在Maven编译阶段执行，主要负责检测和管理Spring Boot应用的启动类
 * 自动识别被@SpringBootApplication或@EnableAutoConfiguration标识的主类
 * 将启动类信息注入到Maven属性中，便于其他插件使用
 * <p>
 * 主要特性：
 * - 自动检测：自动扫描和识别Spring Boot启动类
 * - 属性注入：将启动类名称注入到start.class属性
 * - 部署配置：为部署模块自动设置部署相关属性
 * - 重复检查：检查并警告重复的手动配置
 * - 条件执行：支持通过参数控制是否执行
 * <p>
 * 自动设置的属性：
 * - <b>start.class</b>：启动类的全限定名，供 maven-jar-plugin 或 spring-boot-maven-plugin 使用
 * - <b>maven.install.skip</b>：设置为true，跳过install阶段（部署模块不需要安装到本地仓库）
 * - <b>maven.deploy.skip</b>：设置为true，跳过deploy阶段（部署模块不需要发布到远程仓库）
 * <p>
 * 工作流程：
 * 1. <b>启动类获取</b>：从 SkipPluginMojo 中获取已检测的启动类信息
 * 2. <b>属性检查</b>：检查相关属性是否已经手动配置
 * 3. <b>属性注入</b>：如果未配置，自动注入相关属性
 * 4. <b>日志输出</b>：输出相关的信息和警告日志
 * <p>
 * 检测条件：
 * - 存在被 @SpringBootApplication 标识的类
 * - 或者存在被 @EnableAutoConfiguration 标识的类
 * - 且该类包含main方法
 * <p>
 * 使用场景：
 * - Spring Boot应用的自动化打包配置
 * - 多模块项目中的部署模块识别
 * - CI/CD流水线中的自动化构建
 * - Maven插件之间的属性传递
 * - 微服务项目的标准化构建
 * <p>
 * 注意事项：
 * - 必须在 SkipPluginMojo 之后执行，依赖其检测结果
 * - 不会覆盖已存在的手动配置
 * - 仅对被识别为部署模块的项目生效
 * <p>
 * 配置参数：
 * - skip：是否跳过该Mojo的执行（默认关闭）
 * - name：注入到Maven环境变量的key（默认为start.class）
 * <p>
 * 执行配置：
 * - 默认阶段：compile（编译阶段）
 * - 目标名称：mainclass-property
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
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
     * 1. 将 main class 绑定到 ${start.class} 配置上, 直接使用 ${start.class} 即可获取到 main class, maven-jar-plugin 或 spring-boot-maven-plugin 直接使用 ${start.class} 启动类;
     * 2. 检查打包插件配置是否正确;
     *
     * @since 1.0.0
     * @see SkipPluginMojo#execute()
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

        // 在 SkipPluginMojo 中会检测启动类, 然后注入到环境变量中, 这里直接获取即可
        String startClassName = System.getProperty(this.getProject().getModel().getArtifactId() + Plugins.START_CLASS_SUFFIX);
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

