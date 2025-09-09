package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import java.io.File;
import java.util.Collections;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * NPM包管理器Maven插件，提供了在Maven构建过程中执行npm命令的能力
 * <p>
 * 该插件类封装了npm包管理器的核心功能，使得Java项目能够无缝集成前端依赖管理和构建流程
 * 通过Maven的标准生命周期管理，实现前端资源的自动化下载、安装和管理
 * <p>
 * 核心功能特性：
 * <ul>
 *     <li>npm命令执行 - 支持执行任意npm命令，默认为install</li>
 *     <li>代理配置继承 - 自动继承Maven的代理设置给npm</li>
 *     <li>注册表覆盖 - 支持自定义npm注册表URL</li>
 *     <li>增量构建 - 智能检测package.json变化，跳过不必要的安装</li>
 *     <li>并发安全 - 支持多线程并发构建的线程安全执行</li>
 * </ul>
 * <p>
 * 高级特性支持：
 * <ul>
 *     <li>代理自动配置 - 自动从 Maven settings.xml 读取代理配置</li>
 *     <li>加密设置解密 - 支持Maven加密密码的自动解密</li>
 *     <li>增量构建支持 - 集成Plexus增量构建上下文</li>
 *     <li>灵活的执行控制 - 支持通过参数跳过执行</li>
 * </ul>
 * <p>
 * 该插件特别适用于前后端分离架构中的前端资源管理
 * 在Maven的GENERATE_RESOURCES阶段自动执行，确保前端依赖在打包前安装完成
 * <p>
 * 配置示例：
 * <ul>
 *     <li>默认执行: npm install</li>
 *     <li>自定义命令: npm run build</li>
 *     <li>指定注册表: -DnpmRegistryURL=https://registry.npm.taobao.org</li>
 *     <li>跳过执行: -Dskip.npm=true</li>
 * </ul>
 * <p>
 * 技术实现亮点：
 * <ul>
 *     <li>使用synchronized确保并发安全的执行</li>
 *     <li>集成Maven会话和设置解密器</li>
 *     <li>支持系统属性覆盖配置参数</li>
 *     <li>智能的增量构建和缓存管理</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>React、Vue、Angular等单页应用的依赖管理</li>
 *     <li>微服务项目中的前端资源构建</li>
 *     <li>持续集成中的自动化前端构建流程</li>
 *     <li>开发环境中的前端依赖安装和更新</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.0.0
 */
@Mojo(name = "npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class NpmMojo extends AbstractFrontendMojo {

    /** NPM_REGISTRY_URL */
    private static final String NPM_REGISTRY_URL = "npmRegistryURL";

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.npm.arguments", required = false)
    private String arguments;

    /** Npm inherits proxy config from maven */
    @Parameter(property = "frontend.npm.npmInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean npmInheritsProxyConfigFromMaven;

    /**
     * Registry override, passed as the registry option during npm install if set.
     */
    @Parameter(property = NPM_REGISTRY_URL, required = false, defaultValue = "")
    private String npmRegistryURL;

    /** Session */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /** Build context */
    @Component
    private BuildContext buildContext;

    /** Decrypter */
    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.npm", defaultValue = "${skip.npm}")
    private boolean skip;

    /**
     * Skip execution
     *
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    /**
     * Execute
     *
     * @param factory factory
     * @throws TaskRunnerException task runner exception
     * @since 1.0.0
     */
    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(this.workingDirectory, "package.json");
        if (this.buildContext == null || this.buildContext.hasDelta(packageJson) || !this.buildContext.isIncremental()) {
            ProxyConfig proxyConfig = this.getProxyConfig();
            factory.getNpmRunner(proxyConfig, this.getRegistryUrl()).execute(this.arguments, this.environmentVariables);
        } else {
            this.getLog().info("Skipping npm install as package.json unchanged");
        }
    }

    /**
     * Gets proxy config *
     *
     * @return the proxy config
     * @since 1.0.0
     */
    private ProxyConfig getProxyConfig() {
        if (this.npmInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(this.session, this.decrypter);
        } else {
            this.getLog().info("npm not inheriting proxy config from Maven");
            return new ProxyConfig(Collections.emptyList());
        }
    }

    /**
     * Gets registry url *
     *
     * @return the registry url
     * @since 1.0.0
     */
    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, this.npmRegistryURL);
    }
}
