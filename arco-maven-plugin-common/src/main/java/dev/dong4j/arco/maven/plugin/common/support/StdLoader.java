package dev.dong4j.arco.maven.plugin.common.support;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.jar.JarFile;

/**
 * 标准的资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class StdLoader extends ResourceLoader implements Loader {
    /** Class loader */
    private final ClassLoader classLoader;

    /**
     * Std loader
     *
     * @since 1.0.0
     */
    public StdLoader() {
        this(Thread.currentThread().getContextClassLoader() != null ? Thread.currentThread().getContextClassLoader() :
            ClassLoader.getSystemClassLoader());
    }

    /**
     * Std loader
     *
     * @param classLoader class loader
     * @since 1.0.0
     */
    public StdLoader(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader must not be null");
        }
        this.classLoader = classLoader;
    }

    /**
     * Load enumeration
     *
     * @param path        path
     * @param recursively recursively
     * @param filter      filter
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path, boolean recursively, Filter filter) throws IOException {
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        while (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return new Enumerator(this.classLoader, path, recursively, filter != null ? filter : Filters.ALWAYS);
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
        /** Path */
        private final String path;
        /** Recursively */
        private final boolean recursively;
        /** Filter */
        private final Filter filter;
        /** Urls */
        private final Enumeration<URL> urls;
        /** Resources */
        private Enumeration<Resource> resources;

        /**
         * Enumerator
         *
         * @param classLoader class loader
         * @param path        path
         * @param recursively recursively
         * @param filter      filter
         * @throws IOException io exception
         * @since 1.0.0
         */
        Enumerator(ClassLoader classLoader, String path, boolean recursively, Filter filter) throws IOException {
            this.path = path;
            this.recursively = recursively;
            this.filter = filter;
            this.urls = this.load(classLoader, path);
            this.resources = Collections.enumeration(Collections.<Resource>emptySet());
        }

        /**
         * Load enumeration
         *
         * @param classLoader class loader
         * @param path        path
         * @return the enumeration
         * @throws IOException io exception
         * @since 1.0.0
         */
        private Enumeration<URL> load(ClassLoader classLoader, String path) throws IOException {
            if (path.length() > 0) {
                return classLoader.getResources(path);
            } else {
                Set<URL> set = new LinkedHashSet<>();
                set.add(classLoader.getResource(path));
                Enumeration<URL> urls = classLoader.getResources("META-INF/");
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    if (url.getProtocol().equalsIgnoreCase("jar")) {
                        String spec = url.toString();
                        int index = spec.lastIndexOf("!/");
                        if (index < 0) {
                            continue;
                        }
                        set.add(new URL(url, spec.substring(0, index + "!/".length())));
                    }
                }
                return Collections.enumeration(set);
            }
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
            } else if (!this.resources.hasMoreElements() && !this.urls.hasMoreElements()) {
                return false;
            } else if (this.resources.hasMoreElements()) {
                this.next = this.resources.nextElement();
                return true;
            } else {
                URL url = this.urls.nextElement();
                String protocol = url.getProtocol();
                if ("file".equalsIgnoreCase(protocol)) {
                    try {
                        String uri = Uris.decode(url.getPath(), Charset.defaultCharset());
                        String root = uri.substring(0, uri.lastIndexOf(this.path));
                        URL context = new URL(url, "file:" + Uris.encodePath(root, Charset.defaultCharset()));
                        File file = new File(root);
                        this.resources = new FileLoader(context, file).load(this.path, this.recursively, this.filter);
                        return this.hasMoreElements();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                } else if ("jar".equalsIgnoreCase(protocol)) {
                    try {
                        String uri = Uris.decode(url.getPath(), Charset.defaultCharset());
                        String root = uri.substring(0, uri.lastIndexOf(this.path));
                        URL context = new URL(url, "jar:" + Uris.encodePath(root, Charset.defaultCharset()));
                        JarURLConnection jarURLConnection = (JarURLConnection) url.openConnection();
                        JarFile jarFile = jarURLConnection.getJarFile();
                        this.resources = new JarLoader(context, jarFile).load(this.path, this.recursively, this.filter);
                        return this.hasMoreElements();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                } else {
                    return this.hasMoreElements();
                }
            }
        }
    }

}
