package dev.dong4j.arco.maven.plugin.boot.loader;

import dev.dong4j.arco.maven.plugin.boot.loader.archive.Archive;
import dev.dong4j.arco.maven.plugin.boot.loader.archive.ExplodedArchive;
import dev.dong4j.arco.maven.plugin.boot.loader.archive.JarFileArchive;
import dev.dong4j.arco.maven.plugin.boot.loader.util.SystemPropertyUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 通过属性文件配置类路径和主类的存档启动程序.
 * 与基于可执行jar的模型相比, 该模型通常更灵活, 更易于创建性能良好的OS级服务.
 * 在不同的位置查找要提取加载程序设置的属性文件, 默认为加载程序.properties在当前类路径上或在当前工作目录中.
 * 属性文件的名称可以通过设置系统属性来更改loader.config.name加载程序 (例如-Dloader.config.name=foo会寻找食品属性.
 * 如果该文件不存在, 则尝试加载程序配置位置 (带有允许的前缀classpath:和file:或任何有效的URL) .
 * 找到该文件后, 将其转换为属性并提取可选值 (如果该文件不存在, 也可以将其作为系统属性重写) :
 * loader.path: 以逗号分隔的目录列表 (包含文件资源和/或*.jar或*.zip或archives中的嵌套存档) 或要附加到类路径的存档.
 * BOOT-INF/classes, 应用程序归档文件中的 BOOT-INF/lib 总是被使用
 * loader.main: 设置类加载程序后将执行委托给的主方法.
 *
 * @author Dave Syer
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.15 10:43
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class PropertiesLauncher extends Launcher {

    /** Boot inf classes */
    static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";
    /** Boot inf lib */
    static final String BOOT_INF_LIB = "BOOT-INF/lib/";
    /** PARENT_ONLY_PARAMS */
    private static final Class<?>[] PARENT_ONLY_PARAMS = new Class<?>[]{ClassLoader.class};
    /** URLS_AND_PARENT_PARAMS */
    private static final Class<?>[] URLS_AND_PARENT_PARAMS = new Class<?>[]{URL[].class, ClassLoader.class};
    /** NO_PARAMS */
    private static final Class<?>[] NO_PARAMS = new Class<?>[]{};
    /** NO_URLS */
    protected static final URL[] NO_URLS = new URL[0];
    /** DEBUG */
    private static final String DEBUG = "loader.debug";
    /** Start-Class 主类的属性键 */
    public static final String MAIN = "loader.main";
    /** CLASSPATH_PREFIX */
    private static final String CLASSPATH_PREFIX = "classpath:";
    /** FILE_PREFIX */
    private static final String FILE_PREFIX = "file:";
    /** PROPERTIES_SUFFIX */
    private static final String PROPERTIES_SUFFIX = ".properties";
    /** DOT_JAR */
    private static final String DOT_JAR = ".jar";
    /** DOT_ZIP */
    private static final String DOT_ZIP = ".zip";
    /** SEPARATOR */
    private static final String SEPARATOR = "/";
    /** DOT */
    private static final String DOT = ".";
    /** CURRENT_PATH */
    private static final String CURRENT_PATH = DOT + SEPARATOR;
    /** JAR_FILE */
    private static final String JAR_FILE = "jar:file:";

    /**
     * 类路径项的属性键 (可能包含jar或jar的目录) .
     * 可以使用逗号分隔的列表指定多个条目.
     * 始终使用应用程序存档中的 BOOT-INF/classes、BOOT-INF/lib*.
     */
    public static final String PATH = "loader.path";

    /**
     * 主目录的属性键. 这是外部配置 (如果不在类路径上) 的位置, 也是加载程序路径中任何相对路径的基路径.
     * 默认为当前工作目录 (<code>${user.dir}</code>)).
     */
    public static final String HOME = "loader.home";

    /**
     * 默认命令行参数的属性键.
     * 在启动之前, 这些参数 (如果存在) 在主方法参数之前.
     */
    public static final String ARGS = "loader.args";

    /**
     * 外部配置文件名的属性键 (不包括后缀) .
     * 默认为 'application'. 如果改为  {@link #CONFIG_LOCATION loader config location} , 则忽略
     */
    public static final String CONFIG_NAME = "loader.config.name";

    /**
     * 配置文件位置的属性键 (包括可选的类路径: 、文件: 或URL前缀)
     */
    public static final String CONFIG_LOCATION = "loader.config.location";
    /** boolean 标志的 Properties 键 (默认为false) , 如果设置了该键, 则会将外部配置属性复制到系统属性 (假设Java安全性允许) . */
    public static final String SET_SYSTEM_PROPERTIES = "loader.system";
    /** WORD_SEPARATOR */
    private static final Pattern WORD_SEPARATOR = Pattern.compile("\\W+");
    /** NESTED_ARCHIVE_SEPARATOR */
    private static final String NESTED_ARCHIVE_SEPARATOR = "!" + File.separator;
    /** Home */
    private final File homeFile;
    /** Paths */
    private List<String> paths = new ArrayList<>();
    /** Properties */
    private final Properties properties = new Properties();
    /** Parent */
    private final Archive parent;

    /**
     * Properties launcher
     *
     * @since 1.5.0
     */
    public PropertiesLauncher() {
        try {
            this.homeFile = this.getHomeDirectory();
            this.initializeProperties();
            this.initializePaths();
            this.parent = this.createArchive();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Gets home directory *
     *
     * @return the home directory
     * @since 1.5.0
     */
    protected File getHomeDirectory() {
        try {
            return new File(this.getPropertyWithDefault(HOME, "${user.dir}"));
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    /**
     * Debug
     *
     * @param message message
     * @since 1.5.0
     */
    protected void debug(String message) {
        if (Boolean.getBoolean(DEBUG)) {
            System.out.println(message);
        }
    }

    /**
     * Initialize properties
     *
     * @throws Exception   exception
     * @throws IOException io exception
     * @since 1.5.0
     */
    private void initializeProperties() throws Exception, IOException {
        List<String> configs = new ArrayList<>();
        if (this.getProperty(CONFIG_LOCATION) != null) {
            configs.add(this.getProperty(CONFIG_LOCATION));
        } else {
            String[] names = this.getPropertyWithDefault(CONFIG_NAME, "loader").split(",");
            for (String name : names) {
                configs.add(FILE_PREFIX + this.getHomeDirectory() + SEPARATOR + name + PROPERTIES_SUFFIX);
                configs.add(CLASSPATH_PREFIX + name + PROPERTIES_SUFFIX);
                configs.add(CLASSPATH_PREFIX + BOOT_INF_CLASSES + name + PROPERTIES_SUFFIX);
            }
        }
        for (String config : configs) {
            try (InputStream resource = this.getResource(config)) {
                if (resource != null) {
                    this.debug("Found: " + config);
                    this.loadResource(resource);
                    // Load the first one we find
                    return;
                } else {
                    this.debug("Not found: " + config);
                }
            }
        }
    }

    /**
     * Load resource
     *
     * @param resource resource
     * @throws IOException io exception
     * @throws Exception   exception
     * @since 1.5.0
     */
    private void loadResource(InputStream resource) throws IOException, Exception {
        this.properties.load(resource);
        for (Object key : Collections.list(this.properties.propertyNames())) {
            String text = this.properties.getProperty((String) key);
            String value = SystemPropertyUtils.resolvePlaceholders(this.properties, text);
            if (value != null) {
                this.properties.put(key, value);
            }
        }
        if (Boolean.TRUE.toString().equals(this.getProperty(SET_SYSTEM_PROPERTIES))) {
            this.debug("Adding resolved properties to System properties");
            for (Object key : Collections.list(this.properties.propertyNames())) {
                String value = this.properties.getProperty((String) key);
                System.setProperty((String) key, value);
            }
        }
    }

    /**
     * Gets resource *
     *
     * @param config config
     * @return the resource
     * @throws Exception exception
     * @since 1.5.0
     */
    private InputStream getResource(@NotNull String config) throws Exception {
        if (config.startsWith("classpath:")) {
            return this.getClasspathResource(config.substring("classpath:".length()));
        }
        config = this.handleUrl(config);
        if (this.isUrl(config)) {
            return this.getURLResource(config);
        }
        return this.getFileResource(config);
    }

    /**
     * Handle url
     *
     * @param path path
     * @return the string
     * @throws UnsupportedEncodingException unsupported encoding exception
     * @since 1.5.0
     */
    private @NotNull String handleUrl(@NotNull String path) throws UnsupportedEncodingException {
        if (path.startsWith(JAR_FILE) || path.startsWith(FILE_PREFIX)) {
            path = URLDecoder.decode(path, "UTF-8");
            if (path.startsWith(FILE_PREFIX)) {
                path = path.substring(FILE_PREFIX.length());
                if (path.startsWith("//")) {
                    path = path.substring(2);
                }
            }
        }
        return path;
    }

    /**
     * Is url
     *
     * @param config config
     * @return the boolean
     * @since 1.5.0
     */
    @Contract(pure = true)
    private boolean isUrl(@NotNull String config) {
        return config.contains("://");
    }

    /**
     * Gets classpath resource *
     *
     * @param config config
     * @return the classpath resource
     * @since 1.5.0
     */
    private InputStream getClasspathResource(@NotNull String config) {
        while (config.startsWith(SEPARATOR)) {
            config = config.substring(1);
        }
        config = SEPARATOR + config;
        this.debug("Trying classpath: " + config);
        return this.getClass().getResourceAsStream(config);
    }

    /**
     * Gets file resource *
     *
     * @param config config
     * @return the file resource
     * @throws Exception exception
     * @since 1.5.0
     */
    private @Nullable InputStream getFileResource(String config) throws Exception {
        File file = new File(config);
        this.debug("Trying file: " + config);
        if (file.canRead()) {
            return new FileInputStream(file);
        }
        return null;
    }

    /**
     * Gets url resource *
     *
     * @param config config
     * @return the url resource
     * @throws Exception exception
     * @since 1.5.0
     */
    private @Nullable InputStream getURLResource(String config) throws Exception {
        URL url = new URL(config);
        if (this.exists(url)) {
            URLConnection con = url.openConnection();
            try {
                return con.getInputStream();
            } catch (IOException ex) {
                // Close the HTTP connection (if applicable).
                if (con instanceof HttpURLConnection) {
                    ((HttpURLConnection) con).disconnect();
                }
                throw ex;
            }
        }
        return null;
    }

    /**
     * Exists
     *
     * @param url url
     * @return the boolean
     * @throws IOException io exception
     * @since 1.5.0
     */
    private boolean exists(@NotNull URL url) throws IOException {
        // Try a URL connection content-length header...
        URLConnection connection = url.openConnection();
        try {
            connection.setUseCaches(connection.getClass().getSimpleName().startsWith("JNLP"));
            if (connection instanceof HttpURLConnection) {
                HttpURLConnection httpConnection = (HttpURLConnection) connection;
                httpConnection.setRequestMethod("HEAD");
                int responseCode = httpConnection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return true;
                } else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    return false;
                }
            }
            return (connection.getContentLength() >= 0);
        } finally {
            if (connection instanceof HttpURLConnection) {
                ((HttpURLConnection) connection).disconnect();
            }
        }
    }

    /**
     * Initialize paths
     *
     * @throws Exception exception
     * @since 1.5.0
     */
    private void initializePaths() throws Exception {
        String path = this.getProperty(PATH);
        if (path != null) {
            this.paths = this.parsePathsProperty(path);
        }
        this.debug("Nested archive paths: " + this.paths);
    }

    /**
     * Parse paths property
     *
     * @param commaSeparatedPaths comma separated paths
     * @return the list
     * @since 1.5.0
     */
    private @NotNull List<String> parsePathsProperty(@NotNull String commaSeparatedPaths) {
        List<String> paths = new ArrayList<>();
        for (String path : commaSeparatedPaths.split(",")) {
            path = this.cleanupPath(path);
            // "" means the user wants root of archive but not current directory
            path = "".equals(path) ? SEPARATOR : path;
            paths.add(path);
        }
        if (paths.isEmpty()) {
            paths.add("lib");
        }
        return paths;
    }

    /**
     * Get args
     *
     * @param args args
     * @return the string [ ]
     * @throws Exception exception
     * @since 1.5.0
     */
    protected String[] getArgs(String... args) throws Exception {
        String loaderArgs = this.getProperty(ARGS);
        if (loaderArgs != null) {
            String[] defaultArgs = loaderArgs.split("\\s+");
            String[] additionalArgs = args;
            args = new String[defaultArgs.length + additionalArgs.length];
            System.arraycopy(defaultArgs, 0, args, 0, defaultArgs.length);
            System.arraycopy(additionalArgs, 0, args, defaultArgs.length, additionalArgs.length);
        }
        return args;
    }

    /**
     * Gets main class *
     *
     * @return the main class
     * @throws Exception exception
     * @since 1.5.0
     */
    @Override
    protected String getMainClass() throws Exception {
        String mainClass = this.getProperty(MAIN, "Start-Class", null);
        if (mainClass == null) {
            throw new IllegalStateException("No '" + MAIN + "' or 'Start-Class' specified");
        }
        return mainClass;
    }

    /**
     * Create class loader
     *
     * @param archives archives
     * @return the class loader
     * @throws Exception exception
     * @since 1.5.0
     */
    @Override
    protected ClassLoader createClassLoader(@NotNull List<Archive> archives) throws Exception {
        Set<URL> urls = new LinkedHashSet<>(archives.size());
        for (Archive archive : archives) {
            urls.add(archive.getUrl());
        }
        ClassLoader loader = new LaunchedURLClassLoader(urls.toArray(NO_URLS), this.getClass().getClassLoader());
        this.debug("Classpath: " + urls);
        String customLoaderClassName = this.getProperty("loader.classLoader");
        if (customLoaderClassName != null) {
            loader = this.wrapWithCustomClassLoader(loader, customLoaderClassName);
            this.debug("Using custom class loader: " + customLoaderClassName);
        }
        return loader;
    }

    /**
     * Wrap with custom class loader
     *
     * @param parent    parent
     * @param className class name
     * @return the class loader
     * @throws Exception exception
     * @since 1.5.0
     */
    @SuppressWarnings("unchecked")
    private ClassLoader wrapWithCustomClassLoader(ClassLoader parent, String className) throws Exception {
        Class<ClassLoader> type = (Class<ClassLoader>) Class.forName(className, true, parent);
        ClassLoader classLoader = this.newClassLoader(type, PARENT_ONLY_PARAMS, parent);
        if (classLoader == null) {
            classLoader = this.newClassLoader(type, URLS_AND_PARENT_PARAMS, NO_URLS, parent);
        }
        if (classLoader == null) {
            classLoader = this.newClassLoader(type, NO_PARAMS);
        }
        Assert.notNull(classLoader, "Unable to create class loader for " + className);
        return classLoader;
    }

    /**
     * New class loader
     *
     * @param loaderClass    loader class
     * @param parameterTypes parameter types
     * @param initargs       initargs
     * @return the class loader
     * @throws Exception exception
     * @since 1.5.0
     */
    private @Nullable ClassLoader newClassLoader(@NotNull Class<ClassLoader> loaderClass, Class<?>[] parameterTypes, Object... initargs)
        throws Exception {
        try {
            Constructor<ClassLoader> constructor = loaderClass.getDeclaredConstructor(parameterTypes);
            constructor.setAccessible(true);
            return constructor.newInstance(initargs);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Gets property *
     *
     * @param propertyKey property key
     * @return the property
     * @throws Exception exception
     * @since 1.5.0
     */
    private String getProperty(String propertyKey) throws Exception {
        return this.getProperty(propertyKey, null, null);
    }

    /**
     * Gets property with default *
     *
     * @param propertyKey  property key
     * @param defaultValue default value
     * @return the property with default
     * @throws Exception exception
     * @since 1.5.0
     */
    private String getPropertyWithDefault(String propertyKey, String defaultValue) throws Exception {
        return this.getProperty(propertyKey, null, defaultValue);
    }

    /**
     * Gets property *
     *
     * @param propertyKey  property key
     * @param manifestKey  manifest key
     * @param defaultValue default value
     * @return the property
     * @throws Exception exception
     * @since 1.5.0
     */
    private @Nullable String getProperty(String propertyKey, String manifestKey, String defaultValue) throws Exception {
        if (manifestKey == null) {
            manifestKey = propertyKey.replace('.', '-');
            manifestKey = toCamelCase(manifestKey);
        }
        String property = SystemPropertyUtils.getProperty(propertyKey);
        if (property != null) {
            String value = SystemPropertyUtils.resolvePlaceholders(this.properties, property);
            this.debug("Property '" + propertyKey + "' from environment: " + value);
            return value;
        }
        if (this.properties.containsKey(propertyKey)) {
            String value = SystemPropertyUtils.resolvePlaceholders(this.properties,
                this.properties.getProperty(propertyKey));
            this.debug("Property '" + propertyKey + "' from properties: " + value);
            return value;
        }
        try {
            if (this.homeFile != null) {
                // Prefer home dir for MANIFEST if there is one
                try (ExplodedArchive archive = new ExplodedArchive(this.homeFile, false)) {
                    Manifest manifest = archive.getManifest();
                    if (manifest != null) {
                        String value = manifest.getMainAttributes().getValue(manifestKey);
                        if (value != null) {
                            this.debug("Property '" + manifestKey + "' from home directory manifest: " + value);
                            return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
                        }
                    }
                }
            }
        } catch (IllegalStateException ex) {
            // Ignore
        }
        // Otherwise try the parent archive
        Manifest manifest = this.createArchive().getManifest();
        if (manifest != null) {
            String value = manifest.getMainAttributes().getValue(manifestKey);
            if (value != null) {
                this.debug("Property '" + manifestKey + "' from archive manifest: " + value);
                return SystemPropertyUtils.resolvePlaceholders(this.properties, value);
            }
        }
        return (defaultValue != null) ? SystemPropertyUtils.resolvePlaceholders(this.properties, defaultValue)
            : null;
    }

    /**
     * Gets class path archives *
     *
     * @return the class path archives
     * @throws Exception exception
     * @since 1.5.0
     */
    @Override
    protected List<Archive> getClassPathArchives() throws Exception {
        List<Archive> lib = new ArrayList<>();
        for (String path : this.paths) {
            for (Archive archive : this.getClassPathArchives(path)) {
                if (archive instanceof ExplodedArchive) {
                    List<Archive> nested = new ArrayList<>(archive.getNestedArchives(new ArchiveEntryFilter()));
                    nested.add(0, archive);
                    lib.addAll(nested);
                } else {
                    lib.add(archive);
                }
            }
        }
        this.addNestedEntries(lib);
        return lib;
    }

    /**
     * 获取目录下的 jar 或 目录, 如果是目录则会再次递归获取 jar
     *
     * @param path path
     * @return the class path archives
     * @throws Exception exception
     * @since 1.5.0
     */
    protected @NotNull List<Archive> getClassPathArchives(String path) throws Exception {
        String root = this.cleanupPath(this.handleUrl(path));
        List<Archive> lib = new ArrayList<>();
        File file = new File(root);
        if (!SEPARATOR.equals(root)) {
            if (!this.isAbsolutePath(root)) {
                file = new File(this.homeFile, root);
            }
            if (file.isDirectory()) {
                this.debug("Adding classpath entries from " + file);
                Archive archive = new ExplodedArchive(file, false);
                lib.add(archive);
            }
        }
        Archive archive = this.getArchive(file);
        if (archive != null) {
            this.debug("Adding classpath entries from archive " + archive.getUrl() + root);
            lib.add(archive);
        }
        List<Archive> nestedArchives = this.getNestedArchives(root);
        if (nestedArchives != null) {
            this.debug("Adding classpath entries from nested " + root);
            lib.addAll(nestedArchives);
        }
        return lib;
    }

    /**
     * Is absolute path
     *
     * @param root root
     * @return the boolean
     * @since 1.5.0
     */
    private boolean isAbsolutePath(@NotNull String root) {
        // Windows contains ":" others start with "/"
        return root.contains(":") || root.startsWith(SEPARATOR);
    }

    /**
     * Gets archive *
     *
     * @param file file
     * @return the archive
     * @throws IOException io exception
     * @since 1.5.0
     */
    private @Nullable Archive getArchive(File file) throws IOException {
        if (this.isNestedArchivePath(file)) {
            return null;
        }
        String name = file.getName().toLowerCase(Locale.ENGLISH);
        if (name.endsWith(DOT_JAR) || name.endsWith(DOT_ZIP)) {
            return new JarFileArchive(file);
        }
        return null;
    }

    /**
     * Is nested archive path
     *
     * @param file file
     * @return the boolean
     * @since 1.5.0
     */
    private boolean isNestedArchivePath(@NotNull File file) {
        return file.getPath().contains(NESTED_ARCHIVE_SEPARATOR);
    }

    /**
     * 获取目录下所有的 jar 包
     *
     * @param path path
     * @return the nested archives
     * @throws Exception exception
     * @since 1.5.0
     */
    private @Nullable List<Archive> getNestedArchives(String path) throws Exception {
        Archive parent = this.parent;
        String root = path;
        if (!root.equals(SEPARATOR) && root.startsWith(SEPARATOR) || parent.getUrl().equals(this.homeFile.toURI().toURL())) {
            // If home dir is same as parent archive, no need to add it twice.
            return null;
        }
        int index = root.indexOf('!');
        if (index != -1) {
            File file = new File(this.homeFile, root.substring(0, index));
            if (root.startsWith(JAR_FILE)) {
                file = new File(root.substring(JAR_FILE.length(), index));
            }
            parent = new JarFileArchive(file);
            root = root.substring(index + 1);
            while (root.startsWith(SEPARATOR)) {
                root = root.substring(1);
            }
        }
        if (root.endsWith(DOT_JAR)) {
            File file = new File(this.homeFile, root);
            if (file.exists()) {
                parent = new JarFileArchive(file);
                root = "";
            }
        }
        if (SEPARATOR.equals(root) || CURRENT_PATH.equals(root) || DOT.equals(root)) {
            // The prefix for nested jars is actually empty if it's at the root
            root = "";
        }
        Archive.EntryFilter filter = new PrefixMatchingArchiveFilter(root);
        List<Archive> archives = new ArrayList<>(parent.getNestedArchives(filter));
        if (("".equals(root) || DOT.equals(root)) && !path.endsWith(DOT_JAR) && parent != this.parent) {
            // You can't find the root with an entry filter so it has to be added
            // explicitly. But don't add the root of the parent archive.
            archives.add(parent);
        }
        return archives;
    }

    /**
     * Add nested entries.
     * The parent archive might have "BOOT-INF/lib/" and "BOOT-INF/classes/"
     * directories, meaning we are running from an executable JAR. We add nested
     * entries from there with low priority (i.e. at end).
     *
     * @param lib lib
     * @since 1.5.0
     */
    private void addNestedEntries(@NotNull List<Archive> lib) {
        try {
            lib.addAll(this.parent.getNestedArchives((entry) -> {
                if (entry.isDirectory()) {
                    return entry.getName().equals(BOOT_INF_CLASSES);
                }
                return entry.getName().startsWith(BOOT_INF_LIB);
            }));
        } catch (IOException ignored) {
        }
    }

    /**
     * Cleanup path
     *
     * @param path path
     * @return the string
     * @since 1.5.0
     */
    private @NotNull String cleanupPath(String path) {
        path = path.trim();
        // No need for current dir path
        if (path.startsWith(CURRENT_PATH)) {
            path = path.substring(2);
        }
        String lowerCasePath = path.toLowerCase(Locale.ENGLISH);
        if (lowerCasePath.endsWith(DOT_JAR) || lowerCasePath.endsWith(DOT_ZIP)) {
            return path;
        }
        if (path.endsWith("/*")) {
            path = path.substring(0, path.length() - 1);
        } else {
            // It's a directory
            if (!path.endsWith(SEPARATOR) && !path.equals(DOT)) {
                path = path + SEPARATOR;
            }
        }
        return path;
    }

    /**
     * To camel case
     *
     * @param string string
     * @return the string
     * @since 1.5.0
     */
    @Contract("null -> null")
    private static String toCamelCase(CharSequence string) {
        if (string == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        Matcher matcher = WORD_SEPARATOR.matcher(string);
        int pos = 0;
        while (matcher.find()) {
            builder.append(capitalize(string.subSequence(pos, matcher.end()).toString()));
            pos = matcher.end();
        }
        builder.append(capitalize(string.subSequence(pos, string.length()).toString()));
        return builder.toString();
    }

    /**
     * Capitalize
     *
     * @param str str
     * @return the string
     * @since 1.5.0
     */
    private static @NotNull String capitalize(@NotNull String str) {
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }

    /**
     * Convenience class for finding nested archives that have a prefix in their file path
     * (e.g. "lib/").
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.06.15 10:43
     * @since 1.5.0
     */
    private static final class PrefixMatchingArchiveFilter implements Archive.EntryFilter {

        /** Prefix */
        private final String prefix;
        /** Filter */
        private final ArchiveEntryFilter filter = new ArchiveEntryFilter();

        /**
         * Prefix matching archive filter
         *
         * @param prefix prefix
         * @since 1.5.0
         */
        private PrefixMatchingArchiveFilter(String prefix) {
            this.prefix = prefix;
        }

        /**
         * Matches
         *
         * @param entry entry
         * @return the boolean
         * @since 1.5.0
         */
        @Override
        public boolean matches(Archive.@NotNull Entry entry) {
            if (entry.isDirectory()) {
                return entry.getName().equals(this.prefix);
            }
            return entry.getName().startsWith(this.prefix) && this.filter.matches(entry);
        }

    }

    /**
     * 过滤 jar 和 zip
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.06.15 10:43
     * @since 1.5.0
     */
    public static final class ArchiveEntryFilter implements Archive.EntryFilter {

        /**
         * Matches
         *
         * @param entry entry
         * @return the boolean
         * @since 1.5.0
         */
        @Override
        public boolean matches(Archive.@NotNull Entry entry) {
            return entry.getName().endsWith(DOT_JAR) || entry.getName().endsWith(DOT_ZIP);
        }

    }

    /**
     * Main
     *
     * @param args args
     * @throws Exception exception
     * @since 1.5.0
     */
    public static void main(String[] args) throws Exception {
        PropertiesLauncher launcher = new PropertiesLauncher();
        args = launcher.getArgs(args);
        launcher.launch(args);
    }

}
