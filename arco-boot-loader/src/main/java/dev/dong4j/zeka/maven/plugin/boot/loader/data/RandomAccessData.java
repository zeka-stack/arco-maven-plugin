package dev.dong4j.zeka.maven.plugin.boot.loader.data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface that provides read-only random access to some underlying data.
 * Implementations must allow concurrent reads in a thread-safe manner.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:49
 * @since 1.0.0
 */
public interface RandomAccessData {

    /**
     * Returns an {@link InputStream} that can be used to read the underlying data. The
     * caller is responsible close the underlying stream.
     *
     * @return a new input stream that can be used to read the underlying data.
     * @throws IOException if the stream cannot be opened
     * @since 1.0.0
     */
    InputStream getInputStream() throws IOException;

    /**
     * Returns a new {@link RandomAccessData} for a specific subsection of this data.
     *
     * @param offset the offset of the subsection
     * @param length the length of the subsection
     * @return the subsection data
     * @since 1.0.0
     */
    RandomAccessData getSubsection(long offset, long length);

    /**
     * Reads all the data and returns it as a byte array.
     *
     * @return the data
     * @throws IOException if the data cannot be read
     * @since 1.0.0
     */
    byte[] read() throws IOException;

    /**
     * Reads the {@code length} bytes of data starting at the given {@code offset}.
     *
     * @param offset the offset from which data should be read
     * @param length the number of bytes to be read
     * @return the data
     * @throws IOException if the data cannot be read
     * @since 1.0.0
     */
    byte[] read(long offset, long length) throws IOException;

    /**
     * Returns the size of the data.
     *
     * @return the size
     * @since 1.0.0
     */
    long getSize();

}
