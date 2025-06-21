package dev.dong4j.arco.maven.plugin.boot.loader;

import dev.dong4j.arco.maven.plugin.boot.loader.archive.Archive;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * 可执行存档启动程序的基类
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:46
 * @since 1.0.0
 */
public abstract class ExecutableArchiveLauncher extends Launcher {

    /** Archive */
    private final Archive archive;

    /**
     * Executable archive launcher
     *
     * @since 1.0.0
     */
    protected ExecutableArchiveLauncher() {
        try {
            // 找到自己所在的jar, 并创建 Archive
            this.archive = this.createArchive();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Executable archive launcher
     *
     * @param archive archive
     * @since 1.0.0
     */
    protected ExecutableArchiveLauncher(Archive archive) {
        this.archive = archive;
    }

    /**
     * Gets archive *
     *
     * @return the archive
     * @since 1.0.0
     */
    @Contract(pure = true)
    protected final Archive getArchive() {
        return this.archive;
    }

    /**
     * Gets main class *
     *
     * @return the main class
     * @throws Exception exception
     * @since 1.0.0
     */
    @Override
    protected String getMainClass() throws Exception {
        Manifest manifest = this.archive.getManifest();
        String mainClass = null;
        if (manifest != null) {
            mainClass = manifest.getMainAttributes().getValue("Start-Class");
        }
        if (mainClass == null) {
            throw new IllegalStateException("No 'Start-Class' manifest entry specified in " + this);
        }
        return mainClass;
    }

    /**
     * Gets class path archives *
     *
     * @return the class path archives
     * @throws Exception exception
     * @since 1.0.0
     */
    @Override
    protected List<Archive> getClassPathArchives() throws Exception {
        // 获取 /BOOT-INF/lib 下所有 jar 及 /BOOT-INF/classes 目录对应的 archive
        List<Archive> archives = new ArrayList<>(this.archive.getNestedArchives(this::isNestedArchive));
        this.postProcessClassPathArchives(archives);
        return archives;
    }

    /**
     * 确定指定的 {@link JarEntry} 是否应添加到类路径的嵌套项, 对每个条目调用一次方法.
     *
     * @param entry the jar entry
     * @return {@code true} if the entry is a nested item (jar or folder)
     * @since 1.0.0
     */
    protected abstract boolean isNestedArchive(Archive.Entry entry);

    /**
     * Called to post-process archive entries before they are used. Implementations can
     * add and remove entries.
     *
     * @param archives the archives
     * @since 1.0.0
     */
    protected void postProcessClassPathArchives(List<Archive> archives) {
    }

}
