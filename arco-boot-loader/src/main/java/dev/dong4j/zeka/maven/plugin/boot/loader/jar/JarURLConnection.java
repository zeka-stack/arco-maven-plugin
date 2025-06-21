package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FilePermission;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLStreamHandler;
import java.security.Permission;

/**
 * 扩展 {@link java.net.JarURLConnection} 已以实现 jar in jar 中的资源加载, {@link CustomJarFile#getUrl()}.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @since 1.0.0
 */
@SuppressWarnings("all")
final class JarURLConnection extends java.net.JarURLConnection {

    /** useFastExceptions */
    private static final ThreadLocal<Boolean> useFastExceptions = new ThreadLocal<>();

    /** FILE_NOT_FOUND_EXCEPTION */
    private static final FileNotFoundException FILE_NOT_FOUND_EXCEPTION = new FileNotFoundException(
        "Jar file or entry not found");

    /** NOT_FOUND_CONNECTION_EXCEPTION */
    private static final IllegalStateException NOT_FOUND_CONNECTION_EXCEPTION = new IllegalStateException(
        FILE_NOT_FOUND_EXCEPTION);

    /** SEPARATOR */
    private static final String SEPARATOR = "!/";

    /** EMPTY_JAR_URL */
    private static final URL EMPTY_JAR_URL;

    static {
        try {
            EMPTY_JAR_URL = new URL("jar:", null, 0, "file:!/", new URLStreamHandler() {
                @Override
                protected URLConnection openConnection(URL u) throws IOException {
                    // Stub URLStreamHandler to prevent the wrong JAR Handler from being
                    // Instantiated and cached.
                    return null;
                }
            });
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /** EMPTY_JAR_ENTRY_NAME */
    private static final JarEntryName EMPTY_JAR_ENTRY_NAME = new JarEntryName(new StringSequence(""));

    /** READ_ACTION */
    private static final String READ_ACTION = "read";

    /** NOT_FOUND_CONNECTION */
    private static final JarURLConnection NOT_FOUND_CONNECTION = JarURLConnection.notFound();

    /** Jar file */
    private final CustomJarFile jarFile;

    /** Permission */
    private Permission permission;

    /** Jar file url */
    private URL jarFileUrl;

    /** Jar entry name */
    private final JarEntryName jarEntryName;

    /** Close action */
    private final CloseAction closeAction;

    /** Jar entry */
    private CustomJarEntry jarEntry;

    /**
     * Jar url connection
     *
     * @param url          url
     * @param jarFile      jar file
     * @param jarEntryName jar entry name
     * @param closeAction  close action
     * @throws IOException io exception
     * @since 1.0.0
     */
    private JarURLConnection(URL url, CustomJarFile jarFile, JarEntryName jarEntryName, CloseAction closeAction)
        throws IOException {
        // What we pass to super is ultimately ignored
        super(EMPTY_JAR_URL);
        this.url = url;
        this.jarFile = jarFile;
        this.jarEntryName = jarEntryName;
        this.closeAction = closeAction;
    }

    /**
     * Connect *
     *
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void connect() throws IOException {
        if (this.jarFile == null) {
            throw FILE_NOT_FOUND_EXCEPTION;
        }
        if (!this.jarEntryName.isEmpty() && this.jarEntry == null) {
            this.jarEntry = this.jarFile.getJarEntry(this.getEntryName());
            if (this.jarEntry == null) {
                this.throwFileNotFound(this.jarEntryName, this.jarFile);
            }
        }
        this.connected = true;
    }

    /**
     * Gets jar file *
     *
     * @return the jar file
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public CustomJarFile getJarFile() throws IOException {
        this.connect();
        return this.jarFile;
    }

    /**
     * Gets jar file url *
     *
     * @return the jar file url
     * @since 1.0.0
     */
    @Override
    public URL getJarFileURL() {
        if (this.jarFile == null) {
            throw NOT_FOUND_CONNECTION_EXCEPTION;
        }
        if (this.jarFileUrl == null) {
            this.jarFileUrl = this.buildJarFileUrl();
        }
        return this.jarFileUrl;
    }

    /**
     * Build jar file url url
     *
     * @return the url
     * @since 1.0.0
     */
    private URL buildJarFileUrl() {
        try {
            String spec = this.jarFile.getUrl().getFile();
            if (spec.endsWith(SEPARATOR)) {
                spec = spec.substring(0, spec.length() - SEPARATOR.length());
            }
            if (!spec.contains(SEPARATOR)) {
                return new URL(spec);
            }
            return new URL("jar:" + spec);
        } catch (MalformedURLException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets jar entry *
     *
     * @return the jar entry
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public CustomJarEntry getJarEntry() throws IOException {
        if (this.jarEntryName == null || this.jarEntryName.isEmpty()) {
            return null;
        }
        this.connect();
        return this.jarEntry;
    }

    /**
     * Gets entry name *
     *
     * @return the entry name
     * @since 1.0.0
     */
    @Override
    public String getEntryName() {
        if (this.jarFile == null) {
            throw NOT_FOUND_CONNECTION_EXCEPTION;
        }
        return this.jarEntryName.toString();
    }

    /**
     * Gets input stream *
     *
     * @return the input stream
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public InputStream getInputStream() throws IOException {
        if (this.jarFile == null) {
            throw FILE_NOT_FOUND_EXCEPTION;
        }
        if (this.jarEntryName.isEmpty() && this.jarFile.getType() == CustomJarFile.JarFileType.DIRECT) {
            throw new IOException("no entry name specified");
        }
        this.connect();
        InputStream inputStream = (this.jarEntryName.isEmpty() ? this.jarFile.getData().getInputStream()
            : this.jarFile.getInputStream(this.jarEntry));
        if (inputStream == null) {
            this.throwFileNotFound(this.jarEntryName, this.jarFile);
        }
        return new FilterInputStream(inputStream) {

            @Override
            public void close() throws IOException {
                super.close();
                if (JarURLConnection.this.closeAction != null) {
                    JarURLConnection.this.closeAction.perform();
                }
            }

        };
    }

    /**
     * Throw file not found *
     *
     * @param entry   entry
     * @param jarFile jar file
     * @throws FileNotFoundException file not found exception
     * @since 1.0.0
     */
    private void throwFileNotFound(Object entry, CustomJarFile jarFile) throws FileNotFoundException {
        if (Boolean.TRUE.equals(useFastExceptions.get())) {
            throw FILE_NOT_FOUND_EXCEPTION;
        }
        throw new FileNotFoundException("JAR entry " + entry + " not found in " + jarFile.getName());
    }

    /**
     * Gets content length *
     *
     * @return the content length
     * @since 1.0.0
     */
    @Override
    public int getContentLength() {
        long length = this.getContentLengthLong();
        if (length > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) length;
    }

    /**
     * Gets content length long *
     *
     * @return the content length long
     * @since 1.0.0
     */
    @Override
    public long getContentLengthLong() {
        if (this.jarFile == null) {
            return -1;
        }
        try {
            if (this.jarEntryName.isEmpty()) {
                return this.jarFile.size();
            }
            CustomJarEntry entry = this.getJarEntry();
            return (entry != null) ? (int) entry.getSize() : -1;
        } catch (IOException ex) {
            return -1;
        }
    }

    /**
     * Gets content *
     *
     * @return the content
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Object getContent() throws IOException {
        this.connect();
        return this.jarEntryName.isEmpty() ? this.jarFile : super.getContent();
    }

    /**
     * Gets content type *
     *
     * @return the content type
     * @since 1.0.0
     */
    @Override
    public String getContentType() {
        return (this.jarEntryName != null) ? this.jarEntryName.getContentType() : null;
    }

    /**
     * Gets permission *
     *
     * @return the permission
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Permission getPermission() throws IOException {
        if (this.jarFile == null) {
            throw FILE_NOT_FOUND_EXCEPTION;
        }
        if (this.permission == null) {
            this.permission = new FilePermission(this.jarFile.getRootJarFile().getFile().getPath(), READ_ACTION);
        }
        return this.permission;
    }

    /**
     * Gets last modified *
     *
     * @return the last modified
     * @since 1.0.0
     */
    @Override
    public long getLastModified() {
        if (this.jarFile == null || this.jarEntryName.isEmpty()) {
            return 0;
        }
        try {
            CustomJarEntry entry = this.getJarEntry();
            return (entry != null) ? entry.getTime() : 0;
        } catch (IOException ex) {
            return 0;
        }
    }

    /**
     * Sets use fast exceptions *
     *
     * @param useFastExceptions use fast exceptions
     * @since 1.0.0
     */
    static void setUseFastExceptions(boolean useFastExceptions) {
        JarURLConnection.useFastExceptions.set(useFastExceptions);
    }

    /**
     * Get jar url connection
     *
     * @param url     url
     * @param jarFile jar file
     * @return the jar url connection
     * @throws IOException io exception
     * @since 1.0.0
     */
    static JarURLConnection get(URL url, CustomJarFile jarFile) throws IOException {
        StringSequence spec = new StringSequence(url.getFile());
        int index = indexOfRootSpec(spec, jarFile.getPathFromRoot());
        if (index == -1) {
            return (Boolean.TRUE.equals(useFastExceptions.get()) ? NOT_FOUND_CONNECTION
                : new JarURLConnection(url, null, EMPTY_JAR_ENTRY_NAME, null));
        }
        int separator;
        CustomJarFile connectionJarFile = jarFile;
        while ((separator = spec.indexOf(SEPARATOR, index)) > 0) {
            JarEntryName entryName = JarEntryName.get(spec.subSequence(index, separator));
            CustomJarEntry jarEntry = jarFile.getJarEntry(entryName.toCharSequence());
            if (jarEntry == null) {
                return JarURLConnection.notFound(connectionJarFile, entryName,
                    (connectionJarFile != jarFile) ? connectionJarFile::close : null);
            }
            connectionJarFile = connectionJarFile.getNestedJarFile(jarEntry);
            index = separator + SEPARATOR.length();
        }
        JarEntryName jarEntryName = JarEntryName.get(spec, index);
        if (Boolean.TRUE.equals(useFastExceptions.get()) && !jarEntryName.isEmpty()
            && !connectionJarFile.containsEntry(jarEntryName.toString())) {
            if (connectionJarFile != jarFile) {
                connectionJarFile.close();
            }
            return NOT_FOUND_CONNECTION;
        }
        return new JarURLConnection(url, connectionJarFile, jarEntryName,
            (connectionJarFile != jarFile) ? connectionJarFile::close : null);
    }

    /**
     * Index of root spec int
     *
     * @param file         file
     * @param pathFromRoot path from root
     * @return the int
     * @since 1.0.0
     */
    private static int indexOfRootSpec(StringSequence file, String pathFromRoot) {
        int separatorIndex = file.indexOf(SEPARATOR);
        if (separatorIndex < 0 || !file.startsWith(pathFromRoot, separatorIndex)) {
            return -1;
        }
        return separatorIndex + SEPARATOR.length() + pathFromRoot.length();
    }

    /**
     * Not found jar url connection
     *
     * @return the jar url connection
     * @since 1.0.0
     */
    private static JarURLConnection notFound() {
        try {
            return notFound(null, null, null);
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Not found jar url connection
     *
     * @param jarFile      jar file
     * @param jarEntryName jar entry name
     * @param closeAction  close action
     * @return the jar url connection
     * @throws IOException io exception
     * @since 1.0.0
     */
    private static JarURLConnection notFound(CustomJarFile jarFile, JarEntryName jarEntryName, CloseAction closeAction)
        throws IOException {
        if (Boolean.TRUE.equals(useFastExceptions.get())) {
            if (closeAction != null) {
                closeAction.perform();
            }
            return NOT_FOUND_CONNECTION;
        }
        return new JarURLConnection(null, jarFile, jarEntryName, closeAction);
    }

    /**
     * A JarEntryName parsed from a URL String.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @since 1.0.0
     */
    static class JarEntryName {

        /** Name */
        private final StringSequence name;

        /** Content type */
        private String contentType;

        /**
         * Jar entry name
         *
         * @param spec spec
         * @since 1.0.0
         */
        JarEntryName(StringSequence spec) {
            this.name = this.decode(spec);
        }

        /**
         * Decode string sequence
         *
         * @param source source
         * @return the string sequence
         * @since 1.0.0
         */
        private StringSequence decode(StringSequence source) {
            if (source.isEmpty() || (source.indexOf('%') < 0)) {
                return source;
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream(source.length());
            this.write(source.toString(), bos);
            // AsciiBytes is what is used to store the JarEntries so make it symmetric
            return new StringSequence(AsciiBytes.toString(bos.toByteArray()));
        }

        /**
         * Write *
         *
         * @param source       source
         * @param outputStream output stream
         * @since 1.0.0
         */
        private void write(String source, ByteArrayOutputStream outputStream) {
            int length = source.length();
            for (int i = 0; i < length; i++) {
                int c = source.charAt(i);
                if (c > 127) {
                    try {
                        String encoded = URLEncoder.encode(String.valueOf((char) c), "UTF-8");
                        this.write(encoded, outputStream);
                    } catch (UnsupportedEncodingException ex) {
                        throw new IllegalStateException(ex);
                    }
                } else {
                    if (c == '%') {
                        if ((i + 2) >= length) {
                            throw new IllegalArgumentException(
                                "Invalid encoded sequence \"" + source.substring(i) + "\"");
                        }
                        c = this.decodeEscapeSequence(source, i);
                        i += 2;
                    }
                    outputStream.write(c);
                }
            }
        }

        /**
         * Decode escape sequence char
         *
         * @param source source
         * @param i
         * @return the char
         * @since 1.0.0
         */
        private char decodeEscapeSequence(String source, int i) {
            int hi = Character.digit(source.charAt(i + 1), 16);
            int lo = Character.digit(source.charAt(i + 2), 16);
            if (hi == -1 || lo == -1) {
                throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
            }
            return ((char) ((hi << 4) + lo));
        }

        /**
         * To char sequence char sequence
         *
         * @return the char sequence
         * @since 1.0.0
         */
        CharSequence toCharSequence() {
            return this.name;
        }

        /**
         * To string string
         *
         * @return the string
         * @since 1.0.0
         */
        @Override
        public String toString() {
            return this.name.toString();
        }

        /**
         * Is empty boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        boolean isEmpty() {
            return this.name.isEmpty();
        }

        /**
         * Gets content type *
         *
         * @return the content type
         * @since 1.0.0
         */
        String getContentType() {
            if (this.contentType == null) {
                this.contentType = this.deduceContentType();
            }
            return this.contentType;
        }

        /**
         * Deduce content type string
         *
         * @return the string
         * @since 1.0.0
         */
        private String deduceContentType() {
            // Guess the content type, don't bother with streams as mark is not supported
            String type = this.isEmpty() ? "x-java/jar" : null;
            type = (type != null) ? type : guessContentTypeFromName(this.toString());
            type = (type != null) ? type : "content/unknown";
            return type;
        }

        /**
         * Get jar entry name
         *
         * @param spec spec
         * @return the jar entry name
         * @since 1.0.0
         */
        static JarEntryName get(StringSequence spec) {
            return get(spec, 0);
        }

        /**
         * Get jar entry name
         *
         * @param spec       spec
         * @param beginIndex begin index
         * @return the jar entry name
         * @since 1.0.0
         */
        static JarEntryName get(StringSequence spec, int beginIndex) {
            if (spec.length() <= beginIndex) {
                return EMPTY_JAR_ENTRY_NAME;
            }
            return new JarEntryName(spec.subSequence(beginIndex));
        }

    }

    /**
     * An action to be taken when the connection is being "closed" and its underlying
     * resources are no longer needed.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:48
     * @since 1.0.0
     */
    @FunctionalInterface
    private interface CloseAction {

        /**
         * Perform *
         *
         * @throws IOException io exception
         * @since 1.0.0
         */
        void perform() throws IOException;

    }

}
