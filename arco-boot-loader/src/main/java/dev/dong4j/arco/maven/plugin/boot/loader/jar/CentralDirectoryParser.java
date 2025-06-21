package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import dev.dong4j.arco.maven.plugin.boot.loader.data.RandomAccessData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the central directory from a JAR file.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @see CentralDirectoryVisitor
 * @since 1.0.0
 */
class CentralDirectoryParser {

    /** CENTRAL_DIRECTORY_HEADER_BASE_SIZE */
    private static final int CENTRAL_DIRECTORY_HEADER_BASE_SIZE = 46;

    /** Visitors */
    private final List<CentralDirectoryVisitor> visitors = new ArrayList<>();

    /**
     * Add visitor t
     *
     * @param <T>     parameter
     * @param visitor visitor
     * @return the t
     * @since 1.0.0
     */
    <T extends CentralDirectoryVisitor> T addVisitor(T visitor) {
        this.visitors.add(visitor);
        return visitor;
    }

    /**
     * Parse the source data, triggering {@link CentralDirectoryVisitor visitors}.
     *
     * @param data            the source data
     * @param skipPrefixBytes if prefix bytes should be skipped
     * @return the actual archive data without any prefix bytes
     * @throws IOException on error
     * @since 1.0.0
     */
    RandomAccessData parse(RandomAccessData data, boolean skipPrefixBytes) throws IOException {
        CentralDirectoryEndRecord endRecord = new CentralDirectoryEndRecord(data);
        if (skipPrefixBytes) {
            data = this.getArchiveData(endRecord, data);
        }
        RandomAccessData centralDirectoryData = endRecord.getCentralDirectory(data);
        this.visitStart(endRecord, centralDirectoryData);
        this.parseEntries(endRecord, centralDirectoryData);
        this.visitEnd();
        return data;
    }

    /**
     * Parse entries *
     *
     * @param endRecord            end record
     * @param centralDirectoryData central directory data
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void parseEntries(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData)
        throws IOException {
        byte[] bytes = centralDirectoryData.read(0, centralDirectoryData.getSize());
        CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
        int dataOffset = 0;
        for (int i = 0; i < endRecord.getNumberOfRecords(); i++) {
            fileHeader.load(bytes, dataOffset, null, 0, null);
            this.visitFileHeader(dataOffset, fileHeader);
            dataOffset += CENTRAL_DIRECTORY_HEADER_BASE_SIZE + fileHeader.getName().length()
                + fileHeader.getComment().length() + fileHeader.getExtra().length;
        }
    }

    /**
     * Gets archive data *
     *
     * @param endRecord end record
     * @param data      data
     * @return the archive data
     * @since 1.0.0
     */
    private RandomAccessData getArchiveData(CentralDirectoryEndRecord endRecord, RandomAccessData data) {
        long offset = endRecord.getStartOfArchive(data);
        if (offset == 0) {
            return data;
        }
        return data.getSubsection(offset, data.getSize() - offset);
    }

    /**
     * Visit start *
     *
     * @param endRecord            end record
     * @param centralDirectoryData central directory data
     * @since 1.0.0
     */
    private void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
        for (CentralDirectoryVisitor visitor : this.visitors) {
            visitor.visitStart(endRecord, centralDirectoryData);
        }
    }

    /**
     * Visit file header *
     *
     * @param dataOffset data offset
     * @param fileHeader file header
     * @since 1.0.0
     */
    private void visitFileHeader(int dataOffset, CentralDirectoryFileHeader fileHeader) {
        for (CentralDirectoryVisitor visitor : this.visitors) {
            visitor.visitFileHeader(fileHeader, dataOffset);
        }
    }

    /**
     * Visit end
     *
     * @since 1.0.0
     */
    private void visitEnd() {
        for (CentralDirectoryVisitor visitor : this.visitors) {
            visitor.visitEnd();
        }
    }

}
