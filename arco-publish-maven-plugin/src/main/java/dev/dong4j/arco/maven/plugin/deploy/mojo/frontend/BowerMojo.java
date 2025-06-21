package dev.dong4j.arco.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.crypto.SettingsDecrypter;

import java.util.Collections;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 23:54
 * @since 1.7.0
 */
@Mojo(name = "bower", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class BowerMojo extends AbstractFrontendMojo {

    /**
     * Bower arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.bower", defaultValue = "${skip.bower}")
    private boolean skip;

    /** Session */
    @Parameter(property = "session", defaultValue = "${session}", readonly = true)
    private MavenSession session;

    /** Bower inherits proxy config from maven */
    @Parameter(property = "frontend.bower.bowerInheritsProxyConfigFromMaven", required = false, defaultValue = "true")
    private boolean bowerInheritsProxyConfigFromMaven;

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
     * @throws TaskRunnerException task runner exception
     * @since 1.7.0
     */
    @Override
    protected synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        ProxyConfig proxyConfig = this.getProxyConfig();
        factory.getBowerRunner(proxyConfig).execute(this.arguments, this.environmentVariables);
    }

    /**
     * Gets proxy config *
     *
     * @return the proxy config
     * @since 1.7.0
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
