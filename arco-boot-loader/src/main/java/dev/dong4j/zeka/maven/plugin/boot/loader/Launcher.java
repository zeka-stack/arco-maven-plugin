package dev.dong4j.zeka.maven.plugin.boot.loader;

import dev.dong4j.zeka.maven.plugin.boot.loader.archive.Archive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.ExplodedArchive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.JarFileArchive;
import dev.dong4j.zeka.maven.plugin.boot.loader.jar.CustomJarFile;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;

/**
 * 通过 {@link Archive#getNestedArchives} 方法找到 /BOOT-INF/lib 下所有 jar 及 / BOOT-INF/classes 目录所对应的 archive
 * 通过这些 archives 的 url 生成 LaunchedURLClassLoader, 并将其设置为线程上下文类加载器, 从而启动应用.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:43
 * @since 1.3.0
 */
@SuppressWarnings("all")
public abstract class Launcher {

    /**
     * Launch the application. This method is the initial entry point that should be
     * called by a subclass {@code public static void main(String[] args)} method.
     *
     * @param args the incoming arguments
     * @throws Exception if the application fails to launch
     * @since 1.0.0
     */
    protected void launch(String[] args) throws Exception {
        CustomJarFile.registerUrlProtocolHandler();
        // 生成自定义 ClassLoader
        ClassLoader classLoader = this.createClassLoader(this.getClassPathArchives());
        // 启动应用
        this.launch(args, this.getMainClass(), classLoader);
    }

    /**
     * Create a classloader for the specified archives.
     *
     * @param archives the archives
     * @return the classloader
     * @throws Exception if the classloader cannot be created
     * @since 1.0.0
     */
    protected ClassLoader createClassLoader(@NotNull List<Archive> archives) throws Exception {
        List<URL> urls = new ArrayList<>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        return this.createClassLoader(urls.toArray(new URL[0]));
    }

    /**
     * 使用 LaunchedURLClassLoader 去加载 class 和 jar
     *
     * @param urls the URLs
     * @return the classloader
     * @since 1.0.0
     */
    protected ClassLoader createClassLoader(URL[] urls) {
        return new LaunchedURLClassLoader(urls, this.getClass().getClassLoader());
    }

    /**
     * Launch the application given the archive file and a fully configured classloader.
     *
     * @param args        the incoming arguments
     * @param mainClass   the main class to run
     * @param classLoader the classloader
     * @throws Exception if the launch fails
     * @since 1.0.0
     */
    protected void launch(String[] args, String mainClass, ClassLoader classLoader) throws Exception {
        // 将自定义 ClassLoader 设置为当前线程上下文类加载器
        Thread.currentThread().setContextClassLoader(classLoader);
        // 启动应用
        this.createMainMethodRunner(mainClass, args, classLoader).run();
    }

    /**
     * Create the {@code MainMethodRunner} used to launch the application.
     *
     * @param mainClass   the main class
     * @param args        the incoming arguments
     * @param classLoader the classloader
     * @return the main method runner
     * @since 1.0.0
     */
    protected MainMethodRunner createMainMethodRunner(String mainClass, String[] args, ClassLoader classLoader) {
        return new MainMethodRunner(classLoader, mainClass, args);
    }

    /**
     * Returns the main class that should be launched.
     *
     * @return the name of the main class
     * @throws Exception if the main class cannot be obtained
     * @since 1.0.0
     */
    protected abstract String getMainClass() throws Exception;

    /**
     * Returns the archives that will be used to construct the class path.
     *
     * @return the class path archives
     * @throws Exception if the class path archives cannot be obtained
     * @since 1.0.0
     */
    protected abstract List<Archive> getClassPathArchives() throws Exception;

    /**
     * 从一个类找到它的加载的位置
     *
     * @return the archive
     * @throws Exception exception
     * @since 1.0.0
     */
    protected final @NotNull Archive createArchive() throws Exception {
        ProtectionDomain protectionDomain = this.getClass().getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URI location = (codeSource != null) ? codeSource.getLocation().toURI() : null;
        String path = (location != null) ? location.getSchemeSpecificPart() : null;
        if (path == null) {
            throw new IllegalStateException("Unable to determine code source archive");
        }
        File root = new File(path);
        if (!root.exists()) {
            throw new IllegalStateException("Unable to determine code source archive from " + root);
        }
        return (root.isDirectory() ? new ExplodedArchive(root) : new JarFileArchive(root));
    }

}
