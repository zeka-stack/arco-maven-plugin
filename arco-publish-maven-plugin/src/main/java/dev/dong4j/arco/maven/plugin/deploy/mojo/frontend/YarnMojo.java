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
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.Collections;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.7.0
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
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        File packageJson = new File(this.workingDirectory, "package.json");
        if (this.buildContext == null || this.buildContext.hasDelta(packageJson)
            || !this.buildContext.isIncremental()) {
            ProxyConfig proxyConfig = this.getProxyConfig();
            factory.getYarnRunner(proxyConfig, this.getRegistryUrl()).execute(this.arguments,
                this.environmentVariables);
        } else {
            this.getLog().info("Skipping yarn install as package.json unchanged");
        }
    }

    /**
     * Gets proxy config *
     *
     * @return the proxy config
     * @since 1.7.0
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
     * @since 1.7.0
     */
    private String getRegistryUrl() {
        // check to see if overridden via `-D`, otherwise fallback to pom value
        return System.getProperty(NPM_REGISTRY_URL, this.npmRegistryURL);
    }
}
