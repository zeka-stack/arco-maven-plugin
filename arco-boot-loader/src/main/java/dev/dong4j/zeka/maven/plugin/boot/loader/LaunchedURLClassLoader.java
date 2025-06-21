package dev.dong4j.zeka.maven.plugin.boot.loader;

import dev.dong4j.zeka.maven.plugin.boot.loader.jar.CustomJarFile;
import dev.dong4j.zeka.maven.plugin.boot.loader.jar.Handler;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.jar.JarFile;

/**
 * 通过扩展的 jar 协议, 以实现 jar in jar 这种情况下的 class 文件加载
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:44
 * @since 1.3.0
 */
@SuppressWarnings("all")
public class LaunchedURLClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    /**
     * Create a new {@link LaunchedURLClassLoader} instance.
     *
     * @param urls   the URLs from which to load classes and resources
     * @param parent the parent class loader for delegation
     * @since 1.0.0
     */
    public LaunchedURLClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    /**
     * Find resource url
     *
     * @param name name
     * @return the url
     * @since 1.0.0
     */
    @Override
    public URL findResource(String name) {
        Handler.setUseFastConnectionExceptions(true);
        try {
            return super.findResource(name);
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }

    /**
     * Find resources enumeration
     *
     * @param name name
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        Handler.setUseFastConnectionExceptions(true);
        try {
            return new UseFastConnectionExceptionsEnumeration(super.findResources(name));
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }

    /**
     * Load class class
     *
     * @param name    name
     * @param resolve resolve
     * @return the class
     * @throws ClassNotFoundException class not found exception
     * @since 1.0.0
     */
    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Handler.setUseFastConnectionExceptions(true);
        try {
            try {
                this.definePackageIfNecessary(name);
            } catch (IllegalArgumentException ex) {
                // Tolerate race condition due to being parallel capable
                if (this.getPackage(name) == null) {
                    // This should never happen as the IllegalArgumentException indicates
                    // that the package has already been defined and, therefore,
                    // getPackage(name) should not return null.
                    throw new AssertionError("Package " + name + " has already been defined but it could not be found");
                }
            }
            return super.loadClass(name, resolve);
        } finally {
            Handler.setUseFastConnectionExceptions(false);
        }
    }

    /**
     * Define a package before a {@code findClass} call is made. This is necessary to
     * ensure that the appropriate manifest for nested JARs is associated with the
     * package.
     *
     * @param className the class name being found
     * @since 1.0.0
     */
    private void definePackageIfNecessary(@NotNull String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            String packageName = className.substring(0, lastDot);
            if (this.getPackage(packageName) == null) {
                try {
                    this.definePackage(className, packageName);
                } catch (IllegalArgumentException ex) {
                    // Tolerate race condition due to being parallel capable
                    if (this.getPackage(packageName) == null) {
                        // This should never happen as the IllegalArgumentException
                        // indicates that the package has already been defined and,
                        // therefore, getPackage(name) should not have returned null.
                        throw new AssertionError(
                            "Package " + packageName + " has already been defined but it could not be found");
                    }
                }
            }
        }
    }

    /**
     * Define package *
     *
     * @param className   class name
     * @param packageName package name
     * @since 1.0.0
     */
    private void definePackage(String className, String packageName) {
        try {
            AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> {
                String packageEntryName = packageName.replace('.', '/') + "/";
                String classEntryName = className.replace('.', '/') + ".class";
                for (URL url : this.getURLs()) {
                    try {
                        URLConnection connection = url.openConnection();
                        if (connection instanceof JarURLConnection) {
                            JarFile jarFile = ((JarURLConnection) connection).getJarFile();
                            if (jarFile.getEntry(classEntryName) != null && jarFile.getEntry(packageEntryName) != null
                                && jarFile.getManifest() != null) {
                                this.definePackage(packageName, jarFile.getManifest(), url);
                                return null;
                            }
                        }
                    } catch (IOException ex) {
                        // Ignore
                    }
                }
                return null;
            }, AccessController.getContext());
        } catch (java.security.PrivilegedActionException ex) {
            // Ignore
        }
    }

    /**
     * Clear URL caches.
     *
     * @since 1.0.0
     */
    public void clearCache() {
        for (URL url : this.getURLs()) {
            try {
                URLConnection connection = url.openConnection();
                if (connection instanceof JarURLConnection) {
                    this.clearCache(connection);
                }
            } catch (IOException ex) {
                // Ignore
            }
        }

    }

    /**
     * Clear cache *
     *
     * @param connection connection
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void clearCache(URLConnection connection) throws IOException {
        Object jarFile = ((JarURLConnection) connection).getJarFile();
        if (jarFile instanceof CustomJarFile) {
            ((CustomJarFile) jarFile).clearCache();
        }
    }

    /**
     * <p>Description: >/p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 15:44
     * @since 1.0.0
     */
    private static class UseFastConnectionExceptionsEnumeration implements Enumeration<URL> {

        /** Delegate */
        private final Enumeration<URL> delegate;

        /**
         * Use fast connection exceptions enumeration
         *
         * @param delegate delegate
         * @since 1.0.0
         */
        UseFastConnectionExceptionsEnumeration(Enumeration<URL> delegate) {
            this.delegate = delegate;
        }

        /**
         * Has more elements boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        public boolean hasMoreElements() {
            Handler.setUseFastConnectionExceptions(true);
            try {
                return this.delegate.hasMoreElements();
            } finally {
                Handler.setUseFastConnectionExceptions(false);
            }

        }

        /**
         * Next element url
         *
         * @return the url
         * @since 1.0.0
         */
        @Override
        public URL nextElement() {
            Handler.setUseFastConnectionExceptions(true);
            try {
                return this.delegate.nextElement();
            } finally {
                Handler.setUseFastConnectionExceptions(false);
            }
        }

    }

}
