package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import dev.dong4j.arco.maven.plugin.boot.loader.data.RandomAccessData;

/**
 * Callback visitor triggered by {@link CentralDirectoryParser}.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
interface CentralDirectoryVisitor {

    /**
     * Visit start *
     *
     * @param endRecord            end record
     * @param centralDirectoryData central directory data
     * @since 1.0.0
     */
    void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData);

    /**
     * Visit file header *
     *
     * @param fileHeader file header
     * @param dataOffset data offset
     * @since 1.0.0
     */
    void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset);

    /**
     * Visit end
     *
     * @since 1.0.0
     */
    void visitEnd();

}
