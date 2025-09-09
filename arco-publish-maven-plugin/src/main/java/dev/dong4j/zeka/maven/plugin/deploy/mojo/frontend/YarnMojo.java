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
 * Yarn包管理器Maven插件，提供了在Maven构建过程中执行Yarn命令的能力
 * <p>
 * 该插件类封装了Yarn包管理器的核心功能，为现代前端开发流程提供更快、更可靠的依赖管理方案
 * 相比于npm，Yarn提供了更快的安装速度、更可靠的依赖解析和更安全的包管理
 * <p>
 * 核心功能特性：
 * <ul>
 *     <li>Yarn命令执行 - 支持执行任意Yarn命令，默认为安装依赖</li>
 *     <li>并行安装 - 利用Yarn的并行下载能力提升安装速度</li>
 *     <li>锁定文件 - 自动生成yarn.lock文件确保依赖版本一致性</li>
 *     <li>代理配置继承 - 自动继承Maven的代理设置给Yarn</li>
 *     <li>注册表覆盖 - 支持自定义npm注册表URL</li>
 * </ul>
 * <p>
 * Yarn特有优势：
 * <ul>
 *     <li>更快的安装速度 - 并行下载和缓存优化</li>
 *     <li>更可靠的依赖解析 - 精确的版本控制和锁定</li>
 *     <li>更安全的包验证 - 自动校验包的完整性</li>
 *     <li>更好的网络弹性 - 网络失败自动重试机制</li>
 * </ul>
 * <p>
 * 高级特性支持：
 * <ul>
 *     <li>代理自动配置 - 自动从 Maven settings.xml 读取代理配置</li>
 *     <li>加密设置解密 - 支持Maven加密密码的自动解密</li>
 *     <li>增量构建支持 - 集成Plexus增量构建上下文</li>
 *     <li>灵活的执行控制 - 支持通过参数跳过执行</li>
 *     <li>线程安全执行 - 支持多线程并发构建场景</li>
 * </ul>
 * <p>
 * 该插件特别适用于对依赖管理有高要求的项目
 * 在Maven的GENERATE_RESOURCES阶段执行，与Npm插件保持一致的执行时机
 * <p>
 * 配置示例：
 * <ul>
 *     <li>基本安装: yarn （默认）</li>
 *     <li>指定命令: yarn install --frozen-lockfile</li>
 *     <li>构建命令: yarn build</li>
 *     <li>跳过执行: -Dskip.yarn=true</li>
 * </ul>
 * <p>
 * 技术实现亮点：
 * <ul>
 *     <li>使用synchronized确保并发安全的执行</li>
 *     <li>支持yarn.lock文件的自动生成和管理</li>
 *     <li>智能的package.json变化检测和增量构建</li>
 *     <li>集成Yarn的网络弹性和错误恢复能力</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>大型前端项目中的高效依赖管理</li>
 *     <li>团队协作中的依赖版本一致性保证</li>
 *     <li>持续集成中的可重现构建流程</li>
 *     <li>对安装速度有高要求的开发环境</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.0.0
 */
@Mojo(name = "yarn", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class YarnMojo extends AbstractFrontendMojo {

    /** NPM_REGISTRY_URL */
    private static final String NPM_REGISTRY_URL = "npmRegistryURL";

    /**
     * npm arguments. Default is "install".
     */
    @Parameter(defaultValue = "", property = "frontend.yarn.arguments", required = false)
    private String arguments;

    /** Yarn inherits proxy config from maven */
    @Parameter(property = "frontend.yarn.yarnInheritsProxyConfigFromMaven", required = false,
        defaultValue = "true")
    private boolean yarnInheritsProxyConfigFromMaven;

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
    @Parameter(property = "skip.yarn", defaultValue = "${skip.yarn}")
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
        if (this.buildContext == null || this.buildContext.hasDelta(packageJson)
            || !this.buildContext.isIncremental()) {
            ProxyConfig proxyConfig = this.getProxyConfig();
            factory.getYarnRunner(proxyConfig, this.getRegistryUrl(), false).execute(this.arguments,
                this.environmentVariables);
        } else {
            this.getLog().info("Skipping yarn install as package.json unchanged");
        }
    }

    /**
     * Gets proxy config *
     *
     * @return the proxy config
     * @since 1.0.0
     */
    private ProxyConfig getProxyConfig() {
        if (this.yarnInheritsProxyConfigFromMaven) {
            return MojoUtils.getProxyConfig(this.session, this.decrypter);
        } else {
            this.getLog().info("yarn not inheriting proxy config from Maven");
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
