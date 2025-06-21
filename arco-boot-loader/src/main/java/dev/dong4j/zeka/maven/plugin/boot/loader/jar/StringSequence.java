package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import java.util.Objects;

/**
 * A {@link CharSequence} backed by a single shared {@link String}. Unlike a regular
 * {@link String}, {@link #subSequence(int, int)} operations will not copy the underlying
 * character array.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
@SuppressWarnings("all")
final class StringSequence implements CharSequence {

    /** Source */
    private final String source;

    /** Start */
    private final int start;

    /** End */
    private final int end;

    /** Hash */
    private int hash;

    /**
     * String sequence
     *
     * @param source source
     * @since 1.0.0
     */
    StringSequence(String source) {
        this(source, 0, (source != null) ? source.length() : -1);
    }

    /**
     * String sequence
     *
     * @param source source
     * @param start  start
     * @param end    end
     * @since 1.0.0
     */
    StringSequence(String source, int start, int end) {
        Objects.requireNonNull(source, "Source must not be null");
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (end > source.length()) {
            throw new StringIndexOutOfBoundsException(end);
        }
        this.source = source;
        this.start = start;
        this.end = end;
    }

    /**
     * Sub sequence string sequence
     *
     * @param start start
     * @return the string sequence
     * @since 1.0.0
     */
    StringSequence subSequence(int start) {
        return this.subSequence(start, this.length());
    }

    /**
     * Sub sequence string sequence
     *
     * @param start start
     * @param end   end
     * @return the string sequence
     * @since 1.0.0
     */
    @Override
    public StringSequence subSequence(int start, int end) {
        int subSequenceStart = this.start + start;
        int subSequenceEnd = this.start + end;
        if (subSequenceStart > this.end) {
            throw new StringIndexOutOfBoundsException(start);
        }
        if (subSequenceEnd > this.end) {
            throw new StringIndexOutOfBoundsException(end);
        }
        if (start == 0 && subSequenceEnd == this.end) {
            return this;
        }
        return new StringSequence(this.source, subSequenceStart, subSequenceEnd);
    }

    /**
     * Is empty boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    boolean isEmpty() {
        return this.length() == 0;
    }

    /**
     * Length int
     *
     * @return the int
     * @since 1.0.0
     */
    @Override
    public int length() {
        return this.end - this.start;
    }

    /**
     * Char at char
     *
     * @param index index
     * @return the char
     * @since 1.0.0
     */
    @Override
    public char charAt(int index) {
        return this.source.charAt(this.start + index);
    }

    /**
     * Index of int
     *
     * @param ch ch
     * @return the int
     * @since 1.0.0
     */
    int indexOf(char ch) {
        return this.source.indexOf(ch, this.start) - this.start;
    }

    /**
     * Index of int
     *
     * @param str str
     * @return the int
     * @since 1.0.0
     */
    int indexOf(String str) {
        return this.source.indexOf(str, this.start) - this.start;
    }

    /**
     * Index of int
     *
     * @param str       str
     * @param fromIndex from index
     * @return the int
     * @since 1.0.0
     */
    int indexOf(String str, int fromIndex) {
        return this.source.indexOf(str, this.start + fromIndex) - this.start;
    }

    /**
     * Starts with boolean
     *
     * @param prefix prefix
     * @return the boolean
     * @since 1.0.0
     */
    boolean startsWith(CharSequence prefix) {
        return this.startsWith(prefix, 0);
    }

    /**
     * Starts with boolean
     *
     * @param prefix prefix
     * @param offset offset
     * @return the boolean
     * @since 1.0.0
     */
    boolean startsWith(CharSequence prefix, int offset) {
        int prefixLength = prefix.length();
        if (this.length() - prefixLength - offset < 0) {
            return false;
        }
        int prefixOffset = 0;
        int sourceOffset = offset;
        while (prefixLength-- != 0) {
            if (this.charAt(sourceOffset++) != prefix.charAt(prefixOffset++)) {
                return false;
            }
        }
        return true;
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
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof CharSequence)) {
            return false;
        }
        CharSequence other = (CharSequence) obj;
        int n = this.length();
        if (n != other.length()) {
            return false;
        }
        int i = 0;
        while (n-- != 0) {
            if (this.charAt(i) != other.charAt(i)) {
                return false;
            }
            i++;
        }
        return true;
    }

    /**
     * Hash code int
     *
     * @return the int
     * @since 1.0.0
     */
    @Override
    public int hashCode() {
        int hash = this.hash;
        if (hash == 0 && this.length() > 0) {
            for (int i = this.start; i < this.end; i++) {
                hash = 31 * hash + this.source.charAt(i);
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
        return this.source.substring(this.start, this.end);
    }

}
