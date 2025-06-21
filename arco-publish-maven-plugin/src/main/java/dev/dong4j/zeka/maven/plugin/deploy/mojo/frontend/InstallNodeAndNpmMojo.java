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
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.7.0
 */
@Mojo(name = "install-node-and-npm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class InstallNodeAndNpmMojo extends AbstractFrontendMojo {

    /**
     * Where to download Node.js binary from. Defaults to https://nodejs.org/dist/
     */
    @Parameter(property = "nodeDownloadRoot", required = false, defaultValue = "https://npm.taobao.org/mirrors/node/")
    private String nodeDownloadRoot;

    /**
     * Where to download NPM binary from. Defaults to https://registry.npmjs.org/npm/-/
     */
    @Parameter(property = "npmDownloadRoot", required = false, defaultValue = "https://registry.npm.taobao.org/npm/-/")
    private String npmDownloadRoot;

    /**
     * Where to download Node.js and NPM binaries from.
     *
     * @deprecated use {@link #nodeDownloadRoot} and {@link #npmDownloadRoot} instead, this configuration will be used only when no
     * {@link #nodeDownloadRoot} or {@link #npmDownloadRoot} is specified.
     */
    @Parameter(property = "downloadRoot", required = false, defaultValue = "")
    @Deprecated
    private String downloadRoot;

    /**
     * The version of Node.js to install. IMPORTANT! Most Node.js version names start with 'v', for example 'v0.10.18'
     */
    @Parameter(property = "nodeVersion", required = false, defaultValue = "v12.3.1")
    private String nodeVersion;

    /**
     * The version of NPM to install.
     */
    @Parameter(property = "npmVersion", required = false, defaultValue = "provided")
    private String npmVersion;

    /**
     * Server Id for download username and password
     */
    @Parameter(property = "serverId", defaultValue = "")
    private String serverId;

    /** Session */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.installnodenpm", defaultValue = "${skip.installnodenpm}")
    private boolean skip;

    /** Decrypter */
    @Component(role = SettingsDecrypter.class)
    private SettingsDecrypter decrypter;

    /**
     * Skip execution
     *
     * @return the boolean
     * @since 1.7.0
     */
    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    /**
     * Execute
     *
     * @param factory factory
     * @throws InstallationException installation exception
     * @since 1.7.0
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
     * Gets node download root *
     *
     * @return the node download root
     * @since 1.7.0
     */
    @SuppressWarnings("java:S1874")
    private String getNodeDownloadRoot() {
        if (this.downloadRoot != null && !"".equals(this.downloadRoot) && this.nodeDownloadRoot == null) {
            return this.downloadRoot;
        }
        return this.nodeDownloadRoot;
    }

    /**
     * Gets npm download root *
     *
     * @return the npm download root
     * @since 1.7.0
     */
    @SuppressWarnings("java:S1874")
    private String getNpmDownloadRoot() {
        if (this.downloadRoot != null && !"".equals(this.downloadRoot) && NPMInstaller.DEFAULT_NPM_DOWNLOAD_ROOT.equals(this.npmDownloadRoot)) {
            return this.downloadRoot;
        }
        return this.npmDownloadRoot;
    }
}
