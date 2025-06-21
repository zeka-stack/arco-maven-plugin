package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import java.nio.charset.StandardCharsets;

/**
 * Simple wrapper around a byte array that represents an ASCII. Used for performance
 * reasons to save constructing Strings for ZIP data.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
@SuppressWarnings("all")
final class AsciiBytes {

    /** EMPTY_STRING */
    private static final String EMPTY_STRING = "";

    /** INITIAL_BYTE_BITMASK */
    private static final int[] INITIAL_BYTE_BITMASK = {0x7F, 0x1F, 0x0F, 0x07};

    /** SUBSEQUENT_BYTE_BITMASK */
    private static final int SUBSEQUENT_BYTE_BITMASK = 0x3F;

    /** Bytes */
    private final byte[] bytes;

    /** Offset */
    private final int offset;

    /** Length */
    private final int length;

    /** String */
    private String string;

    /** Hash */
    private int hash;

    /**
     * Create a new {@link AsciiBytes} from the specified String.
     *
     * @param string the source string
     * @since 1.0.0
     */
    AsciiBytes(String string) {
        this(string.getBytes(StandardCharsets.UTF_8));
        this.string = string;
    }

    /**
     * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
     * are not expected to change.
     *
     * @param bytes the source bytes
     * @since 1.0.0
     */
    AsciiBytes(byte[] bytes) {
        this(bytes, 0, bytes.length);
    }

    /**
     * Create a new {@link AsciiBytes} from the specified bytes. NOTE: underlying bytes
     * are not expected to change.
     *
     * @param bytes  the source bytes
     * @param offset the offset
     * @param length the length
     * @since 1.0.0
     */
    AsciiBytes(byte[] bytes, int offset, int length) {
        if (offset < 0 || length < 0 || (offset + length) > bytes.length) {
            throw new IndexOutOfBoundsException();
        }
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    /**
     * Length int
     *
     * @return the int
     * @since 1.0.0
     */
    int length() {
        return this.length;
    }

    /**
     * Starts with boolean
     *
     * @param prefix prefix
     * @return the boolean
     * @since 1.0.0
     */
    boolean startsWith(AsciiBytes prefix) {
        if (this == prefix) {
            return true;
        }
        if (prefix.length > this.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (this.bytes[i + this.offset] != prefix.bytes[i + prefix.offset]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Ends with boolean
     *
     * @param postfix postfix
     * @return the boolean
     * @since 1.0.0
     */
    boolean endsWith(AsciiBytes postfix) {
        if (this == postfix) {
            return true;
        }
        if (postfix.length > this.length) {
            return false;
        }
        for (int i = 0; i < postfix.length; i++) {
            if (this.bytes[this.offset + (this.length - 1) - i] != postfix.bytes[postfix.offset + (postfix.length - 1)
                - i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Substring ascii bytes
     *
     * @param beginIndex begin index
     * @return the ascii bytes
     * @since 1.0.0
     */
    AsciiBytes substring(int beginIndex) {
        return this.substring(beginIndex, this.length);
    }

    /**
     * Substring ascii bytes
     *
     * @param beginIndex begin index
     * @param endIndex   end index
     * @return the ascii bytes
     * @since 1.0.0
     */
    AsciiBytes substring(int beginIndex, int endIndex) {
        if (this.offset + endIndex - beginIndex > this.bytes.length) {
            throw new IndexOutOfBoundsException();
        }
        return new AsciiBytes(this.bytes, this.offset + beginIndex, this.length);
    }

    /**
     * Matches boolean
     *
     * @param name   name
     * @param suffix suffix
     * @return the boolean
     * @since 1.0.0
     */
    boolean matches(CharSequence name, char suffix) {
        int charIndex = 0;
        int nameLen = name.length();
        int totalLen = nameLen + ((suffix != 0) ? 1 : 0);
        for (int i = this.offset; i < this.offset + this.length; i++) {
            int b = this.bytes[i];
            int remainingUtfBytes = this.getNumberOfUtfBytes(b) - 1;
            b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
            for (int j = 0; j < remainingUtfBytes; j++) {
                b = (b << 6) + (this.bytes[++i] & SUBSEQUENT_BYTE_BITMASK);
            }
            char c = this.getChar(name, suffix, charIndex++);
            if (b <= 0xFFFF) {
                if (c != b) {
                    return false;
                }
            } else {
                if (c != ((b >> 0xA) + 0xD7C0)) {
                    return false;
                }
                c = this.getChar(name, suffix, charIndex++);
                if (c != ((b & 0x3FF) + 0xDC00)) {
                    return false;
                }
            }
        }
        return charIndex == totalLen;
    }

    /**
     * Gets char *
     *
     * @param name   name
     * @param suffix suffix
     * @param index  index
     * @return the char
     * @since 1.0.0
     */
    private char getChar(CharSequence name, char suffix, int index) {
        if (index < name.length()) {
            return name.charAt(index);
        }
        if (index == name.length()) {
            return suffix;
        }
        return 0;
    }

    /**
     * Gets number of utf bytes *
     *
     * @param b b
     * @return the number of utf bytes
     * @since 1.0.0
     */
    private int getNumberOfUtfBytes(int b) {
        if ((b & 0x80) == 0) {
            return 1;
        }
        int numberOfUtfBytes = 0;
        while ((b & 0x80) != 0) {
            b <<= 1;
            numberOfUtfBytes++;
        }
        return numberOfUtfBytes;
    }

    /**
     * Equals boolean
     *
     * @param obj obj
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        if (obj.getClass() == AsciiBytes.class) {
            AsciiBytes other = (AsciiBytes) obj;
            if (this.length == other.length) {
                for (int i = 0; i < this.length; i++) {
                    if (this.bytes[this.offset + i] != other.bytes[other.offset + i]) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Hash code int
     *
     * @return the int
     * @since 1.0.0
     */
    @Override
    @SuppressWarnings(value = {"java:S1117", "java:S127"})
    public int hashCode() {
        int hash = this.hash;
        if (hash == 0 && this.bytes.length > 0) {
            for (int i = this.offset; i < this.offset + this.length; i++) {
                int b = this.bytes[i];
                int remainingUtfBytes = this.getNumberOfUtfBytes(b) - 1;
                b &= INITIAL_BYTE_BITMASK[remainingUtfBytes];
                for (int j = 0; j < remainingUtfBytes; j++) {
                    b = (b << 6) + (this.bytes[++i] & SUBSEQUENT_BYTE_BITMASK);
                }
                if (b <= 0xFFFF) {
                    hash = 31 * hash + b;
                } else {
                    hash = 31 * hash + ((b >> 0xA) + 0xD7C0);
                    hash = 31 * hash + ((b & 0x3FF) + 0xDC00);
                }
            }
            this.hash = hash;
        }
        return hash;
    }

    /**
     * To string string
     *
     * @return the string
     * @since 1.0.0
     */
    @Override
    public String toString() {
        if (this.string == null) {
            if (this.length == 0) {
                this.string = EMPTY_STRING;
            } else {
                this.string = new String(this.bytes, this.offset, this.length, StandardCharsets.UTF_8);
            }
        }
        return this.string;
    }

    /**
     * To string string
     *
     * @param bytes bytes
     * @return the string
     * @since 1.0.0
     */
    static String toString(byte[] bytes) {
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Hash code int
     *
     * @param charSequence char sequence
     * @return the int
     * @since 1.0.0
     */
    static int hashCode(CharSequence charSequence) {
        // We're compatible with String's hashCode()
        if (charSequence instanceof StringSequence) {
            // ... but save making an unnecessary String for StringSequence
            return charSequence.hashCode();
        }
        return charSequence.toString().hashCode();
    }

    /**
     * Hash code int
     *
     * @param hash   hash
     * @param suffix suffix
     * @return the int
     * @since 1.0.0
     */
    static int hashCode(int hash, char suffix) {
        return (suffix != 0) ? (31 * hash + suffix) : hash;
    }

}
