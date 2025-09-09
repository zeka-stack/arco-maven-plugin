package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.exception.NullAdditionalPropertyValueException;
import java.io.File;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 项目构建信息生成Mojo，用于自动生成build-info.properties文件
 * <p>
 * 该Mojo在Maven资源生成阶段执行，自动收集并生成项目的构建相关信息
 * 包括项目基本信息（groupId、artifactId、name、version）和构建时间戳
 * 支持自定义属性注入，生成的build-info.properties文件可被应用程序读取
 * <p>
 * 主要特性：
 * - 自动信息收集：自动收集Maven项目的基本信息
 * - 时间戳生成：记录精确的构建时间，使用中国时区格式
 * - 自定义属性：支持通过additionalProperties注入额外的构建信息
 * - 条件执行：支持通过参数控制是否生成构建信息文件
 * - 标准位置：生成的文件位于META-INF目录下，便于应用程序访问
 * <p>
 * 生成的构建信息包含：
 * - <b>build.group</b>：项目的groupId
 * - <b>build.artifact</b>：项目的artifactId
 * - <b>build.name</b>：项目的名称
 * - <b>build.version</b>：项目的版本号
 * - <b>build.time</b>：构建时间（yyyy-MM-dd HH:mm:ss格式）
 * - <b>build.xxx</b>：自定义属性（通过additionalProperties配置）
 * <p>
 * 使用场景：
 * - Spring Boot应用的构建信息展示
 * - 运行时获取应用版本和构建时间
 * - 监控系统中的应用版本追踪
 * - 部署环境中的版本验证
 * - API接口中的系统信息返回
 * <p>
 * 配置参数：
 * - skip：是否跳过构建信息生成（默认关闭）
 * - outputFile：生成的build-info.properties文件路径
 * - time：构建时间配置（默认为off）
 * - additionalProperties：自定义的额外属性Map
 * <p>
 * 执行配置：
 * - 默认阶段：generate-resources（资源生成阶段）
 * - 目标名称：generate-build-info
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 07:08
 * @since 1.0.0
 */
@Mojo(name = "generate-build-info", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class GenerateProjectBuildInfoMojo extends ZekaMavenPluginAbstractMojo {

    /**
     * The location of the generated build-info.properties.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/build-info.properties")
    private File outputFile;

    /**
     * The value used for the {@code build.time} property in a form suitable for
     * {@link Instant#parse(CharSequence)}. Defaults to {@code session.request.startTime}.
     * To disable the {@code build.time} property entirely, use {@code 'off'}.
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "off")
    private String time;

    /**
     * Set this to 'true' to bypass artifact deploy
     *
     * @since 2.4
     */
    @Parameter(property = Plugins.SKIP_BUILD_INFO, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /**
     * 自定义配置, 会写入到 build-info.properties
     */
    @Parameter
    private Map<String, String> additionalProperties;

    /**
     * Execute *
     *
     * @since 1.0.0
     */
    @SneakyThrows
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("generate-build-info is skipped");
            return;
        }
        try {
            Properties properties = new Properties();
            properties.put("build.group", this.project.getGroupId());
            properties.put("build.artifact", this.project.getArtifactId());
            properties.put("build.name", this.project.getName());
            properties.put("build.version", this.project.getVersion());

            SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            properties.put("build.time", dateTimeFormatter.format(new Date()));

            if (this.additionalProperties != null) {
                this.additionalProperties.forEach((name, value) -> properties.put("build." + name, value));
            }

            new FileWriter(this.outputFile).write(properties);
            this.buildContext.refresh(this.outputFile);
        } catch (NullAdditionalPropertyValueException ex) {
            throw new MojoFailureException("生成 build-info.properties 失败. " + ex.getMessage(), ex);
        } catch (Exception ex) {
            throw new MojoExecutionException(ex.getMessage(), ex);
        }
    }

}
