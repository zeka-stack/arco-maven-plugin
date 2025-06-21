package dev.dong4j.arco.maven.plugin.helper.mojo;

import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.FileWriter;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

/**
 * <p>Description: 在 validate 阶段将 maven 的 profile 写入到指定文件, 在应用启动时获取此配置 </p>
 *
 * @author dong4j
 * @version 1.0.3
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "profile-active-property", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class SpringProfilesActivePropertyMojo extends ArcoMavenPluginAbstractMojo {
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

