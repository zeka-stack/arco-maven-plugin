package dev.dong4j.arco.maven.plugin.boot.loader.archive;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

/**
 * <p>Description: </p>
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
     * @since 2.2.0
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
