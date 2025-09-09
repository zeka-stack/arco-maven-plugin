package dev.dong4j.zeka.maven.plugin.boot.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

/**
 * 归档文件抽象接口，定义了JAR文件和目录归档的通用操作
 * <p>
 * 该接口为Spring Boot的类加载机制提供了统一的归档文件抽象
 * 支持JAR文件和解压目录两种归档格式的统一操作
 * 提供了获取URL、清单文件、嵌套归档等核心功能
 * <p>
 * 主要特性：
 * - 统一抽象：为JAR文件和目录提供统一的操作接口
 * - 迭代支持：实现Iterable接口，支持遍历归档中的所有条目
 * - 资源管理：实现AutoCloseable接口，支持自动资源释放
 * - URL支持：提供统一的URL获取机制
 * - 清单访问：支持访问归档的META-INF/MANIFEST.MF文件
 * - 嵌套归档：支持获取归档内的嵌套JAR文件
 * <p>
 * 实现类型：
 * - <b>JarFileArchive</b>：用于处理JAR文件归档
 * - <b>ExplodedArchive</b>：用于处理解压目录归档
 * <p>
 * 核心方法：
 * - <b>getUrl()</b>：获取归档的URL地址，用于类加载器
 * - <b>getManifest()</b>：获取MANIFEST.MF文件内容
 * - <b>getNestedArchives()</b>：获取嵌套的归档文件（如BOOT-INF/lib/*.jar）
 * <p>
 * 使用场景：
 * - Spring Boot应用的类路径扫描
 * - 动态类加载和热部署
 * - JAR文件内部结构分析
 * - 微服务打包和部署
 * - 插件化架构的资源加载
 * <p>
 * 内部接口：
 * - <b>Entry</b>：表示归档中的单个条目（文件或目录）
 * - <b>EntryFilter</b>：用于过滤归档条目的策略接口
 * <p>
 * 注意事项：
 * - 实现类需要处理线程安全性
 * - 资源使用后必须正确释放
 * - URL生成需要考虑跨平台兼容性
 * - 嵌套归档的递归处理需要防止死循环
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:49
 * @see JarFileArchive
 * @since 1.0.0
 */
public interface Archive extends Iterable<Archive.Entry>, AutoCloseable {

    /**
     * 获取该归档的 url
     *
     * @return the archive URL
     * @throws MalformedURLException if the URL is malformed
     * @since 1.0.0
     */
    URL getUrl() throws MalformedURLException;

    /**
     * 获取 jar!/META-INF/MANIFEST.MF 或 [ArchiveDir]/META-INF/MANIFEST.MF
     *
     * @return the manifest
     * @throws IOException if the manifest cannot be read
     * @since 1.0.0
     */
    Manifest getManifest() throws IOException;

    /**
     * 获取 jar!/BOOT-INF/lib/*.jar 或 [ArchiveDir]/BOOT-INF/lib/*.jar
     *
     * @param filter the filter used to limit entries
     * @return nested archives
     * @throws IOException if nested archives cannot be read
     * @since 1.0.0
     */
    List<Archive> getNestedArchives(EntryFilter filter) throws IOException;

    /**
     * Closes the {@code Archive}, releasing any open resources.
     *
     * @throws Exception if an error occurs during close processing
     * @since 1.0.0
     */
    @Override
    default void close() throws Exception {

    }

    /**
     * Represents a single entry in the archive.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    interface Entry {

        /**
         * Returns {@code true} if the entry represents a directory.
         *
         * @return if the entry is a directory
         * @since 1.0.0
         */
        boolean isDirectory();

        /**
         * Returns the name of the entry.
         *
         * @return the name of the entry
         * @since 1.0.0
         */
        String getName();

    }

    /**
     * Strategy interface to filter {@link Entry Entries}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    interface EntryFilter {

        /**
         * Apply the jar entry filter.
         *
         * @param entry the entry to filter
         * @return {@code true} if the filter matches
         * @since 1.0.0
         */
        boolean matches(Entry entry);

    }

}
