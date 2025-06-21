package dev.dong4j.arco.maven.plugin.boot.loader.data;

import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

/**
 * {@link RandomAccessData} implementation backed by a {@link RandomAccessFile}.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:49
 * @since 1.0.0
 */
public class RandomAccessDataFile implements RandomAccessData {

    /** File access */
    private final FileAccess fileAccess;

    /** Offset */
    private final long offset;

    /** Length */
    private final long length;

    /**
     * Create a new {@link RandomAccessDataFile} backed by the specified file.
     *
     * @param file the underlying file
     * @throws IllegalArgumentException if the file is null or does not exist
     * @since 1.0.0
     */
    public RandomAccessDataFile(File file) {
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }
        this.fileAccess = new FileAccess(file);
        this.offset = 0L;
        this.length = file.length();
    }

    /**
     * Private constructor used to create a {@link #getSubsection(long, long) subsection}.
     *
     * @param fileAccess provides access to the underlying file
     * @param offset     the offset of the section
     * @param length     the length of the section
     * @since 1.0.0
     */
    private RandomAccessDataFile(FileAccess fileAccess, long offset, long length) {
        this.fileAccess = fileAccess;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Returns the underlying File.
     *
     * @return the underlying file
     * @since 1.0.0
     */
    public File getFile() {
        return this.fileAccess.file;
    }

    /**
     * Gets input stream *
     *
     * @return the input stream
     * @since 1.0.0
     */
    @Override
    public InputStream getInputStream() {
        return new DataInputStream();
    }

    /**
     * Gets subsection *
     *
     * @param offset offset
     * @param length length
     * @return the subsection
     * @since 1.0.0
     */
    @Override
    public RandomAccessData getSubsection(long offset, long length) {
        if (offset < 0 || length < 0 || offset + length > this.length) {
            throw new IndexOutOfBoundsException();
        }
        return new RandomAccessDataFile(this.fileAccess, this.offset + offset, length);
    }

    /**
     * Read byte [ ]
     *
     * @return the byte [ ]
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public byte[] read() throws IOException {
        return this.read(0, this.length);
    }

    /**
     * Read byte [ ]
     *
     * @param offset offset
     * @param length length
     * @return the byte [ ]
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public byte[] read(long offset, long length) throws IOException {
        if (offset > this.length) {
            throw new IndexOutOfBoundsException();
        }
        if (offset + length > this.length) {
            throw new EOFException();
        }
        byte[] bytes = new byte[(int) length];
        this.read(bytes, offset, 0, bytes.length);
        return bytes;
    }

    /**
     * Read int
     *
     * @param bytes    bytes
     * @param position position
     * @param offset   offset
     * @param length   length
     * @return the int
     * @throws IOException io exception
     * @since 1.0.0
     */
    private int read(byte[] bytes, long position, int offset, int length) throws IOException {
        if (position > this.length) {
            return -1;
        }
        return this.fileAccess.read(bytes, this.offset + position, offset, length);
    }

    /**
     * Gets size *
     *
     * @return the size
     * @since 1.0.0
     */
    @Override
    public long getSize() {
        return this.length;
    }

    /**
     * Close *
     *
     * @throws IOException io exception
     * @since 1.0.0
     */
    public void close() throws IOException {
        this.fileAccess.close();
    }

    /**
     * {@link InputStream} implementation for the {@link RandomAccessDataFile}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private class DataInputStream extends InputStream {

        /** Position */
        private int position;

        /**
         * Read int
         *
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        @Override
        public int read() throws IOException {
            int read = this.readByte(this.position);
            if (read > -1) {
                this.moveOn(1);
            }
            return read;
        }

        /**
         * Read int
         *
         * @param b b
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        @Override
        public int read(byte[] b) throws IOException {
            return this.read(b, 0, (b != null) ? b.length : 0);
        }

        /**
         * Read int
         *
         * @param b   b
         * @param off off
         * @param len len
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (b == null) {
                throw new NullPointerException("Bytes must not be null");
            }
            return this.doRead(b, off, len);
        }

        /**
         * Perform the actual read.
         *
         * @param b   the bytes to read or {@code null} when reading a single byte
         * @param off the offset of the byte array
         * @param len the length of data to read
         * @return the number of bytes read into {@code b} or the actual read byte if     {@code b} is {@code null}. Returns -1 when the
         * end of the stream is reached
         * @throws IOException in case of I/O errors
         * @since 1.0.0
         */
        int doRead(byte[] b, int off, int len) throws IOException {
            if (len == 0) {
                return 0;
            }
            int cappedLen = this.cap(len);
            if (cappedLen <= 0) {
                return -1;
            }
            return (int) this.moveOn(RandomAccessDataFile.this.read(b, this.position, off, cappedLen));
        }

        /**
         * Skip long
         *
         * @param n n
         * @return the long
         * @since 1.0.0
         */
        @Override
        public long skip(long n) {
            return (n <= 0) ? 0 : this.moveOn(this.cap(n));
        }

        /**
         * Cap the specified value such that it cannot exceed the number of bytes
         * remaining.
         *
         * @param n the value to cap
         * @return the capped value
         * @since 1.0.0
         */
        private int cap(long n) {
            return (int) Math.min(RandomAccessDataFile.this.length - this.position, n);
        }

        /**
         * Move the stream position forwards the specified amount.
         *
         * @param amount the amount to move
         * @return the amount moved
         * @since 1.0.0
         */
        private long moveOn(int amount) {
            this.position += amount;
            return amount;
        }

        /**
         * Read byte int
         *
         * @param position position
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        private int readByte(long position) throws IOException {
            if (position >= RandomAccessDataFile.this.length) {
                return -1;
            }
            return RandomAccessDataFile.this.fileAccess.readByte(RandomAccessDataFile.this.offset + position);
        }

    }

    /**
     * <p>Description: >/p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private static final class FileAccess {

        /** Monitor */
        private final Object monitor = new Object();

        /** File */
        private final File file;

        /** Random access file */
        private RandomAccessFile randomAccessFile;

        /**
         * File access
         *
         * @param file file
         * @since 1.0.0
         */
        private FileAccess(File file) {
            this.file = file;
            this.openIfNecessary();
        }

        /**
         * Read int
         *
         * @param bytes    bytes
         * @param position position
         * @param offset   offset
         * @param length   length
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        private int read(byte[] bytes, long position, int offset, int length) throws IOException {
            synchronized (this.monitor) {
                this.openIfNecessary();
                this.randomAccessFile.seek(position);
                return this.randomAccessFile.read(bytes, offset, length);
            }
        }

        /**
         * Open if necessary
         *
         * @since 1.0.0
         */
        private void openIfNecessary() {
            if (this.randomAccessFile == null) {
                try {
                    this.randomAccessFile = new RandomAccessFile(this.file, "r");
                } catch (FileNotFoundException ex) {
                    throw new IllegalArgumentException(
                        String.format("File %s must exist", this.file.getAbsolutePath()));
                }
            }
        }

        /**
         * Close *
         *
         * @throws IOException io exception
         * @since 1.0.0
         */
        private void close() throws IOException {
            synchronized (this.monitor) {
                if (this.randomAccessFile != null) {
                    this.randomAccessFile.close();
                    this.randomAccessFile = null;
                }
            }
        }

        /**
         * Read byte int
         *
         * @param position position
         * @return the int
         * @throws IOException io exception
         * @since 1.0.0
         */
        private int readByte(long position) throws IOException {
            synchronized (this.monitor) {
                this.openIfNecessary();
                this.randomAccessFile.seek(position);
                return this.randomAccessFile.read();
            }
        }

    }

}
