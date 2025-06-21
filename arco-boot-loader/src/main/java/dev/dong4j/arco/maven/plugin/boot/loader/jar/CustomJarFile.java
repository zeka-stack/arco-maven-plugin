package dev.dong4j.arco.maven.plugin.boot.loader.jar;

import dev.dong4j.arco.maven.plugin.boot.loader.data.RandomAccessData;
import dev.dong4j.arco.maven.plugin.boot.loader.data.RandomAccessDataFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Extended variant of {@link java.util.jar.JarFile} that behaves in the same way but
 * offers the following additional functionality.
 * <ul>
 * <li>A nested {@link CustomJarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} based
 * on any directory entry.</li>
 * <li>A nested {@link CustomJarFile} can be {@link #getNestedJarFile(ZipEntry) obtained} for
 * embedded JAR files (as long as their entry is not compressed).</li>
 * </ul>
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
public class CustomJarFile extends java.util.jar.JarFile {

    /** MANIFEST_NAME */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";
    /** PROTOCOL_HANDLER */
    private static final String PROTOCOL_HANDLER = "java.protocol.handler.pkgs";
    /** HANDLERS_PACKAGE */
    private static final String HANDLERS_PACKAGE = "dev.dong4j.arco.maven.plugin.boot.loader";
    /** META_INF */
    private static final AsciiBytes META_INF = new AsciiBytes("META-INF/");
    /** SIGNATURE_FILE_EXTENSION */
    private static final AsciiBytes SIGNATURE_FILE_EXTENSION = new AsciiBytes(".SF");
    /** Root file */
    private final RandomAccessDataFile rootFile;
    /** Path from root */
    private final String pathFromRoot;
    /** Data */
    private final RandomAccessData data;
    /** Type */
    private final JarFileType type;
    /** Url */
    private URL url;
    /** Url string */
    private String urlString;
    /** Entries */
    private final JarFileEntries entries;
    /** Manifest supplier */
    private final Supplier<Manifest> manifestSupplier;
    /** Manifest */
    private SoftReference<Manifest> manifest;
    /** Signed */
    private boolean signed;
    /** Comment */
    private String comment;

    /**
     * Create a new {@link CustomJarFile} backed by the specified file.
     *
     * @param file the root jar file
     * @throws IOException if the file cannot be read
     * @since 1.0.0
     */
    public CustomJarFile(File file) throws IOException {
        this(new RandomAccessDataFile(file));
    }

    /**
     * Create a new {@link CustomJarFile} backed by the specified file.
     *
     * @param file the root jar file
     * @throws IOException if the file cannot be read
     * @since 1.0.0
     */
    CustomJarFile(RandomAccessDataFile file) throws IOException {
        this(file, "", file, JarFileType.DIRECT);
    }

    /**
     * Private constructor used to create a new {@link CustomJarFile} either directly or from a
     * nested entry.
     *
     * @param rootFile     the root jar file
     * @param pathFromRoot the name of this file
     * @param data         the underlying data
     * @param type         the type of the jar file
     * @throws IOException if the file cannot be read
     * @since 1.0.0
     */
    private CustomJarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarFileType type)
        throws IOException {
        this(rootFile, pathFromRoot, data, null, type, null);
    }

    /**
     * Jar file
     *
     * @param rootFile         root file
     * @param pathFromRoot     path from root
     * @param data             data
     * @param filter           filter
     * @param type             type
     * @param manifestSupplier manifest supplier
     * @throws IOException io exception
     * @since 1.0.0
     */
    @SuppressWarnings("java:S112")
    private CustomJarFile(RandomAccessDataFile rootFile, String pathFromRoot, RandomAccessData data, JarEntryFilter filter,
                          JarFileType type, Supplier<Manifest> manifestSupplier) throws IOException {
        super(rootFile.getFile());
        this.rootFile = rootFile;
        this.pathFromRoot = pathFromRoot;
        CentralDirectoryParser parser = new CentralDirectoryParser();
        this.entries = parser.addVisitor(new JarFileEntries(this, filter));
        this.type = type;
        parser.addVisitor(this.centralDirectoryVisitor());
        try {
            this.data = parser.parse(data, filter == null);
        } catch (RuntimeException ex) {
            this.close();
            throw ex;
        }
        this.manifestSupplier = (manifestSupplier != null) ? manifestSupplier : () -> {
            try (InputStream inputStream = this.getInputStream(MANIFEST_NAME)) {
                if (inputStream == null) {
                    return null;
                }
                return new Manifest(inputStream);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        };
    }

    /**
     * Central directory visitor central directory visitor
     *
     * @return the central directory visitor
     * @since 1.0.0
     */
    private CentralDirectoryVisitor centralDirectoryVisitor() {
        return new CentralDirectoryVisitor() {

            @Override
            public void visitStart(CentralDirectoryEndRecord endRecord, RandomAccessData centralDirectoryData) {
                CustomJarFile.this.comment = endRecord.getComment();
            }

            @Override
            public void visitFileHeader(CentralDirectoryFileHeader fileHeader, int dataOffset) {
                AsciiBytes name = fileHeader.getName();
                if (name.startsWith(META_INF) && name.endsWith(SIGNATURE_FILE_EXTENSION)) {
                    CustomJarFile.this.signed = true;
                }
            }

            @Override
            public void visitEnd() {
                // nothing to do
            }

        };
    }

    /**
     * Gets root jar file *
     *
     * @return the root jar file
     * @since 1.0.0
     */
    protected final RandomAccessDataFile getRootJarFile() {
        return this.rootFile;
    }

    /**
     * Gets data *
     *
     * @return the data
     * @since 1.0.0
     */
    RandomAccessData getData() {
        return this.data;
    }

    /**
     * Gets manifest *
     *
     * @return the manifest
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Manifest getManifest() throws IOException {
        Manifest tempManifest = (this.manifest != null) ? this.manifest.get() : null;
        if (tempManifest == null) {
            try {
                tempManifest = this.manifestSupplier.get();
            } catch (RuntimeException ex) {
                throw new IOException(ex);
            }
            this.manifest = new SoftReference<>(tempManifest);
        }
        return tempManifest;
    }

    /**
     * Entries enumeration
     *
     * @return the enumeration
     * @since 1.0.0
     */
    @Override
    public Enumeration<java.util.jar.JarEntry> entries() {
        Iterator<CustomJarEntry> iterator = this.entries.iterator();
        return new Enumeration<java.util.jar.JarEntry>() {

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public java.util.jar.JarEntry nextElement() {
                return iterator.next();
            }

        };
    }

    /**
     * Gets jar entry *
     *
     * @param name name
     * @return the jar entry
     * @since 1.0.0
     */
    public CustomJarEntry getJarEntry(CharSequence name) {
        return this.entries.getEntry(name);
    }

    /**
     * Gets jar entry *
     *
     * @param name name
     * @return the jar entry
     * @since 1.0.0
     */
    @Override
    public CustomJarEntry getJarEntry(String name) {
        return (CustomJarEntry) this.getEntry(name);
    }

    /**
     * Contains entry boolean
     *
     * @param name name
     * @return the boolean
     * @since 1.0.0
     */
    public boolean containsEntry(String name) {
        return this.entries.containsEntry(name);
    }

    /**
     * Gets entry *
     *
     * @param name name
     * @return the entry
     * @since 1.0.0
     */
    @Override
    public ZipEntry getEntry(String name) {
        return this.entries.getEntry(name);
    }

    /**
     * Gets input stream *
     *
     * @param entry entry
     * @return the input stream
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public synchronized InputStream getInputStream(ZipEntry entry) throws IOException {
        if (entry instanceof CustomJarEntry) {
            return this.entries.getInputStream((CustomJarEntry) entry);
        }
        return this.getInputStream((entry != null) ? entry.getName() : null);
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
        return this.entries.getInputStream(name);
    }

    /**
     * Return a nested {@link CustomJarFile} loaded from the specified entry.
     *
     * @param entry the zip entry
     * @return a {@link CustomJarFile} for the entry
     * @throws IOException if the nested jar file cannot be read
     * @since 1.0.0
     */
    public synchronized CustomJarFile getNestedJarFile(ZipEntry entry) throws IOException {
        return this.getNestedJarFile((CustomJarEntry) entry);
    }

    /**
     * Return a nested {@link CustomJarFile} loaded from the specified entry.
     *
     * @param entry the zip entry
     * @return a {@link CustomJarFile} for the entry
     * @throws IOException if the nested jar file cannot be read
     * @since 1.0.0
     */
    public synchronized CustomJarFile getNestedJarFile(CustomJarEntry entry) throws IOException {
        try {
            return this.createJarFileFromEntry(entry);
        } catch (Exception ex) {
            throw new IOException("Unable to open nested jar file '" + entry.getName() + "'", ex);
        }
    }

    /**
     * Create jar file from entry jar file
     *
     * @param entry entry
     * @return the jar file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private CustomJarFile createJarFileFromEntry(CustomJarEntry entry) throws IOException {
        if (entry.isDirectory()) {
            return this.createJarFileFromDirectoryEntry(entry);
        }
        return this.createJarFileFromFileEntry(entry);
    }

    /**
     * Create jar file from directory entry jar file
     *
     * @param entry entry
     * @return the jar file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private CustomJarFile createJarFileFromDirectoryEntry(CustomJarEntry entry) throws IOException {
        AsciiBytes name = entry.getAsciiBytesName();
        JarEntryFilter filter = candidate -> {
            if (candidate.startsWith(name) && !candidate.equals(name)) {
                return candidate.substring(name.length());
            }
            return null;
        };
        return new CustomJarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName().substring(0, name.length() - 1),
            this.data, filter, JarFileType.NESTED_DIRECTORY, this.manifestSupplier);
    }

    /**
     * Create jar file from file entry jar file
     *
     * @param entry entry
     * @return the jar file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private CustomJarFile createJarFileFromFileEntry(CustomJarEntry entry) throws IOException {
        if (entry.getMethod() != ZipEntry.STORED) {
            throw new IllegalStateException(
                "Unable to open nested entry '" + entry.getName() + "'. It has been compressed and nested "
                    + "jar files must be stored without compression. Please check the "
                    + "mechanism used to create your executable jar file");
        }
        RandomAccessData entryData = this.entries.getEntryData(entry.getName());
        return new CustomJarFile(this.rootFile, this.pathFromRoot + "!/" + entry.getName(), entryData,
            JarFileType.NESTED_JAR);
    }

    /**
     * Gets comment *
     *
     * @return the comment
     * @since 1.0.0
     */
    @Override
    public String getComment() {
        return this.comment;
    }

    /**
     * Size int
     *
     * @return the int
     * @since 1.0.0
     */
    @Override
    public int size() {
        return this.entries.getSize();
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
        if (this.type == JarFileType.DIRECT) {
            this.rootFile.close();
        }
    }

    /**
     * Gets url string *
     *
     * @return the url string
     * @throws MalformedURLException malformed url exception
     * @since 1.0.0
     */
    String getUrlString() throws MalformedURLException {
        if (this.urlString == null) {
            this.urlString = this.getUrl().toString();
        }
        return this.urlString;
    }

    /**
     * Return a URL that can be used to access this JAR file. NOTE: the specified URL
     * cannot be serialized and or cloned.
     *
     * @return the URL
     * @throws MalformedURLException if the URL is malformed
     * @since 1.0.0
     */
    public URL getUrl() throws MalformedURLException {
        if (this.url == null) {
            Handler handler = new Handler(this);
            String file = this.rootFile.getFile().toURI() + this.pathFromRoot + "!/";
            file = file.replace("file:////", "file://"); // Fix UNC paths
            this.url = new URL("jar", "", -1, file, handler);
        }
        return this.url;
    }

    /**
     * To string string
     *
     * @return the string
     * @since 1.0.0
     */
    @Override
    public String toString() {
        return this.getName();
    }

    /**
     * Gets name *
     *
     * @return the name
     * @since 1.0.0
     */
    @Override
    public String getName() {
        return this.rootFile.getFile() + this.pathFromRoot;
    }

    /**
     * Is signed boolean
     *
     * @return the boolean
     * @since 1.0.0
     */
    boolean isSigned() {
        return this.signed;
    }

    /**
     * Sets entry certificates *
     *
     * @param entry entry
     * @since 1.0.0
     */
    void setupEntryCertificates(CustomJarEntry entry) {
        // Fallback to JarInputStream to obtain certificates, not fast but hopefully not
        // happening that often.
        try {
            try (JarInputStream inputStream = new JarInputStream(this.getData().getInputStream())) {
                java.util.jar.JarEntry certEntry = inputStream.getNextJarEntry();
                while (certEntry != null) {
                    inputStream.closeEntry();
                    if (entry.getName().equals(certEntry.getName())) {
                        this.setCertificates(entry, certEntry);
                    }
                    this.setCertificates(this.getJarEntry(certEntry.getName()), certEntry);
                    certEntry = inputStream.getNextJarEntry();
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Sets certificates *
     *
     * @param entry     entry
     * @param certEntry cert entry
     * @since 1.0.0
     */
    private void setCertificates(CustomJarEntry entry, java.util.jar.JarEntry certEntry) {
        if (entry != null) {
            entry.setCertificates(certEntry);
        }
    }

    /**
     * Clear cache
     *
     * @since 1.0.0
     */
    public void clearCache() {
        this.entries.clearCache();
    }

    /**
     * Gets path from root *
     *
     * @return the path from root
     * @since 1.0.0
     */
    protected String getPathFromRoot() {
        return this.pathFromRoot;
    }

    /**
     * Gets type *
     *
     * @return the type
     * @since 1.0.0
     */
    JarFileType getType() {
        return this.type;
    }

    /**
     * Register a {@literal 'java.protocol.handler.pkgs'} property so that a
     * {@link URLStreamHandler} will be located to deal with jar URLs.
     *
     * @since 1.0.0
     */
    public static void registerUrlProtocolHandler() {
        String handlers = System.getProperty(PROTOCOL_HANDLER, "");
        System.setProperty(PROTOCOL_HANDLER,
            ("".equals(handlers) ? HANDLERS_PACKAGE : handlers + "|" + HANDLERS_PACKAGE));
        resetCachedUrlHandlers();
    }

    /**
     * Reset any cached handlers just in case a jar protocol has already been used. We
     * reset the handler by trying to set a null {@link URLStreamHandlerFactory} which
     * should have no effect other than clearing the handlers cache.
     *
     * @since 1.0.0
     */
    @SuppressWarnings("java:S1181")
    private static void resetCachedUrlHandlers() {
        try {
            URL.setURLStreamHandlerFactory(null);
        } catch (Error ignored) {
            // nothing to do
        }
    }

    /**
     * The type of a {@link CustomJarFile}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @since 1.0.0
     */
    enum JarFileType {

        /** Direct jar file type */
        DIRECT,
        /** Nested directory jar file type */
        NESTED_DIRECTORY,
        /** Nested jar jar file type */
        NESTED_JAR

    }

}
