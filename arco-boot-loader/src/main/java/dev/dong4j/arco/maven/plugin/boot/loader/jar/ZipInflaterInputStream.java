package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * {@link InflaterInputStream} that supports the writing of an extra "dummy" byte (which
 * is required with JDK 6) and returns accurate available() results.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
class ZipInflaterInputStream extends InflaterInputStream {

    /** Available */
    private int available;

    /** Extra bytes written */
    private boolean extraBytesWritten;

    /**
     * Zip inflater input stream
     *
     * @param inputStream input stream
     * @param size        size
     * @since 1.0.0
     */
    ZipInflaterInputStream(InputStream inputStream, int size) {
        super(inputStream, new Inflater(true), getInflaterBufferSize(size));
        this.available = size;
    }

    /**
     * Available int
     *
     * @return the int
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public int available() throws IOException {
        if (this.available < 0) {
            return super.available();
        }
        return this.available;
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
        int result = super.read(b, off, len);
        if (result != -1) {
            this.available -= result;
        }
        return result;
    }

    /**
     * Close *
     *
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void close() throws IOException {
        super.close();
        this.inf.end();
    }

    /**
     * Fill *
     *
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    protected void fill() throws IOException {
        try {
            super.fill();
        } catch (EOFException ex) {
            if (this.extraBytesWritten) {
                throw ex;
            }
            this.len = 1;
            this.buf[0] = 0x0;
            this.extraBytesWritten = true;
            this.inf.setInput(this.buf, 0, this.len);
        }
    }

    /**
     * Gets inflater buffer size *
     *
     * @param size size
     * @return the inflater buffer size
     * @since 1.0.0
     */
    private static int getInflaterBufferSize(long size) {
        size += 2; // inflater likes some space
        size = (size > 65536) ? 8192 : size;
        size = (size <= 0) ? 4096 : size;
        return (int) size;
    }

}
