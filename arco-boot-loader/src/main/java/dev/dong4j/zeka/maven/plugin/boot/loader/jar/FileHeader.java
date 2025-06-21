package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import java.util.zip.ZipEntry;

/**
 * A file header record that has been loaded from a Jar file.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @see CustomJarEntry
 * @see CentralDirectoryFileHeader
 * @since 1.0.0
 */
interface FileHeader {

    /**
     * Returns {@code true} if the header has the given name.
     *
     * @param name   the name to test
     * @param suffix an additional suffix (or {@code 0})
     * @return {@code true} if the header has the given name
     * @since 1.0.0
     */
    boolean hasName(CharSequence name, char suffix);

    /**
     * Return the offset of the load file header within the archive data.
     *
     * @return the local header offset
     * @since 1.0.0
     */
    long getLocalHeaderOffset();

    /**
     * Return the compressed size of the entry.
     *
     * @return the compressed size.
     * @since 1.0.0
     */
    long getCompressedSize();

    /**
     * Return the uncompressed size of the entry.
     *
     * @return the uncompressed size.
     * @since 1.0.0
     */
    long getSize();

    /**
     * Return the method used to compress the data.
     *
     * @return the zip compression method
     * @see ZipEntry#STORED ZipEntry#STOREDZipEntry#STOREDZipEntry#STOREDZipEntry#STORED
     * @see ZipEntry#DEFLATED ZipEntry#DEFLATEDZipEntry#DEFLATEDZipEntry#DEFLATEDZipEntry#DEFLATED
     * @since 1.0.0
     */
    int getMethod();

}
