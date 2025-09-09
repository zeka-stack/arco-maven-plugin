package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import java.util.Collections;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;

/**
 * Bower 包管理工具 Maven 插件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 23:54
 * @since 1.0.0
 */
@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class BowerMojo extends AbstractFrontendMojo {

    /** Bower 命令参数，默认为 "install" */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /** 是否跳过插件执行 */
    @Parameter(property = "skip.bower", defaultValue = "${skip.bower}")
    private boolean skip;

    /** Maven 执行会话 */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /** Bower 是否继承 Maven 的代理配置 */
    @Parameter(property = "frontend.bower.bowerInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean bowerInheritsProxyConfigFromMaven;

    /** 设置解密器 */
    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * 判断是否跳过当前插件的执行
     *
     * @return true 如果需要跳过执行
     * @since 1.0.0
     */
    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    /**
     * 执行 Bower 命令进行包管理操作
     *
     * @param factory 前端插件工厂
     * @throws TaskRunnerException 任务执行失败时抛出
     * @since 1.0.0
     */
    @Override
    protected synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        ProxyConfig proxyConfig = this.getProxyConfig();
        factory.getBowerRunner(proxyConfig).execute(this.arguments, this.environmentVariables);
    }

    /**
     * 获取代理配置，可选继承 Maven 代理设置
     *
     * @return 代理配置对象
     * @since 1.0.0
     */
    private ProxyConfig getProxyConfig() {
        if (this.bowerInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(this.session, this.decrypter);
        } else {
            this.getLog().info("bower not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.emptyList());
        }
    }

}
