package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import dev.dong4j.zeka.maven.plugin.boot.loader.data.RandomAccessData;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A ZIP File "Central directory file header record" (CDFH).
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @see <a href="https://en.wikipedia.org/wiki/Zip_%28file_format%29">Zip File Format</a>
 * @since 1.0.0
 */
final class CentralDirectoryFileHeader implements FileHeader {

    /** SLASH */
    private static final AsciiBytes SLASH = new AsciiBytes("/");

    /** NO_EXTRA */
    private static final byte[] NO_EXTRA = {};

    /** NO_COMMENT */
    private static final AsciiBytes NO_COMMENT = new AsciiBytes("");

    /** Header */
    private byte[] header;

    /** Header offset */
    private int headerOffset;

    /** Name */
    private AsciiBytes name;

    /** Extra */
    private byte[] extra;

    /** Comment */
    private AsciiBytes comment;

    /** Local header offset */
    private long localHeaderOffset;

    /**
     * Central directory file header
     *
     * @since 1.0.0
     */
    CentralDirectoryFileHeader() {
    }

    /**
     * Central directory file header
     *
     * @param header            header
     * @param headerOffset      header offset
     * @param name              name
     * @param extra             extra
     * @param comment           comment
     * @param localHeaderOffset local header offset
     * @since 1.0.0
     */
    CentralDirectoryFileHeader(byte[] header, int headerOffset, AsciiBytes name, byte[] extra, AsciiBytes comment,
                               long localHeaderOffset) {
        this.header = header;
        this.headerOffset = headerOffset;
        this.name = name;
        this.extra = extra;
        this.comment = comment;
        this.localHeaderOffset = localHeaderOffset;
    }

    /**
     * Load *
     *
     * @param data           data
     * @param dataOffset     data offset
     * @param variableData   variable data
     * @param variableOffset variable offset
     * @param filter         filter
     * @throws IOException io exception
     * @since 1.0.0
     */
    void load(byte[] data, int dataOffset, RandomAccessData variableData, int variableOffset, JarEntryFilter filter)
        throws IOException {
        // Load fixed part
        this.header = data;
        this.headerOffset = dataOffset;
        long nameLength = Bytes.littleEndianValue(data, dataOffset + 28, 2);
        long extraLength = Bytes.littleEndianValue(data, dataOffset + 30, 2);
        long commentLength = Bytes.littleEndianValue(data, dataOffset + 32, 2);
        this.localHeaderOffset = Bytes.littleEndianValue(data, dataOffset + 42, 4);
        // Load variable part
        dataOffset += 46;
        if (variableData != null) {
            data = variableData.read(variableOffset + 46L, nameLength + extraLength + commentLength);
            dataOffset = 0;
        }
        this.name = new AsciiBytes(data, dataOffset, (int) nameLength);
        if (filter != null) {
            this.name = filter.apply(this.name);
        }
        this.extra = NO_EXTRA;
        this.comment = NO_COMMENT;
        if (extraLength > 0) {
            this.extra = new byte[(int) extraLength];
            System.arraycopy(data, (int) (dataOffset + nameLength), this.extra, 0, this.extra.length);
        }
        if (commentLength > 0) {
            this.comment = new AsciiBytes(data, (int) (dataOffset + nameLength + extraLength), (int) commentLength);
        }
    }

    /**
     * Gets name *
     *
     * @return the name
     * @since 1.0.0
     */
    AsciiBytes getName() {
        return this.name;
    }

    /**
     * Has name boolean
     *
     * @param name   name
     * @param suffix suffix
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean hasName(CharSequence name, char suffix) {
        return this.name.matches(name, suffix);
    }

    /**
     * Is directory boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    boolean isDirectory() {
        return this.name.endsWith(SLASH);
    }

    /**
     * Gets method *
     *
     * @return the method
     * @since 1.0.0
     */
    @Override
    public int getMethod() {
        return (int) Bytes.littleEndianValue(this.header, this.headerOffset + 10, 2);
    }

    /**
     * Gets time *
     *
     * @return the time
     * @since 1.0.0
     */
    long getTime() {
        long datetime = Bytes.littleEndianValue(this.header, this.headerOffset + 12, 4);
        return this.decodeMsDosFormatDateTime(datetime);
    }

    /**
     * Decode MS-DOS Date Time details. See <a href=
     * "https://docs.microsoft.com/en-gb/windows/desktop/api/winbase/nf-winbase-dosdatetimetofiletime">
     * Microsoft's documentation</a> for more details of the format.
     *
     * @param datetime the date and time
     * @return the date and time as milliseconds since the epoch
     * @since 1.0.0
     */
    private long decodeMsDosFormatDateTime(long datetime) {
        LocalDateTime localDateTime = LocalDateTime.of((int) (((datetime >> 25) & 0x7f) + 1980),
            (int) ((datetime >> 21) & 0x0f), (int) ((datetime >> 16) & 0x1f),
            (int) ((datetime >> 11) & 0x1f),
            (int) ((datetime >> 5) & 0x3f), (int) ((datetime << 1) & 0x3e));
        return localDateTime.toEpochSecond(ZoneId.systemDefault().getRules().getOffset(localDateTime)) * 1000;
    }

    /**
     * Gets crc *
     *
     * @return the crc
     * @since 1.0.0
     */
    long getCrc() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 16, 4);
    }

    /**
     * Gets compressed size *
     *
     * @return the compressed size
     * @since 1.0.0
     */
    @Override
    public long getCompressedSize() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 20, 4);
    }

    /**
     * Gets size *
     *
     * @return the size
     * @since 1.0.0
     */
    @Override
    public long getSize() {
        return Bytes.littleEndianValue(this.header, this.headerOffset + 24, 4);
    }

    /**
     * Get extra byte [ ]
     *
     * @return the byte [ ]
     * @since 1.0.0
     */
    byte[] getExtra() {
        return this.extra;
    }

    /**
     * Has extra boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    boolean hasExtra() {
        return this.extra.length > 0;
    }

    /**
     * Gets comment *
     *
     * @return the comment
     * @since 1.0.0
     */
    AsciiBytes getComment() {
        return this.comment;
    }

    /**
     * Gets local header offset *
     *
     * @return the local header offset
     * @since 1.0.0
     */
    @Override
    public long getLocalHeaderOffset() {
        return this.localHeaderOffset;
    }

    /**
     * Clone central directory file header
     *
     * @return the central directory file header
     * @since 1.0.0
     */
    @Override
    @SuppressWarnings("java:S2975")
    public CentralDirectoryFileHeader clone() throws CloneNotSupportedException {
        super.clone();
        byte[] newHeader = new byte[46];
        System.arraycopy(this.header, this.headerOffset, newHeader, 0, newHeader.length);
        return new CentralDirectoryFileHeader(newHeader, 0, this.name, newHeader, this.comment, this.localHeaderOffset);
    }

    /**
     * From random access data central directory file header
     *
     * @param data   data
     * @param offset offset
     * @param filter filter
     * @return the central directory file header
     * @throws IOException io exception
     * @since 1.0.0
     */
    static CentralDirectoryFileHeader fromRandomAccessData(RandomAccessData data, int offset, JarEntryFilter filter)
        throws IOException {
        CentralDirectoryFileHeader fileHeader = new CentralDirectoryFileHeader();
        byte[] bytes = data.read(offset, 46);
        fileHeader.load(bytes, 0, data, offset, filter);
        return fileHeader;
    }

}
