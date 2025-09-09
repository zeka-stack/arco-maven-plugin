package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.ProxyConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

/**
 * Maven 插件工具类，提供代理配置、服务器解密和执行条件判断等实用功能
 * <p>
 * 该工具类封装了前端构建插件常用的工具方法，包括 Maven 代理配置的获取和解密、
 * 服务器认证信息的处理、增量构建的条件判断等。支持从 Maven 设置中
 * 自动读取代理配置，并处理加密密码的解密操作。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.0.0
 */
@UtilityClass
class MojoUtils {

    /** 日志记录器 */
    private static final Logger LOGGER = LoggerFactory.getLogger(MojoUtils.class);

    /**
     * 将异常转换为 Maven 插件失败异常
     *
     * @param <E> 异常类型参数
     * @param e   原始异常
     * @return Maven 插件失败异常
     * @since 1.0.0
     */
    static <E extends Throwable> MojoFailureException toMojoFailureException(E e) {
        String causeMessage = e.getCause() != null ? ": " + e.getCause().getMessage() : "";
        return new MojoFailureException(e.getMessage() + causeMessage, e);
    }

    /**
     * 获取 Maven 代理配置，包括从设置中读取活跃代理并自动解密
     * <p>
     * 该方法从 Maven 会话中获取配置的代理列表，过滤出活跃的代理设置，
     * 并使用提供的解密器对加密的用户名和密码进行解密处理。
     * 支持多个代理配置同时生效。
     *
     * @param mavenSession Maven 执行会话
     * @param decrypter    设置解密器
     * @return 代理配置对象
     * @since 1.0.0
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
     * 解密代理设置中的加密信息
     *
     * @param proxy     代理对象
     * @param decrypter 设置解密器
     * @return 解密后的代理对象
     * @since 1.0.0
     */
    private static Proxy decryptProxy(Proxy proxy, SettingsDecrypter decrypter) {
        DefaultSettingsDecryptionRequest decryptionRequest = new DefaultSettingsDecryptionRequest(proxy);
        SettingsDecryptionResult decryptedResult = decrypter.decrypt(decryptionRequest);
        return decryptedResult.getProxy();
    }

    /**
     * 从 Maven 设置中获取并解密指定的服务器配置
     *
     * @param serverId     服务器 ID
     * @param mavenSession Maven 执行会话
     * @param decrypter    设置解密器
     * @return 解密后的服务器对象，找不到时返回 null
     * @since 1.0.0
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
     * 判断是否应该执行构建任务，支持增量构建和文件变更检测
     * <p>
     * 该方法综合考虑多个因素来决定是否执行构建：
     * 1. 非增量构建时始终执行
     * 2. 触发文件发生变更时执行
     * 3. 源目录中有文件变更时执行
     *
     * @param buildContext 构建上下文
     * @param triggerfiles 触发文件列表
     * @param srcdir       源目录
     * @return true 如果应该执行构建
     * @since 1.0.0
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
