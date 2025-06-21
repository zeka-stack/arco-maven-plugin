package dev.dong4j.arco.maven.plugin.common.support;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Jar包资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class JarLoader extends ResourceLoader implements Loader {
    /** Context */
    private final URL context;
    /** Jar file */
    private final JarFile jarFile;

    /**
     * Jar loader
     *
     * @param file file
     * @throws IOException io exception
     * @since 1.0.0
     */
    public JarLoader(File file) throws IOException {
        this(new URL("jar:" + file.toURI().toURL() + "!/"), new JarFile(file));
    }

    /**
     * Jar loader
     *
     * @param jarUrl jar url
     * @throws IOException io exception
     * @since 1.0.0
     */
    public JarLoader(URL jarUrl) throws IOException {
        this(jarUrl, ((JarURLConnection) jarUrl.openConnection()).getJarFile());
    }

    /**
     * Jar loader
     *
     * @param context context
     * @param jarFile jar file
     * @since 1.0.0
     */
    public JarLoader(URL context, JarFile jarFile) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (jarFile == null) {
            throw new IllegalArgumentException("jarFile must not be null");
        }
        this.context = context;
        this.jarFile = jarFile;
    }

    /**
     * Load enumeration
     *
     * @param path        path
     * @param recursively recursively
     * @param filter      filter
     * @return the enumeration
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path, boolean recursively, Filter filter) {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return new Enumerator(this.context, this.jarFile, path, recursively, filter != null ? filter : Filters.ALWAYS);
    }

    /**
     * <p>Description: >/p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 01:02
     * @since 1.0.0
     */
    private static class Enumerator extends ResourceEnumerator implements Enumeration<Resource> {
        /** Context */
        private final URL context;
        /** Path */
        private final String path;
        /** Folder */
        private final String folder;
        /** Recursively */
        private final boolean recursively;
        /** Filter */
        private final Filter filter;
        /** Entries */
        private final Enumeration<JarEntry> entries;

        /**
         * Enumerator
         *
         * @param context     context
         * @param jarFile     jar file
         * @param path        path
         * @param recursively recursively
         * @param filter      filter
         * @since 1.0.0
         */
        Enumerator(URL context, JarFile jarFile, String path, boolean recursively, Filter filter) {
            this.context = context;
            this.path = path;
            this.folder = path.endsWith("/") || path.length() == 0 ? path : path + "/";
            this.recursively = recursively;
            this.filter = filter;
            this.entries = jarFile.entries();
        }

        /**
         * Has more elements boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean hasMoreElements() {
            if (this.next != null) {
                return true;
            }
            while (this.entries.hasMoreElements()) {
                JarEntry jarEntry = this.entries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String name = jarEntry.getName();
                if (name.equals(this.path)
                    || (this.recursively && name.startsWith(this.folder))
                    || (!this.recursively && name.startsWith(this.folder) && name.indexOf('/', this.folder.length()) < 0)) {
                    try {
                        URL url = new URL(this.context, Uris.encodePath(name, Charset.defaultCharset()));
                        if (this.filter.filtrate(name, url)) {
                            this.next = new Res(name, url);
                            return true;
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            return false;
        }

    }

}
