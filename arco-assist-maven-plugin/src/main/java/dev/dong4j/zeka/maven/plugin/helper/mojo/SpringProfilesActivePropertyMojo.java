package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Spring Profile活跃属性管理Mojo，在validate阶段将Maven Profile写入指定文件
 * <p>
 * 该Mojo在Maven源码生成阶段执行，主要负责管理Spring Boot应用的Profile配置
 * 将Maven构建时的Profile信息传递给Spring应用，实现构建环境和运行环境的一致性
 * 支持自动初始化和环境切换，提供灵活的开发和部署支持
 * <p>
 * 主要特性：
 * - 环境一致性：确保Maven和Spring的环境配置一致
 * - 自动初始化：首次构建时自动创建默认配置（local）
 * - 非破坏性：不会覆盖已存在的手动配置
 * - 属性注入：自动向Maven环境注入profile.active属性
 * - 灵活配置：支持通过修改文件切换环境
 * <p>
 * 工作流程：
 * 1. <b>检查文件存在</b>：判断 spring.profiles.active 文件是否存在
 * 2. <b>创建默认配置</b>：如果文件不存在，创建并写入"local"
 * 3. <b>属性注入</b>：检查Maven属性中的profile.active配置
 * 4. <b>属性设置</b>：如果属性为空，自动设置为"local"
 * <p>
 * 环境切换方式：
 * - <b>修改文件</b>：直接修改 spring.profiles.active 文件内容
 * - <b>Maven参数</b>：使用 -Dprofile.active=xxx 参数
 * - <b>重置环境</b>：使用 mvn clean 清理，下次构建自动初始化为local
 * <p>
 * 支持的环境配置：
 * - <b>local</b>：本地开发环境（默认）
 * - <b>dev</b>：开发测试环境
 * - <b>test</b>：测试环境
 * - <b>staging</b>：预生产环境
 * - <b>prod</b>：生产环境
 * <p>
 * 使用场景：
 * - Spring Boot应用的环境配置管理
 * - 开发、测试、生产环境的自动切换
 * - CI/CD流水线中的环境控制
 * - 微服务部署中的配置管理
 * - 多环境应用的配置统一管理
 * <p>
 * 配置参数：
 * - skip：是否跳过该Mojo的执行（默认关闭）
 * - outputFile：spring.profiles.active文件的输出路径
 * <p>
 * 执行配置：
 * - 默认阶段：generate-sources（源码生成阶段）
 * - 目标名称：profile-active-property
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "profile-active-property", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class SpringProfilesActivePropertyMojo extends ZekaMavenPluginAbstractMojo {
    /** LOCAL */
    public static final String LOCAL = "local";
    /** PROFILE_ACTIVE */
    public static final String PROFILE_ACTIVE = "profile.active";

    /** Skip */
    @Parameter(property = Plugins.SKIP_BUILD_ACTIVE_FILE, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /** Output file */
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/profile/spring.profiles.active")
    private File outputFile;

    /**
     * 每次编译时, 如果 spring.profiles.active 不存在, 都会写入 local, 如果需要连接 dev 或 test 环境, 修改此配置即可, 如果需要重置, clean 即可.
     *
     * @throws MojoExecutionException mojo execution exception
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        if (this.skip) {
            this.getLog().info("profile-active-property is skipped");
            return;
        }

        try {
            // 不存在才新建, 避免重写已定义的 profile
            if (!this.outputFile.exists()) {
                new FileWriter(this.outputFile).writeContent(LOCAL);
                this.buildContext.refresh(this.outputFile);
            }

            if (StringUtils.isBlank(this.getProject().getProperties().getProperty(PROFILE_ACTIVE))) {
                // 兼容处理, 如果业务端写了 spring.profiles.active=${profile.active}, 将自动替换
                this.defineProperty(PROFILE_ACTIVE, LOCAL);
                this.getLog().debug(PROFILE_ACTIVE + " = " + this.getProject().getProperties().getProperty(PROFILE_ACTIVE));
            }
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}

