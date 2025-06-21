package dev.dong4j.arco.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import lombok.experimental.UtilityClass;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.build.incremental.BuildContext;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.7.0
 */
@UtilityClass
class MojoUtils {

    /** LOGGER */
    private static final Logger LOGGER = LoggerFactory.getLogger(MojoUtils.class);

    /**
     * To mojo failure exception
     *
     * @param <E> parameter
     * @param e   e
     * @return the mojo failure exception
     * @since 1.7.0
     */
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e) {
        String causeMessage = e.getCause() != null ? ": " + e.getCause().getMessage() : "";
        return new MojoFailureException(e.getMessage() + causeMessage, e);
    }

    /**
     * Gets proxy config *
     *
     * @param mavenSession maven session
     * @param decrypter    decrypter
     * @return the proxy config
     * @since 1.7.0
     */
    static ProxyConfig getProxyConfig(MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (mavenSession == null ||
            mavenSession.getSettings() == null ||
            mavenSession.getSettings().getProxies() == null ||
            mavenSession.getSettings().getProxies().isEmpty()) {
            return new ProxyConfig(Collections.emptyList());
        } else {
            List<Proxy> mavenProxies = mavenSession.getSettings().getProxies();

            List<ProxyConfig.Proxy> proxies = new ArrayList<>(mavenProxies.size());

            for (Proxy mavenProxy : mavenProxies) {
                if (mavenProxy.isActive()) {
                    mavenProxy = decryptProxy(mavenProxy, decrypter);
                    proxies.add(new ProxyConfig.Proxy(mavenProxy.getId(), mavenProxy.getProtocol(), mavenProxy.getHost(),
                        mavenProxy.getPort(), mavenProxy.getUsername(), mavenProxy.getPassword(),
                        mavenProxy.getNonProxyHosts()));
                }
            }

            LOGGER.info("Found proxies: [{}]", proxies);
            return new ProxyConfig(proxies);
        }
    }

    /**
     * Decrypt proxy
     *
     * @param proxy     proxy
     * @param decrypter decrypter
     * @return the proxy
     * @since 1.7.0
     */
    private static Proxy decryptProxy(Proxy proxy, SettingsDecrypter decrypter) {
        DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(proxy);
        SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
        return decryptedResult.getProxy();
    }

    /**
     * Decrypt server
     *
     * @param serverId     server id
     * @param mavenSession maven session
     * @param decrypter    decrypter
     * @return the server
     * @since 1.7.0
     */
    static Server decryptServer(String serverId, MavenSession mavenSession, SettingsDecrypter decrypter) {
        if (StringUtils.isEmpty(serverId)) {
            return null;
        }
        Server server = mavenSession.getSettings().getServer(serverId);
        if (server != null) {
            DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(server);
            SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
            return decryptedResult.getServer();
        } else {
            LOGGER.warn("Could not find server [{}] in settings.xml", serverId);
            return null;
        }
    }

    /**
     * Should execute
     *
     * @param buildContext build context
     * @param triggerfiles triggerfiles
     * @param srcdir       srcdir
     * @return the boolean
     * @since 1.7.0
     */
    static boolean shouldExecute(BuildContext buildContext, List<File> triggerfiles, File srcdir) {

        // If there is no buildContext, or this is not an incremental build, always execute.
        if (buildContext == null || !buildContext.isIncremental()) {
            return true;
        }

        if (triggerfiles != null) {
            for (File triggerfile : triggerfiles) {
                if (buildContext.hasDelta(triggerfile)) {
                    return true;
                }
            }
        }

        if (srcdir == null) {
            return true;
        }

        // Check for changes in the srcdir
        Scanner scanner = buildContext.newScanner(srcdir);
        scanner.scan();
        String[] includedFiles = scanner.getIncludedFiles();
        return (includedFiles != null && includedFiles.length > 0);
    }
}
