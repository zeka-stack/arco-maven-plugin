package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.InstallationException;
import com.github.eirslett.maven.plugins.frontend.lib.NPMInstaller;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.SettingsDecrypter;

/**
 * Node.js 和 NPM 安装 Maven 插件
 * <p>
 * 该插件负责自动下载和安装指定版本的 Node.js 和 NPM 环境，为前端构建提供基础运行时支持。
 * 支持自定义下载源、代理配置和认证设置，默认使用淘宝镜像加速下载。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.0.0
 */
@Mojo(name = "install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndNpmMojo extends AbstractFrontendMojo {

    /** Node.js 二进制文件下载根地址，默认使用淘宝镜像 */
    @Parameter(property = "nodeDownloadRoot", required = false, defaultValue = "https://npm.taobao.org/mirrors/node/")
    private String nodeDownloadRoot;

    /** NPM 二进制文件下载根地址，默认使用淘宝镜像 */
    @Parameter(property = "npmDownloadRoot", required = false, defaultValue = "https://registry.npm.taobao.org/npm/-/")
    private String npmDownloadRoot;

    /** 下载根地址（已废弃），推荐使用 nodeDownloadRoot 和 npmDownloadRoot */
    @Parameter(property = "downloadRoot", required = false, defaultValue = "")
    @Deprecated
    private String downloadRoot;

    /** 要安装的 Node.js 版本号，版本名通常以 'v' 开头 */
    @Parameter(property = "nodeVersion", required = false, defaultValue = "v12.3.1")
    private String nodeVersion;

    /** 要安装的 NPM 版本号 */
    @Parameter(property = "npmVersion", required = false, defaultValue = "provided")
    private String npmVersion;

    /** 下载认证服务器 ID */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    /** Maven 执行会话 */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /** 是否跳过插件执行 */
    @Parameter(property = "skip.installnodenpm", defaultValue = "${skip.installnodenpm}")
    private boolean skip;

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
     * 执行 Node.js 和 NPM 的安装操作
     * <p>
     * 该方法会根据配置参数下载并安装指定版本的 Node.js 和 NPM：
     * 1. 获取代理配置和认证信息
     * 2. 确定下载源地址
     * 3. 创建 Node.js 安装器并配置参数
     * 4. 创建 NPM 安装器并配置参数
     * 5. 依次执行安装操作
     *
     * @param factory 前端插件工厂，用于创建安装器实例
     * @throws InstallationException 安装过程中出现错误时抛出
     * @since 1.0.0
     */
    @Override
    public void execute(FrontendPluginFactory factory) throws InstallationException {
        ProxyConfig proxyConfig = MojoUtils.getProxyConfig(this.session, this.decrypter);
        String nodeRoot = this.getNodeDownloadRoot();
        String npmRoot = this.getNpmDownloadRoot();
        Server server = MojoUtils.decryptServer(this.serverId, this.session, this.decrypter);
        if (null != server) {
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(this.nodeVersion)
                .setNodeDownloadRoot(nodeRoot)
                .setNpmVersion(this.npmVersion)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .install();
            factory.getNPMInstaller(proxyConfig)
                .setNodeVersion(this.nodeVersion)
                .setNpmVersion(this.npmVersion)
                .setNpmDownloadRoot(npmRoot)
                .setUserName(server.getUsername())
                .setPassword(server.getPassword())
                .install();
        } else {
            factory.getNodeInstaller(proxyConfig)
                .setNodeVersion(this.nodeVersion)
                .setNodeDownloadRoot(nodeRoot)
                .setNpmVersion(this.npmVersion)
                .install();
            factory.getNPMInstaller(proxyConfig)
                .setNodeVersion(this.nodeVersion)
                .setNpmVersion(this.npmVersion)
                .setNpmDownloadRoot(npmRoot)
                .install();
        }
    }

    /**
     * 获取 Node.js 下载根地址，优先使用新配置，兼容旧配置
     *
     * @return Node.js 下载根地址
     * @since 1.0.0
     */
    @SuppressWarnings("java:S1874")
    private String getNodeDownloadRoot() {
        if (this.downloadRoot != null && !"".equals(this.downloadRoot) && this.nodeDownloadRoot == null) {
            return this.downloadRoot;
        }
        return this.nodeDownloadRoot;
    }

    /**
     * 获取 NPM 下载根地址，优先使用新配置，兼容旧配置
     *
     * @return NPM 下载根地址
     * @since 1.0.0
     */
    @SuppressWarnings("java:S1874")
    private String getNpmDownloadRoot() {
        if (this.downloadRoot != null && !"".equals(this.downloadRoot) && NPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT.equals(this.npmDownloadRoot)) {
            return this.downloadRoot;
        }
        return this.npmDownloadRoot;
    }
}
