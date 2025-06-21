package dev.dong4j.zeka.maven.plugin.boot.loader.jar;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * 扩展 {@link URLStreamHandler} 已以实现 jar in jar 中的资源加载, {@link CustomJarFile}.
 * 原始的 JarFile URL 只支持一个 '!/', 比如: jar:file:/tmp/target/demo-0.0.1-SNAPSHOT.jar!/com/example/SpringBootDemoApplication.class
 * 这里扩展为支持多个 '!/', 用于处理 jar in jar 的问题:
 * jar:file:/tmp/target/demo-0.0.1-SNAPSHOT.jar!/lib/spring-boot-2.2.1.RELEASE.jar!/META-INF/MANIFEST.MF
 * <p>
 * 为了被 JVM 加载为一个 URL protocol handler, 需要满足:
 * 1. 必须是 public
 * 2. 类名必须为 Handler
 * 3. 必须在 jar 包下
 * JDk 或 ClassLoader 读取资源的流程:
 * 1. LaunchedURLClassLoader.loadClass
 * 2. URL.getContent()
 * 3. URL.openConnection()
 * 4. Handler.openConnection(URL)
 * 5. JarURLConnection.getInputStream
 *
 * @author Phillip Webb
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 15:48
 * @see CustomJarFile#registerUrlProtocolHandler() JarFile#registerUrlProtocolHandler()
 * @since 1.0.0
 */
@Slf4j
public class Handler extends URLStreamHandler {

    /** JAR_PROTOCOL */
    private static final String JAR_PROTOCOL = "jar:";

    /** FILE_PROTOCOL */
    private static final String FILE_PROTOCOL = "file:";

    /** SEPARATOR */
    private static final String SEPARATOR = "!/";

    /** CURRENT_DIR */
    private static final String CURRENT_DIR = "/./";

    /** CURRENT_DIR_PATTERN */
    private static final Pattern CURRENT_DIR_PATTERN = Pattern.compile(CURRENT_DIR, Pattern.LITERAL);

    /** PARENT_DIR */
    private static final String PARENT_DIR = "/../";

    /** FALLBACK_HANDLERS */
    private static final String[] FALLBACK_HANDLERS = {"sun.net.www.protocol.jar.Handler"};

    /** Root file cache */
    private static SoftReference<Map<File, CustomJarFile>> rootFileCache;

    static {
        // 使用 SoftReference 保存打开过的 JarFile
        rootFileCache = new SoftReference<>(null);
    }

    /** Jar file */
    private final CustomJarFile jarFile;

    /** Fallback handler */
    private URLStreamHandler fallbackHandler;

    /**
     * Handler
     *
     * @since 1.0.0
     */
    public Handler() {
        this(null);
    }

    /**
     * Handler
     *
     * @param jarFile jar file
     * @since 1.0.0
     */
    public Handler(CustomJarFile jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * Open connection url connection
     *
     * @param url url
     * @return the url connection
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
        if (this.jarFile != null && this.isUrlInJarFile(url, this.jarFile)) {
            return JarURLConnection.get(url, this.jarFile);
        }
        try {
            return JarURLConnection.get(url, this.getRootJarFileFromUrl(url));
        } catch (Exception ex) {
            return this.openFallbackConnection(url, ex);
        }
    }

    /**
     * Is url in jar file boolean
     *
     * @param url     url
     * @param jarFile jar file
     * @return the boolean
     * @throws MalformedURLException malformed url exception
     * @since 1.0.0
     */
    private boolean isUrlInJarFile(URL url, CustomJarFile jarFile) throws MalformedURLException {
        // Try the path first to save building a new url string each time
        return url.getPath().startsWith(jarFile.getUrl().getPath())
            && url.toString().startsWith(jarFile.getUrlString());
    }

    /**
     * Open fallback connection url connection
     *
     * @param url    url
     * @param reason reason
     * @return the url connection
     * @throws IOException io exception
     * @since 1.0.0
     */
    private URLConnection openFallbackConnection(URL url, Exception reason) throws IOException {
        try {
            return this.openConnection(this.getFallbackHandler(), url);
        } catch (Exception ex) {
            if (reason instanceof IOException) {
                this.log(false, "Unable to open fallback handler", ex);
                throw (IOException) reason;
            }
            this.log(true, "Unable to open fallback handler", ex);
            if (reason instanceof RuntimeException) {
                throw (RuntimeException) reason;
            }
            throw new IllegalStateException(reason);
        }
    }

    /**
     * Log *
     *
     * @param warning warning
     * @param message message
     * @param cause   cause
     * @since 1.0.0
     */
    @SuppressWarnings("all")
    private void log(boolean warning, String message, Exception cause) {
        try {
            Level level = warning ? Level.WARNING : Level.FINEST;
            Logger.getLogger(this.getClass().getName()).log(level, message, cause);
        } catch (Exception ex) {
            if (warning) {
                System.err.println("WARNING: " + message);
            }
        }
    }

    /**
     * Gets fallback handler *
     *
     * @return the fallback handler
     * @since 1.0.0
     */
    private URLStreamHandler getFallbackHandler() {
        if (this.fallbackHandler != null) {
            return this.fallbackHandler;
        }
        for (String handlerClassName : FALLBACK_HANDLERS) {
            try {
                Class<?> handlerClass = Class.forName(handlerClassName);
                this.fallbackHandler = (URLStreamHandler) handlerClass.newInstance();
                return this.fallbackHandler;
            } catch (Exception ignored) {
                // nothing to do
            }
        }
        throw new IllegalStateException("Unable to find fallback handler");
    }

    /**
     * Open connection url connection
     *
     * @param handler handler
     * @param url     url
     * @return the url connection
     * @throws Exception exception
     * @since 1.0.0
     */
    private URLConnection openConnection(URLStreamHandler handler, URL url) throws IOException {
        return new URL(null, url.toExternalForm(), handler).openConnection();
    }

    /**
     * Parse url *
     *
     * @param context context
     * @param spec    spec
     * @param start   start
     * @param limit   limit
     * @since 1.0.0
     */
    @Override
    protected void parseURL(URL context, String spec, int start, int limit) {
        if (spec.regionMatches(true, 0, JAR_PROTOCOL, 0, JAR_PROTOCOL.length())) {
            this.setFile(context, this.getFileFromSpec(spec.substring(start, limit)));
        } else {
            this.setFile(context, this.getFileFromContext(context, spec.substring(start, limit)));
        }
    }

    /**
     * Gets file from spec *
     *
     * @param spec spec
     * @return the file from spec
     * @since 1.0.0
     */
    private String getFileFromSpec(String spec) {
        int separatorIndex = spec.lastIndexOf("!/");
        if (separatorIndex == -1) {
            throw new IllegalArgumentException("No !/ in spec '" + spec + "'");
        }
        try {
            new URL(spec.substring(0, separatorIndex));
            return spec;
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Invalid spec URL '" + spec + "'", ex);
        }
    }

    /**
     * Gets file from context *
     *
     * @param context context
     * @param spec    spec
     * @return the file from context
     * @since 1.0.0
     */
    private String getFileFromContext(URL context, String spec) {
        String file = context.getFile();
        if (spec.startsWith("/")) {
            return this.trimToJarRoot(file) + SEPARATOR + spec.substring(1);
        }
        if (file.endsWith("/")) {
            return file + spec;
        }
        int lastSlashIndex = file.lastIndexOf('/');
        if (lastSlashIndex == -1) {
            throw new IllegalArgumentException("No / found in context URL's file '" + file + "'");
        }
        return file.substring(0, lastSlashIndex + 1) + spec;
    }

    /**
     * Trim to jar root string
     *
     * @param file file
     * @return the string
     * @since 1.0.0
     */
    private String trimToJarRoot(String file) {
        int lastSeparatorIndex = file.lastIndexOf(SEPARATOR);
        if (lastSeparatorIndex == -1) {
            throw new IllegalArgumentException("No !/ found in context URL's file '" + file + "'");
        }
        return file.substring(0, lastSeparatorIndex);
    }

    /**
     * Sets file *
     *
     * @param context context
     * @param file    file
     * @since 1.0.0
     */
    private void setFile(URL context, String file) {
        String path = this.normalize(file);
        String query = null;
        int queryIndex = path.lastIndexOf('?');
        if (queryIndex != -1) {
            query = path.substring(queryIndex + 1);
            path = path.substring(0, queryIndex);
        }
        this.setURL(context, JAR_PROTOCOL, null, -1, null, null, path, query, context.getRef());
    }

    /**
     * Normalize string
     *
     * @param file file
     * @return the string
     * @since 1.0.0
     */
    private String normalize(String file) {
        if (!file.contains(CURRENT_DIR) && !file.contains(PARENT_DIR)) {
            return file;
        }
        int afterLastSeparatorIndex = file.lastIndexOf(SEPARATOR) + SEPARATOR.length();
        String afterSeparator = file.substring(afterLastSeparatorIndex);
        afterSeparator = this.replaceParentDir(afterSeparator);
        afterSeparator = this.replaceCurrentDir(afterSeparator);
        return file.substring(0, afterLastSeparatorIndex) + afterSeparator;
    }

    /**
     * Replace parent dir string
     *
     * @param file file
     * @return the string
     * @since 1.0.0
     */
    private String replaceParentDir(String file) {
        int parentDirIndex;
        while ((parentDirIndex = file.indexOf(PARENT_DIR)) >= 0) {
            int precedingSlashIndex = file.lastIndexOf('/', parentDirIndex - 1);
            if (precedingSlashIndex >= 0) {
                file = file.substring(0, precedingSlashIndex) + file.substring(parentDirIndex + 3);
            } else {
                file = file.substring(parentDirIndex + 4);
            }
        }
        return file;
    }

    /**
     * Replace current dir string
     *
     * @param file file
     * @return the string
     * @since 1.0.0
     */
    private String replaceCurrentDir(String file) {
        return CURRENT_DIR_PATTERN.matcher(file).replaceAll("/");
    }

    /**
     * Hash code int
     *
     * @param u u
     * @return the int
     * @since 1.0.0
     */
    @Override
    protected int hashCode(URL u) {
        return this.hashCode(u.getProtocol(), u.getFile());
    }

    /**
     * Hash code int
     *
     * @param protocol protocol
     * @param file     file
     * @return the int
     * @since 1.0.0
     */
    @SuppressWarnings("java:S2112")
    private int hashCode(String protocol, String file) {
        int result = (protocol != null) ? protocol.hashCode() : 0;
        int separatorIndex = file.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            return result + file.hashCode();
        }
        String source = file.substring(0, separatorIndex);
        String entry = this.canonicalize(file.substring(separatorIndex + 2));
        try {
            result += new URL(source).hashCode();
        } catch (IOException ex) {
            result += source.hashCode();
        }
        result += entry.hashCode();
        return result;
    }

    /**
     * Same file boolean
     *
     * @param u1 u 1
     * @param u2 u 2
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    protected boolean sameFile(URL u1, URL u2) {
        if (!u1.getProtocol().equals("jar") || !u2.getProtocol().equals("jar")) {
            return false;
        }
        int separator1 = u1.getFile().indexOf(SEPARATOR);
        int separator2 = u2.getFile().indexOf(SEPARATOR);
        if (separator1 == -1 || separator2 == -1) {
            return super.sameFile(u1, u2);
        }
        String nested1 = u1.getFile().substring(separator1 + SEPARATOR.length());
        String nested2 = u2.getFile().substring(separator2 + SEPARATOR.length());
        if (!nested1.equals(nested2)) {
            String canonical1 = this.canonicalize(nested1);
            String canonical2 = this.canonicalize(nested2);
            if (!canonical1.equals(canonical2)) {
                return false;
            }
        }
        String root1 = u1.getFile().substring(0, separator1);
        String root2 = u2.getFile().substring(0, separator2);
        try {
            return super.sameFile(new URL(root1), new URL(root2));
        } catch (MalformedURLException ex) {
            // Continue
        }
        return super.sameFile(u1, u2);
    }

    /**
     * Canonicalize string
     *
     * @param path path
     * @return the string
     * @since 1.0.0
     */
    private String canonicalize(String path) {
        return path.replace(SEPARATOR, "/");
    }

    /**
     * Gets root jar file from url *
     *
     * @param url url
     * @return the root jar file from url
     * @throws IOException io exception
     * @since 1.0.0
     */
    public CustomJarFile getRootJarFileFromUrl(URL url) throws IOException {
        String spec = url.getFile();
        int separatorIndex = spec.indexOf(SEPARATOR);
        if (separatorIndex == -1) {
            throw new MalformedURLException("Jar URL does not contain !/ separator");
        }
        String name = spec.substring(0, separatorIndex);
        return this.getRootJarFile(name);
    }

    /**
     * Gets root jar file *
     *
     * @param name name
     * @return the root jar file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private CustomJarFile getRootJarFile(String name) throws IOException {
        try {
            if (!name.startsWith(FILE_PROTOCOL)) {
                throw new IllegalStateException("Not a file URL");
            }
            File file = new File(URI.create(name));
            Map<File, CustomJarFile> cache = rootFileCache.get();
            CustomJarFile result = (cache != null) ? cache.get(file) : null;
            if (result == null) {
                result = new CustomJarFile(file);
                addToRootFileCache(file, result);
            }
            return result;
        } catch (Exception ex) {
            throw new IOException("Unable to open root Jar file '" + name + "'", ex);
        }
    }

    /**
     * Add the given {@link CustomJarFile} to the root file cache.
     *
     * @param sourceFile the source file to add
     * @param jarFile    the jar file.
     * @since 1.0.0
     */
    static void addToRootFileCache(File sourceFile, CustomJarFile jarFile) {
        Map<File, CustomJarFile> cache = rootFileCache.get();
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            rootFileCache = new SoftReference<>(cache);
        }
        cache.put(sourceFile, jarFile);
    }

    /**
     * Set if a generic static exception can be thrown when a URL cannot be connected.
     * This optimization is used during class loading to save creating lots of exceptions
     * which are then swallowed.
     *
     * @param useFastConnectionExceptions if fast connection exceptions can be used.
     * @since 1.0.0
     */
    public static void setUseFastConnectionExceptions(boolean useFastConnectionExceptions) {
        JarURLConnection.setUseFastExceptions(useFastConnectionExceptions);
    }

}
