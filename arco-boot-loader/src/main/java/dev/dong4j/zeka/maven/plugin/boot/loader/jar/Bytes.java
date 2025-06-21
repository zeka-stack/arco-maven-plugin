package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

/**
 * Utilities for dealing with bytes from ZIP files.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
final class Bytes {

    /**
     * Bytes
     *
     * @since 1.0.0
     */
    private Bytes() {
    }

    /**
     * Little endian value long
     *
     * @param bytes  bytes
     * @param offset offset
     * @param length length
     * @return the long
     * @since 1.0.0
     */
    static long littleEndianValue(byte[] bytes, int offset, int length) {
        long value = 0;
        for (int i = length - 1; i >= 0; i--) {
            value = ((value << 8) | (bytes[offset + i] & 0xFF));
        }
        return value;
    }

}
