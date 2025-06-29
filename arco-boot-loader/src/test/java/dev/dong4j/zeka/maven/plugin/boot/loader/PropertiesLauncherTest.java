package dev.dong4j.zeka.maven.plugin.boot.loader;

import dev.dong4j.zeka.maven.plugin.boot.loader.archive.Archive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.ExplodedArchive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.JarFileArchive;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.FileSystemResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.07.04 21:47
 * @since 1.5.0
 */
@SuppressWarnings("all")
class PropertiesLauncherTest {

    @TempDir
    File tempDir;

    private ClassLoader contextClassLoader;


    @BeforeEach
    void setup() {
        this.contextClassLoader = Thread.currentThread().getContextClassLoader();
        System.setProperty("loader.home", new File("src/test/resources").getAbsolutePath());
    }

    @AfterEach
    void close() {
        Thread.currentThread().setContextClassLoader(this.contextClassLoader);
        System.clearProperty("loader.home");
        System.clearProperty("loader.path");
        System.clearProperty("loader.main");
        System.clearProperty("loader.config.name");
        System.clearProperty("loader.config.location");
        System.clearProperty("loader.system");
        System.clearProperty("loader.classLoader");
    }

    @Test
    void testDefaultHome() {
        System.clearProperty("loader.home");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getHomeDirectory()).isEqualTo(new File(System.getProperty("user.dir")));
    }

    @Test
    void testAlternateHome() throws Exception {
        System.setProperty("loader.home", "src/test/resources/home");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getHomeDirectory()).isEqualTo(new File(System.getProperty("loader.home")));
        assertThat(launcher.getMainClass()).isEqualTo("demo.HomeApplication");
    }

    @Test
    void testNonExistentHome() {
        System.setProperty("loader.home", "src/test/resources/nonexistent");
    }

    @Test
    void testUserSpecifiedMain() throws Exception {
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("demo.Application");
        assertThat(System.getProperty("loader.main")).isNull();
    }

    @Test
    void testUserSpecifiedConfigName() throws Exception {
        System.setProperty("loader.config.name", "foo");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("my.Application");
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[etc/]");
    }

    @Test
    void testRootOfClasspathFirst() throws Exception {
        System.setProperty("loader.config.name", "bar");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("my.BarApplication");
    }

    @Test
    void testUserSpecifiedDotPath() {
        System.setProperty("loader.path", ".");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[.]");
    }

    @Test
    void testUserSpecifiedSlashPath() throws Exception {
        System.setProperty("loader.path", "jars/");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[jars/]");
    }

    @Test
    void testUserSpecifiedWildcardPath() throws Exception {
        System.setProperty("loader.path", "jars/*");
        System.setProperty("loader.main", "demo.Application");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[jars/]");
        launcher.launch(new String[0]);
        this.waitFor("Hello World");
    }

    @Test
    void testUserSpecifiedJarPath() throws Exception {
        System.setProperty("loader.path", "jars/app.jar");
        System.setProperty("loader.main", "demo.Application");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[jars/app.jar]");
        launcher.launch(new String[0]);
        this.waitFor("Hello World");
    }

    @Test
    void testUserSpecifiedDirectoryContainingJarFileWithNestedArchives() throws Exception {
        System.setProperty("loader.path", "nested-jars");
        System.setProperty("loader.main", "demo.Application");
        PropertiesLauncher launcher = new PropertiesLauncher();
        launcher.launch(new String[0]);
        this.waitFor("Hello World");
    }

    @Test
    void testUserSpecifiedJarPathWithDot() throws Exception {
        System.setProperty("loader.path", "./jars/app.jar");
        System.setProperty("loader.main", "demo.Application");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[jars/app.jar]");
        launcher.launch(new String[0]);
        this.waitFor("Hello World");
    }

    @Test
    void testUserSpecifiedClassLoader() throws Exception {
        System.setProperty("loader.path", "jars/app.jar");
        System.setProperty("loader.classLoader", URLClassLoader.class.getName());
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString()).isEqualTo("[jars/app.jar]");
        launcher.launch(new String[0]);
        this.waitFor("Hello World");
    }

    @Test
    void testUserSpecifiedClassPathOrder() throws Exception {
        System.setProperty("loader.path", "more-jars/app.jar,jars/app.jar");
        System.setProperty("loader.classLoader", URLClassLoader.class.getName());
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(ReflectionTestUtils.getField(launcher, "paths").toString())
            .isEqualTo("[more-jars/app.jar, jars/app.jar]");
        launcher.launch(new String[0]);
        this.waitFor("Hello Other World");
    }

    @Test
    void testCustomClassLoaderCreation() throws Exception {
        System.setProperty("loader.classLoader", TestLoader.class.getName());
        PropertiesLauncher launcher = new PropertiesLauncher();
        ClassLoader loader = launcher.createClassLoader(this.archives());
        assertThat(loader).isNotNull();
        assertThat(loader.getClass().getName()).isEqualTo(TestLoader.class.getName());
    }

    private List<Archive> archives() throws Exception {
        List<Archive> archives = new ArrayList<>();
        String path = System.getProperty("java.class.path");
        for (String url : path.split(File.pathSeparator)) {
            Archive archive = this.archive(url);
            if (archive != null) {
                archives.add(archive);
            }
        }
        return archives;
    }

    private Archive archive(String url) throws IOException {
        File file = new FileSystemResource(url).getFile();
        if (!file.exists()) {
            return null;
        }
        if (url.endsWith(".jar")) {
            return new JarFileArchive(file);
        }
        return new ExplodedArchive(file);
    }

    @Test
    void testUserSpecifiedConfigPathWins() throws Exception {

        System.setProperty("loader.config.name", "foo");
        System.setProperty("loader.config.location", "classpath:bar.properties");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("my.BarApplication");
    }

    @Test
    void testSystemPropertySpecifiedMain() throws Exception {
        System.setProperty("loader.main", "foo.Bar");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("foo.Bar");
    }

    @Test
    void testSystemPropertiesSet() {
        System.setProperty("loader.system", "true");
        new PropertiesLauncher();
        assertThat(System.getProperty("loader.main")).isEqualTo("demo.Application");
    }

    @Test
    void testArgsEnhanced() throws Exception {
        System.setProperty("loader.args", "foo");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(Arrays.asList(launcher.getArgs("bar")).toString()).isEqualTo("[foo, bar]");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testLoadPathCustomizedUsingManifest() throws Exception {
        System.setProperty("loader.home", this.tempDir.getAbsolutePath());
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().putValue("Loader-Path", "/foo.jar, /bar");
        File manifestFile = new File(this.tempDir, "META-INF/MANIFEST.MF");
        manifestFile.getParentFile().mkdirs();
        try (FileOutputStream manifestStream = new FileOutputStream(manifestFile)) {
            manifest.write(manifestStream);
        }
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat((List<String>) ReflectionTestUtils.getField(launcher, "paths")).containsExactly("/foo.jar", "/bar/");
    }

    @Test
    void testManifestWithPlaceholders() throws Exception {
        System.setProperty("loader.home", "src/test/resources/placeholders");
        PropertiesLauncher launcher = new PropertiesLauncher();
        assertThat(launcher.getMainClass()).isEqualTo("demo.FooApplication");
    }

    private void waitFor(String value) throws Exception {
    }

    private Condition<Archive> endingWith(String value) {
        return new Condition<Archive>() {

            @Override
            public boolean matches(Archive archive) {
                return archive.toString().endsWith(value);
            }

        };
    }

    static class TestLoader extends URLClassLoader {

        TestLoader(ClassLoader parent) {
            super(new URL[0], parent);
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }

    }
}
