package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import dev.dong4j.zeka.maven.plugin.boot.loader.data.RandomAccessData;

import java.io.IOException;

/**
 * A ZIP File "End of central directory record" (EOCD).
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @see <a href="https://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
 * @since 1.0.0
 */
class CentralDirectoryEndRecord {

    /** MINIMUM_SIZE */
    private static final int MINIMUM_SIZE = 22;

    /** MAXIMUM_COMMENT_LENGTH */
    private static final int MAXIMUM_COMMENT_LENGTH = 0xFFFF;

    /** ZIP64_MAGICCOUNT */
    private static final int ZIP64_MAGICCOUNT = 0xFFFF;

    /** MAXIMUM_SIZE */
    private static final int MAXIMUM_SIZE = MINIMUM_SIZE + MAXIMUM_COMMENT_LENGTH;

    /** SIGNATURE */
    private static final int SIGNATURE = 0x06054b50;

    /** COMMENT_LENGTH_OFFSET */
    private static final int COMMENT_LENGTH_OFFSET = 20;

    /** READ_BLOCK_SIZE */
    private static final int READ_BLOCK_SIZE = 256;

    /** Zip 64 end */
    private final Zip64End zip64End;

    /** Block */
    private byte[] block;

    /** Offset */
    private int offset;

    /** Size */
    private int size;

    /**
     * Create a new {@link CentralDirectoryEndRecord} instance from the specified
     * {@link RandomAccessData}, searching backwards from the end until a valid block is
     * located.
     *
     * @param data the source data
     * @throws IOException in case of I/O errors
     * @since 1.0.0
     */
    CentralDirectoryEndRecord(RandomAccessData data) throws IOException {
        this.block = this.createBlockFromEndOfData(data, READ_BLOCK_SIZE);
        this.size = MINIMUM_SIZE;
        this.offset = this.block.length - this.size;
        while (!this.isValid()) {
            this.size++;
            if (this.size > this.block.length) {
                if (this.size >= MAXIMUM_SIZE || this.size > data.getSize()) {
                    throw new IOException(
                        "Unable to find ZIP central directory records after reading " + this.size + " bytes");
                }
                this.block = this.createBlockFromEndOfData(data, this.size + READ_BLOCK_SIZE);
            }
            this.offset = this.block.length - this.size;
        }
        int startOfCentralDirectoryEndRecord = (int) (data.getSize() - this.size);
        this.zip64End = this.isZip64() ? new Zip64End(data, startOfCentralDirectoryEndRecord) : null;
    }

    /**
     * Create block from end of data byte [ ]
     *
     * @param data data
     * @param size size
     * @return the byte [ ]
     * @throws IOException io exception
     * @since 1.0.0
     */
    private byte[] createBlockFromEndOfData(RandomAccessData data, int size) throws IOException {
        int length = (int) Math.min(data.getSize(), size);
        return data.read(data.getSize() - length, length);
    }

    /**
     * Is valid boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    private boolean isValid() {
        if (this.block.length < MINIMUM_SIZE || Bytes.littleEndianValue(this.block, this.offset, 4) != SIGNATURE) {
            return false;
        }
        // Total size must be the structure size + comment
        long commentLength = Bytes.littleEndianValue(this.block, this.offset + COMMENT_LENGTH_OFFSET, 2);
        return this.size == MINIMUM_SIZE + commentLength;
    }

    /**
     * Is zip 64 boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    private boolean isZip64() {
        return (int) Bytes.littleEndianValue(this.block, this.offset + 10, 2) == ZIP64_MAGICCOUNT;
    }

    /**
     * Returns the location in the data that the archive actually starts. For most files
     * the archive data will start at 0, however, it is possible to have prefixed bytes
     * (often used for startup scripts) at the beginning of the data.
     *
     * @param data the source data
     * @return the offset within the data where the archive begins
     * @since 1.0.0
     */
    long getStartOfArchive(RandomAccessData data) {
        long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
        long specifiedOffset = Bytes.littleEndianValue(this.block, this.offset + 16, 4);
        long zip64EndSize = (this.zip64End != null) ? this.zip64End.getSize() : 0L;
        int zip64LocSize = (this.zip64End != null) ? Zip64Locator.ZIP64_LOCSIZE : 0;
        long actualOffset = data.getSize() - this.size - length - zip64EndSize - zip64LocSize;
        return actualOffset - specifiedOffset;
    }

    /**
     * Return the bytes of the "Central directory" based on the offset indicated in this
     * record.
     *
     * @param data the source data
     * @return the central directory data
     * @since 1.0.0
     */
    RandomAccessData getCentralDirectory(RandomAccessData data) {
        if (this.zip64End != null) {
            return this.zip64End.getCentralDirectory(data);
        }
        long innerOffset = Bytes.littleEndianValue(this.block, this.offset + 16, 4);
        long length = Bytes.littleEndianValue(this.block, this.offset + 12, 4);
        return data.getSubsection(innerOffset, length);
    }

    /**
     * Return the number of ZIP entries in the file.
     *
     * @return the number of records in the zip
     * @since 1.0.0
     */
    int getNumberOfRecords() {
        if (this.zip64End != null) {
            return this.zip64End.getNumberOfRecords();
        }
        long numberOfRecords = Bytes.littleEndianValue(this.block, this.offset + 10, 2);
        return (int) numberOfRecords;
    }

    /**
     * Gets comment *
     *
     * @return the comment
     * @since 1.0.0
     */
    String getComment() {
        int commentLength = (int) Bytes.littleEndianValue(this.block, this.offset + COMMENT_LENGTH_OFFSET, 2);
        AsciiBytes comment = new AsciiBytes(this.block, this.offset + COMMENT_LENGTH_OFFSET + 2, commentLength);
        return comment.toString();
    }

    /**
     * A Zip64 end of central directory record.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter     4.3.14 of Zip64 specification</a>
     * @since 1.0.0
     */
    private static final class Zip64End {

        /** ZIP64_ENDTOT */
        private static final int ZIP64_ENDTOT = 32; // total number of entries

        /** ZIP64_ENDSIZ */
        private static final int ZIP64_ENDSIZ = 40; // central directory size in bytes

        /** ZIP64_ENDOFF */
        private static final int ZIP64_ENDOFF = 48; // offset of first CEN header

        /** Locator */
        private final Zip64Locator locator;

        /** Central directory offset */
        private final long centralDirectoryOffset;

        /** Central directory length */
        private final long centralDirectoryLength;

        /** Number of records */
        private final int numberOfRecords;

        /**
         * Zip 64 end
         *
         * @param data                      data
         * @param centratDirectoryEndOffset centrat directory end offset
         * @throws IOException io exception
         * @since 1.0.0
         */
        private Zip64End(RandomAccessData data, int centratDirectoryEndOffset) throws IOException {
            this(data, new Zip64Locator(data, centratDirectoryEndOffset));
        }

        /**
         * Zip 64 end
         *
         * @param data    data
         * @param locator locator
         * @throws IOException io exception
         * @since 1.0.0
         */
        private Zip64End(RandomAccessData data, Zip64Locator locator) throws IOException {
            this.locator = locator;
            byte[] block = data.read(locator.getZip64EndOffset(), 56);
            this.centralDirectoryOffset = Bytes.littleEndianValue(block, ZIP64_ENDOFF, 8);
            this.centralDirectoryLength = Bytes.littleEndianValue(block, ZIP64_ENDSIZ, 8);
            this.numberOfRecords = (int) Bytes.littleEndianValue(block, ZIP64_ENDTOT, 8);
        }

        /**
         * Return the size of this zip 64 end of central directory record.
         *
         * @return size of this zip 64 end of central directory record
         * @since 1.0.0
         */
        private long getSize() {
            return this.locator.getZip64EndSize();
        }

        /**
         * Return the bytes of the "Central directory" based on the offset indicated in
         * this record.
         *
         * @param data the source data
         * @return the central directory data
         * @since 1.0.0
         */
        private RandomAccessData getCentralDirectory(RandomAccessData data) {
            return data.getSubsection(this.centralDirectoryOffset, this.centralDirectoryLength);
        }

        /**
         * Return the number of entries in the zip64 archive.
         *
         * @return the number of records in the zip
         * @since 1.0.0
         */
        private int getNumberOfRecords() {
            return this.numberOfRecords;
        }

    }

    /**
     * A Zip64 end of central directory locator.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @see <a href="https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT">Chapter     4.3.15 of Zip64 specification</a>
     * @since 1.0.0
     */
    private static final class Zip64Locator {

        /** Zip 64 locsize */
        static final int ZIP64_LOCSIZE = 20; // locator size
        /** Zip 64 locoff */
        static final int ZIP64_LOCOFF = 8; // offset of zip64 end

        /** Zip 64 end offset */
        private final long zip64EndOffset;

        /** Offset */
        private final int offset;

        /**
         * Zip 64 locator
         *
         * @param data                      data
         * @param centralDirectoryEndOffset central directory end offset
         * @throws IOException io exception
         * @since 1.0.0
         */
        private Zip64Locator(RandomAccessData data, int centralDirectoryEndOffset) throws IOException {
            this.offset = centralDirectoryEndOffset - ZIP64_LOCSIZE;
            byte[] block = data.read(this.offset, ZIP64_LOCSIZE);
            this.zip64EndOffset = Bytes.littleEndianValue(block, ZIP64_LOCOFF, 8);
        }

        /**
         * Return the size of the zip 64 end record located by this zip64 end locator.
         *
         * @return size of the zip 64 end record located by this zip64 end locator
         * @since 1.0.0
         */
        private long getZip64EndSize() {
            return this.offset - this.zip64EndOffset;
        }

        /**
         * Return the offset to locate {@link Zip64End}.
         *
         * @return offset of the Zip64 end of central directory record
         * @since 1.0.0
         */
        private long getZip64EndOffset() {
            return this.zip64EndOffset;
        }

    }

}
