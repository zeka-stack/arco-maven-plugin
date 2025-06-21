package dev.dong4j.arco.maven.plugin.boot.loader.archive;

import dev.dong4j.arco.maven.plugin.boot.loader.jar.CustomJarFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by a {@link CustomJarFile}.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:49
 * @since 1.0.0
 */
public class JarFileArchive implements Archive {

    /** UNPACK_MARKER */
    private static final String UNPACK_MARKER = "UNPACK:";

    /** BUFFER_SIZE */
    private static final int BUFFER_SIZE = 32 * 1024;

    /** Jar file */
    private final CustomJarFile jarFile;

    /** Url */
    private URL url;

    /** Temp unpack folder */
    private File tempUnpackFolder;

    /**
     * Jar file archive
     *
     * @param file file
     * @throws IOException io exception
     * @since 1.0.0
     */
    public JarFileArchive(File file) throws IOException {
        this(file, file.toURI().toURL());
    }

    /**
     * Jar file archive
     *
     * @param file file
     * @param url  url
     * @throws IOException io exception
     * @since 1.0.0
     */
    public JarFileArchive(File file, URL url) throws IOException {
        this(new CustomJarFile(file));
        this.url = url;
    }

    /**
     * Jar file archive
     *
     * @param jarFile jar file
     * @since 1.0.0
     */
    public JarFileArchive(CustomJarFile jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Gets url *
     *
     * @return the url
     * @throws MalformedURLException malformed url exception
     * @since 1.0.0
     */
    @Override
    public URL getUrl() throws MalformedURLException {
        if (this.url != null) {
            return this.url;
        }
        return this.jarFile.getUrl();
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
        return this.jarFile.getManifest();
    }

    /**
     * Gets nested archives *
     *
     * @param filter filter
     * @return the nested archives
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public List<Archive> getNestedArchives(EntryFilter filter) throws IOException {
        List<Archive> nestedArchives = new ArrayList<>();
        for (Entry entry : this) {
            if (filter.matches(entry)) {
                nestedArchives.add(this.getNestedArchive(entry));
            }
        }
        return Collections.unmodifiableList(nestedArchives);
    }

    /**
     * Iterator iterator
     *
     * @return the iterator
     * @since 1.0.0
     */
    @Override
    public Iterator<Entry> iterator() {
        return new EntryIterator(this.jarFile.entries());
    }

    /**
     * Close *
     *
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void close() throws IOException {
        this.jarFile.close();
    }

    /**
     * Gets nested archive *
     *
     * @param entry entry
     * @return the nested archive
     * @throws IOException io exception
     * @since 1.0.0
     */
    protected Archive getNestedArchive(Entry entry) throws IOException {
        JarEntry jarEntry = ((JarFileEntry) entry).getJarEntry();
        if (jarEntry.getComment().startsWith(UNPACK_MARKER)) {
            return this.getUnpackedNestedArchive(jarEntry);
        }
        try {
            return new JarFileArchive(this.jarFile.getNestedJarFile(jarEntry));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to get nested archive for entry " + entry.getName(), ex);
        }
    }

    /**
     * Gets unpacked nested archive *
     *
     * @param jarEntry jar entry
     * @return the unpacked nested archive
     * @throws IOException io exception
     * @since 1.0.0
     */
    private Archive getUnpackedNestedArchive(JarEntry jarEntry) throws IOException {
        String name = jarEntry.getName();
        if (name.lastIndexOf('/') != -1) {
            name = name.substring(name.lastIndexOf('/') + 1);
        }
        File file = new File(this.getTempUnpackFolder(), name);
        if (!file.exists() || file.length() != jarEntry.getSize()) {
            this.unpack(jarEntry, file);
        }
        return new JarFileArchive(file, file.toURI().toURL());
    }

    /**
     * Gets temp unpack folder *
     *
     * @return the temp unpack folder
     * @since 1.0.0
     */
    private File getTempUnpackFolder() {
        if (this.tempUnpackFolder == null) {
            File tempFolder = new File(System.getProperty("java.io.tmpdir"));
            this.tempUnpackFolder = this.createUnpackFolder(tempFolder);
        }
        return this.tempUnpackFolder;
    }

    /**
     * Create unpack folder file
     *
     * @param parent parent
     * @return the file
     * @since 1.0.0
     */
    private File createUnpackFolder(File parent) {
        int attempts = 0;
        while (attempts++ < 1000) {
            String fileName = new File(this.jarFile.getName()).getName();
            File unpackFolder = new File(parent, fileName + "-spring-boot-libs-" + UUID.randomUUID());
            if (unpackFolder.mkdirs()) {
                return unpackFolder;
            }
        }
        throw new IllegalStateException("Failed to create unpack folder in directory '" + parent + "'");
    }

    /**
     * Unpack *
     *
     * @param entry entry
     * @param file  file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void unpack(JarEntry entry, File file) throws IOException {
        try (InputStream inputStream = this.jarFile.getInputStream(entry);
             OutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        }
    }

    /**
     * To string string
     *
     * @return the string
     * @since 1.0.0
     */
    @Override
    public String toString() {
        try {
            return this.getUrl().toString();
        } catch (Exception ex) {
            return "jar archive";
        }
    }

    /**
     * {@link Archive.Entry} iterator implementation backed by {@link JarEntry}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private static class EntryIterator implements Iterator<Entry> {

        /** Enumeration */
        private final Enumeration<JarEntry> enumeration;

        /**
         * Entry iterator
         *
         * @param enumeration enumeration
         * @since 1.0.0
         */
        EntryIterator(Enumeration<JarEntry> enumeration) {
            this.enumeration = enumeration;
        }

        /**
         * Has next boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean hasNext() {
            return this.enumeration.hasMoreElements();
        }

        /**
         * Next entry
         *
         * @return the entry
         * @since 1.0.0
         */
        @Override
        @SuppressWarnings("java:S2272")
        public Entry next() {
            return new JarFileEntry(this.enumeration.nextElement());
        }

        /**
         * Remove
         *
         * @since 1.0.0
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("remove");
        }

    }

    /**
     * {@link Archive.Entry} implementation backed by a {@link JarEntry}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private static class JarFileEntry implements Entry {

        /** Jar entry */
        private final JarEntry jarEntry;

        /**
         * Jar file entry
         *
         * @param jarEntry jar entry
         * @since 1.0.0
         */
        JarFileEntry(JarEntry jarEntry) {
            this.jarEntry = jarEntry;
        }

        /**
         * Gets jar entry *
         *
         * @return the jar entry
         * @since 1.0.0
         */
        JarEntry getJarEntry() {
            return this.jarEntry;
        }

        /**
         * Is directory boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean isDirectory() {
            return this.jarEntry.isDirectory();
        }

        /**
         * Gets name *
         *
         * @return the name
         * @since 1.0.0
         */
        @Override
        public String getName() {
            return this.jarEntry.getName();
        }

    }

}
