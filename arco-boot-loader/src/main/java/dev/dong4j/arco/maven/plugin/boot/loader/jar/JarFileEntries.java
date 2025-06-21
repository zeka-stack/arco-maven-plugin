package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import dev.dong4j.arco.maven.plugin.boot.loader.data.RandomAccessData;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Provides access to entries from a {@link CustomJarFile}. In order to reduce memory
 * consumption entry details are stored using int arrays. The {@code hashCodes} array
 * stores the hash code of the entry name, the {@code centralDirectoryOffsets} provides
 * the offset to the central directory record and {@code positions} provides the original
 * order position of the entry. The arrays are stored in hashCode order so that a binary
 * search can be used to find a name.
 * <p>
 * A typical Spring Boot application will have somewhere in the region of 10,500 entries
 * which should consume about 122K.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
@SuppressWarnings("all")
class JarFileEntries implements CentralDirectoryVisitor, Iterable<CustomJarEntry> {

    /** META_INF_PREFIX */
    private static final String META_INF_PREFIX = "META-INF/";

    /** MULTI_RELEASE */
    private static final Name MULTI_RELEASE = new Name("Multi-Release");

    /** BASE_VERSION */
    private static final int BASE_VERSION = 8;

    /** RUNTIME_VERSION */
    private static final int RUNTIME_VERSION;

    static {
        int version;
        try {
            Object runtimeVersion = Runtime.class.getMethod("version").invoke(null);
            version = (int) runtimeVersion.getClass().getMethod("major").invoke(runtimeVersion);
        } catch (Throwable ex) {
            version = BASE_VERSION;
        }
        RUNTIME_VERSION = version;
    }

    /** LOCAL_FILE_HEADER_SIZE */
    private static final long LOCAL_FILE_HEADER_SIZE = 30;

    /** SLASH */
    private static final char SLASH = '/';

    /** NO_SUFFIX */
    private static final char NO_SUFFIX = 0;

    /** ENTRY_CACHE_SIZE */
    protected static final int ENTRY_CACHE_SIZE = 25;

    /** Jar file */
    private final CustomJarFile jarFile;

    /** Filter */
    private final JarEntryFilter filter;

    /** Central directory data */
    private RandomAccessData centralDirectoryData;

    /** Size */
    private int size;

    /** Hash codes */
    private int[] hashCodes;

    /** Central directory offsets */
    private int[] centralDirectoryOffsets;

    /** Positions */
    private int[] positions;

    /** Multi release jar */
    private Boolean multiReleaseJar;

    /** entriesCache */
    private final Map<Integer, FileHeader> entriesCache = Collections
        .synchronizedMap(new LinkedHashMap<Integer, FileHeader>(16, 0.75f, true) {

            private static final long serialVersionUID = 3626484089358515308L;

            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, FileHeader> eldest) {
                if (JarFileEntries.this.jarFile.isSigned()) {
                    return false;
                }
                return this.size() >= ENTRY_CACHE_SIZE;
            }

        });

    /**
     * Jar file entries
     *
     * @param jarFile jar file
     * @param filter  filter
     * @since 1.0.0
     */
    JarFileEntries(CustomJarFile jarFile, JarEntryFilter filter) {
        this.jarFile = jarFile;
        this.filter = filter;
        if (RUNTIME_VERSION == BASE_VERSION) {
            this.multiReleaseJar = false;
        }
    }

    /**
     * Visit start *
     *
     * @param endRecord            end record
     * @param centralDirectoryData central directory data
     * @since 1.0.0
     */
    @Override
    public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
        int maxSize = endRecord.getNumberOfRecords();
        this.centralDirectoryData = centralDirectoryData;
        this.hashCodes = new int[maxSize];
        this.centralDirectoryOffsets = new int[maxSize];
        this.positions = new int[maxSize];
    }

    /**
     * Visit file header *
     *
     * @param fileHeader file header
     * @param dataOffset data offset
     * @since 1.0.0
     */
    @Override
    public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
        AsciiBytes name = this.applyFilter(fileHeader.getName());
        if (name != null) {
            this.add(name, dataOffset);
        }
    }

    /**
     * Add *
     *
     * @param name       name
     * @param dataOffset data offset
     * @since 1.0.0
     */
    private void add(AsciiBytes name, int dataOffset) {
        this.hashCodes[this.size] = name.hashCode();
        this.centralDirectoryOffsets[this.size] = dataOffset;
        this.positions[this.size] = this.size;
        this.size++;
    }

    /**
     * Visit end
     *
     * @since 1.0.0
     */
    @Override
    public void visitEnd() {
        this.sort(0, this.size - 1);
        int[] positions = this.positions;
        this.positions = new int[positions.length];
        for (int i = 0; i < this.size; i++) {
            this.positions[positions[i]] = i;
        }
    }

    /**
     * Gets size *
     *
     * @return the size
     * @since 1.0.0
     */
    int getSize() {
        return this.size;
    }

    /**
     * Sort *
     *
     * @param left  left
     * @param right right
     * @since 1.0.0
     */
    @SuppressWarnings("java:S3776")
    private void sort(int left, int right) {
        // Quick sort algorithm, uses hashCodes as the source but sorts all arrays
        if (left < right) {
            int pivot = this.hashCodes[left + (right - left) / 2];
            int i = left;
            int j = right;
            while (i <= j) {
                while (this.hashCodes[i] < pivot) {
                    i++;
                }
                while (this.hashCodes[j] > pivot) {
                    j--;
                }
                if (i <= j) {
                    this.swap(i, j);
                    i++;
                    j--;
                }
            }
            if (left < j) {
                this.sort(left, j);
            }
            if (right > i) {
                this.sort(i, right);
            }
        }
    }

    /**
     * Swap *
     *
     * @param i
     * @param j j
     * @since 1.0.0
     */
    private void swap(int i, int j) {
        this.swap(this.hashCodes, i, j);
        this.swap(this.centralDirectoryOffsets, i, j);
        this.swap(this.positions, i, j);
    }

    /**
     * Swap *
     *
     * @param array array
     * @param i
     * @param j     j
     * @since 1.0.0
     */
    private void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    /**
     * Iterator iterator
     *
     * @return the iterator
     * @since 1.0.0
     */
    @Override
    public Iterator<CustomJarEntry> iterator() {
        return new EntryIterator();
    }

    /**
     * Contains entry boolean
     *
     * @param name name
     * @return the boolean
     * @since 1.0.0
     */
    boolean containsEntry(CharSequence name) {
        return this.getEntry(name, FileHeader.class, true) != null;
    }

    /**
     * Gets entry *
     *
     * @param name name
     * @return the entry
     * @since 1.0.0
     */
    CustomJarEntry getEntry(CharSequence name) {
        return this.getEntry(name, CustomJarEntry.class, true);
    }

    /**
     * Gets input stream *
     *
     * @param name name
     * @return the input stream
     * @throws IOException io exception
     * @since 1.0.0
     */
    InputStream getInputStream(String name) throws IOException {
        FileHeader entry = this.getEntry(name, FileHeader.class, false);
        return this.getInputStream(entry);
    }

    /**
     * Gets input stream *
     *
     * @param entry entry
     * @return the input stream
     * @throws IOException io exception
     * @since 1.0.0
     */
    InputStream getInputStream(FileHeader entry) throws IOException {
        if (entry == null) {
            return null;
        }
        InputStream inputStream = this.getEntryData(entry).getInputStream();
        if (entry.getMethod() == ZipEntry.DEFLATED) {
            inputStream = new ZipInflaterInputStream(inputStream, (int) entry.getSize());
        }
        return inputStream;
    }

    /**
     * Gets entry data *
     *
     * @param name name
     * @return the entry data
     * @throws IOException io exception
     * @since 1.0.0
     */
    RandomAccessData getEntryData(String name) throws IOException {
        FileHeader entry = this.getEntry(name, FileHeader.class, false);
        if (entry == null) {
            return null;
        }
        return this.getEntryData(entry);
    }

    /**
     * Gets entry data *
     *
     * @param entry entry
     * @return the entry data
     * @throws IOException io exception
     * @since 1.0.0
     */
    private RandomAccessData getEntryData(FileHeader entry) throws IOException {
        // aspectjrt-1.7.4.jar has a different ext bytes length in the
        // local directory to the central directory. We need to re-read
        // here to skip them
        RandomAccessData data = this.jarFile.getData();
        byte[] localHeader = data.read(entry.getLocalHeaderOffset(), LOCAL_FILE_HEADER_SIZE);
        long nameLength = Bytes.littleEndianValue(localHeader, 26, 2);
        long extraLength = Bytes.littleEndianValue(localHeader, 28, 2);
        return data.getSubsection(entry.getLocalHeaderOffset() + LOCAL_FILE_HEADER_SIZE + nameLength + extraLength,
            entry.getCompressedSize());
    }

    /**
     * Gets entry *
     *
     * @param <T>        parameter
     * @param name       name
     * @param type       type
     * @param cacheEntry cache entry
     * @return the entry
     * @since 1.0.0
     */
    private <T extends FileHeader> T getEntry(CharSequence name, Class<T> type, boolean cacheEntry) {
        T entry = this.doGetEntry(name, type, cacheEntry, null);
        if (!this.isMetaInfEntry(name) && this.isMultiReleaseJar()) {
            int version = RUNTIME_VERSION;
            AsciiBytes nameAlias = (entry instanceof CustomJarEntry) ? ((CustomJarEntry) entry).getAsciiBytesName()
                : new AsciiBytes(name.toString());
            while (version > BASE_VERSION) {
                T versionedEntry = this.doGetEntry("META-INF/versions/" + version + "/" + name, type, cacheEntry, nameAlias);
                if (versionedEntry != null) {
                    return versionedEntry;
                }
                version--;
            }
        }
        return entry;
    }

    /**
     * Is meta inf entry boolean
     *
     * @param name name
     * @return the boolean
     * @since 1.0.0
     */
    private boolean isMetaInfEntry(CharSequence name) {
        return name.toString().startsWith(META_INF_PREFIX);
    }

    /**
     * Is multi release jar boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    private boolean isMultiReleaseJar() {
        Boolean multiRelease = this.multiReleaseJar;
        if (multiRelease != null) {
            return multiRelease;
        }
        try {
            Manifest manifest = this.jarFile.getManifest();
            if (manifest == null) {
                multiRelease = false;
            } else {
                Attributes attributes = manifest.getMainAttributes();
                multiRelease = attributes.containsKey(MULTI_RELEASE);
            }
        } catch (IOException ex) {
            multiRelease = false;
        }
        this.multiReleaseJar = multiRelease;
        return multiRelease;
    }

    /**
     * Do get entry t
     *
     * @param <T>        parameter
     * @param name       name
     * @param type       type
     * @param cacheEntry cache entry
     * @param nameAlias  name alias
     * @return the t
     * @since 1.0.0
     */
    private <T extends FileHeader> T doGetEntry(CharSequence name, Class<T> type, boolean cacheEntry,
                                                AsciiBytes nameAlias) {
        int hashCode = AsciiBytes.hashCode(name);
        T entry = this.getEntry(hashCode, name, NO_SUFFIX, type, cacheEntry, nameAlias);
        if (entry == null) {
            hashCode = AsciiBytes.hashCode(hashCode, SLASH);
            entry = this.getEntry(hashCode, name, SLASH, type, cacheEntry, nameAlias);
        }
        return entry;
    }

    /**
     * Gets entry *
     *
     * @param <T>        parameter
     * @param hashCode   hash code
     * @param name       name
     * @param suffix     suffix
     * @param type       type
     * @param cacheEntry cache entry
     * @param nameAlias  name alias
     * @return the entry
     * @since 1.0.0
     */
    private <T extends FileHeader> T getEntry(int hashCode, CharSequence name, char suffix, Class<T> type,
                                              boolean cacheEntry, AsciiBytes nameAlias) {
        int index = this.getFirstIndex(hashCode);
        while (index >= 0 && index < this.size && this.hashCodes[index] == hashCode) {
            T entry = this.getEntry(index, type, cacheEntry, nameAlias);
            if (entry.hasName(name, suffix)) {
                return entry;
            }
            index++;
        }
        return null;
    }

    /**
     * Gets entry *
     *
     * @param <T>        parameter
     * @param index      index
     * @param type       type
     * @param cacheEntry cache entry
     * @param nameAlias  name alias
     * @return the entry
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    private <T extends FileHeader> T getEntry(int index, Class<T> type, boolean cacheEntry, AsciiBytes nameAlias) {
        try {
            FileHeader cached = this.entriesCache.get(index);
            FileHeader entry = (cached != null) ? cached : CentralDirectoryFileHeader
                .fromRandomAccessData(this.centralDirectoryData, this.centralDirectoryOffsets[index], this.filter);
            if (CentralDirectoryFileHeader.class.equals(entry.getClass()) && type.equals(CustomJarEntry.class)) {
                entry = new CustomJarEntry(this.jarFile, (CentralDirectoryFileHeader) entry, nameAlias);
            }
            if (cacheEntry && cached != entry) {
                this.entriesCache.put(index, entry);
            }
            return (T) entry;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets first index *
     *
     * @param hashCode hash code
     * @return the first index
     * @since 1.0.0
     */
    private int getFirstIndex(int hashCode) {
        int index = Arrays.binarySearch(this.hashCodes, 0, this.size, hashCode);
        if (index < 0) {
            return -1;
        }
        while (index > 0 && this.hashCodes[index - 1] == hashCode) {
            index--;
        }
        return index;
    }

    /**
     * Clear cache
     *
     * @since 1.0.0
     */
    void clearCache() {
        this.entriesCache.clear();
    }

    /**
     * Apply filter ascii bytes
     *
     * @param name name
     * @return the ascii bytes
     * @since 1.0.0
     */
    private AsciiBytes applyFilter(AsciiBytes name) {
        return (this.filter != null) ? this.filter.apply(name) : name;
    }

    /**
     * Iterator for contained entries.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @since 1.0.0
     */
    private class EntryIterator implements Iterator<CustomJarEntry> {

        /** Index */
        private int index = 0;

        /**
         * Has next boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean hasNext() {
            return this.index < JarFileEntries.this.size;
        }

        /**
         * Next jar entry
         *
         * @return the jar entry
         * @since 1.0.0
         */
        @Override
        public CustomJarEntry next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            int entryIndex = JarFileEntries.this.positions[this.index];
            this.index++;
            return JarFileEntries.this.getEntry(entryIndex, CustomJarEntry.class, false, null);
        }

    }

}
