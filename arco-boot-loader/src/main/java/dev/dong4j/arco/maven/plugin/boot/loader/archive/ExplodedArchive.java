package dev.dong4j.arco.maven.plugin.boot.loader.archive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.jar.Manifest;

/**
 * {@link Archive} implementation backed by an exploded archive directory.
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:49
 * @since 1.0.0
 */
public class ExplodedArchive implements Archive {

    /** SKIPPED_NAMES */
    private static final Set<String> SKIPPED_NAMES = new HashSet<>(Arrays.asList(".", ".."));

    /** Root */
    private final File root;

    /** Recursive */
    private final boolean recursive;

    /** Manifest file */
    private final File manifestFile;

    /** Manifest */
    private Manifest manifest;

    /**
     * Create a new {@link ExplodedArchive} instance.
     *
     * @param root the root folder
     * @since 1.0.0
     */
    public ExplodedArchive(File root) {
        this(root, true);
    }

    /**
     * Create a new {@link ExplodedArchive} instance.
     *
     * @param root      the root folder
     * @param recursive if recursive searching should be used to locate the manifest.                  Defaults to {@code true}, folders
     *                  with a large tree might want to set this to                  {@code
     *                  false}.
     * @since 1.0.0
     */
    public ExplodedArchive(File root, boolean recursive) {
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("Invalid source folder " + root);
        }
        this.root = root;
        this.recursive = recursive;
        this.manifestFile = this.getManifestFile(root);
    }

    /**
     * Gets manifest file *
     *
     * @param root root
     * @return the manifest file
     * @since 1.0.0
     */
    private File getManifestFile(File root) {
        File metaInf = new File(root, "META-INF");
        return new File(metaInf, "MANIFEST.MF");
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
        return this.root.toURI().toURL();
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
        if (this.manifest == null && this.manifestFile.exists()) {
            try (FileInputStream inputStream = new FileInputStream(this.manifestFile)) {
                this.manifest = new Manifest(inputStream);
            }
        }
        return this.manifest;
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
        return new FileEntryIterator(this.root, this.recursive);
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
        File file = ((FileEntry) entry).getFile();
        return (file.isDirectory() ? new ExplodedArchive(file) : new JarFileArchive(file));
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
            return "exploded archive";
        }
    }

    /**
     * File based {@link Entry} {@link Iterator}.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private static class FileEntryIterator implements Iterator<Entry> {

        /** Entry comparator */
        private final Comparator<File> entryComparator = new EntryComparator();

        /** Root */
        private final File root;

        /** Recursive */
        private final boolean recursive;

        /** Stack */
        private final Deque<Iterator<File>> stack = new LinkedList<>();

        /** Current */
        private File current;

        /**
         * File entry iterator
         *
         * @param root      root
         * @param recursive recursive
         * @since 1.0.0
         */
        FileEntryIterator(File root, boolean recursive) {
            this.root = root;
            this.recursive = recursive;
            this.stack.add(this.listFiles(root));
            this.current = this.poll();
        }

        /**
         * Has next boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean hasNext() {
            return this.current != null;
        }

        /**
         * Next entry
         *
         * @return the entry
         * @since 1.0.0
         */
        @Override
        public Entry next() {
            if (this.current == null) {
                throw new NoSuchElementException();
            }
            File file = this.current;
            if (file.isDirectory() && (this.recursive || file.getParentFile().equals(this.root))) {
                this.stack.addFirst(this.listFiles(file));
            }
            this.current = this.poll();
            String name = file.toURI().getPath().substring(this.root.toURI().getPath().length());
            return new FileEntry(name, file);
        }

        /**
         * List files iterator
         *
         * @param file file
         * @return the iterator
         * @since 1.0.0
         */
        private Iterator<File> listFiles(File file) {
            File[] files = file.listFiles();
            if (files == null) {
                return Collections.emptyIterator();
            }
            Arrays.sort(files, this.entryComparator);
            return Arrays.asList(files).iterator();
        }

        /**
         * Poll file
         *
         * @return the file
         * @since 1.0.0
         */
        private File poll() {
            while (!this.stack.isEmpty()) {
                while (this.stack.peek().hasNext()) {
                    File file = this.stack.peek().next();
                    if (!SKIPPED_NAMES.contains(file.getName())) {
                        return file;
                    }
                }
                this.stack.poll();
            }
            return null;
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

        /**
         * {@link Comparator} that orders {@link File} entries by their absolute paths.
         *
         * @author dong4j
         * @version 1.0.0
         * @email "mailto:dong4j@gmail.com"
         * @date 2020.04.30 15:49
         * @since 1.0.0
         */
        private static class EntryComparator implements Comparator<File> {

            /**
             * Compare int
             *
             * @param o1 o 1
             * @param o2 o 2
             * @return the int
             * @since 1.0.0
             */
            @Override
            public int compare(File o1, File o2) {
                return o1.getAbsolutePath().compareTo(o2.getAbsolutePath());
            }

        }

    }

    /**
     * {@link Entry} backed by a File.
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:49
     * @since 1.0.0
     */
    private static class FileEntry implements Entry {

        /** Name */
        private final String name;

        /** File */
        private final File file;

        /**
         * File entry
         *
         * @param name name
         * @param file file
         * @since 1.0.0
         */
        FileEntry(String name, File file) {
            this.name = name;
            this.file = file;
        }

        /**
         * Gets file *
         *
         * @return the file
         * @since 1.0.0
         */
        File getFile() {
            return this.file;
        }

        /**
         * Is directory boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean isDirectory() {
            return this.file.isDirectory();
        }

        /**
         * Gets name *
         *
         * @return the name
         * @since 1.0.0
         */
        @Override
        public String getName() {
            return this.name;
        }

    }

}
