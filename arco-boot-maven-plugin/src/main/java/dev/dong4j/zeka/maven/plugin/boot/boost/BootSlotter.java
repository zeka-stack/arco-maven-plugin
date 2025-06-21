package dev.dong4j.zeka.maven.plugin.boot.boost;

import dev.dong4j.zeka.maven.plugin.boot.loader.jar.CustomJarFile;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveInputStream;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;

/**
 * 为可运行 jar 进行扩展, 主要是修改 main-class 和 start-class, 将原来的 class 字节码全部移动到 {@link BootSlotter#BOOT_INF_CLASSES} 目录下,
 * 便于 {@link BootLauncher} 加载启动.
 * 重新生成的 jar 最终目录结构如下:
 * formatter:off
 * .
 * ├── BOOT-INF
 * │    └── classes            # 部署包原来的 classes
 * ├── META-INF              # 部署包打包为 jar 时生成的相关元数据
 * │    └── maven
 * └── dev/dong4j/zeka/maven/plugin
 * └── boot
 * ├── boost    # 代理启动类, 使用 java -jar 实际启动的 main class
 * └── loader   # classpath 处理, 启动类调用等
 * formatter:on
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.29 18:52
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class BootSlotter extends FileSlotter implements Slotter {
    /** BOOST_CLASS_ROOT_PATH */
    public static final String BOOST_CLASS_ROOT_PATH = "dev/dong4j/zeka/maven/plugin/boot";
    /** BOOST_CLASS_PATH */
    public static final String BOOST_CLASS_PATH = BOOST_CLASS_ROOT_PATH + "/boost/**";
    /** LOADER_CLASS_PATH */
    public static final String LOADER_CLASS_PATH = BOOST_CLASS_ROOT_PATH + "/loader/**";
    /** Boot inf classes */
    static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";
    /** MANIFEST_MF */
    private static final String MANIFEST_MF = "META-INF/MANIFEST.MF";
    /** MAIN_CLASS_ATTRIBUTE */
    private static final String MAIN_CLASS_ATTRIBUTE = "Main-Class";
    /** START_CLASS_ATTRIBUTE */
    private static final String START_CLASS_ATTRIBUTE = "Start-Class";
    /** BOOT_CLASSES_ATTRIBUTE */
    private static final String BOOT_CLASSES_ATTRIBUTE = "Spring-Boot-Classes";
    /** BOOT_LIB_ATTRIBUTE */
    private static final String BOOT_LIB_ATTRIBUTE = "Spring-Boot-Lib";
    /** CLASS_PATH */
    private static final String CLASS_PATH = "Class-Path";
    /** JAR */
    public static final String JAR = ".jar";
    /** CLASS */
    public static final String CLASS = ".class";
    /** BOOT_LAUNCHER */
    private static final String BOOT_LAUNCHER = BootLauncher.class.getName();
    /** 保存 main class 所有的目录 */
    private final Set<String> orginalMainClassdirectories = new HashSet<>(6);
    /** 保存 main class 所在的目录 */
    private String orginalMainClassPath;

    /**
     * 保存原始的 class 目录, 不迁移到新的 jar 中.
     *
     * @param src  src
     * @param dest dest
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void slot(File src, File dest) throws IOException {
        CustomJarFile orginalJarFile = new CustomJarFile(src);

        Manifest manifest = orginalJarFile.getManifest();
        // 原始的 main-class 属性
        String orginalMainClass = manifest.getMainAttributes().getValue(MAIN_CLASS_ATTRIBUTE);
        // package 类型转换为路径
        String mainClasspath = orginalMainClass.replace(".", Matcher.quoteReplacement(File.separator));
        File file = new File(mainClasspath);

        File parentFile = file.getParentFile();
        this.orginalMainClassPath = parentFile.getPath() + File.separator;

        for (; parentFile != null; parentFile = parentFile.getParentFile()) {
            String directory = parentFile.getPath() + File.separator;
            this.orginalMainClassdirectories.add(directory);
        }

        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
            this.slot(in, out);
        }
    }

    /**
     * 处理原始的 jar, 将启动 jar 中的 class 添加到 {@link BootSlotter#BOOT_INF_CLASSES} 目录下, 添加启动相关 class.
     *
     * @param in  in
     * @param out out
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void slot(InputStream in, OutputStream out) throws IOException {

        try (JarArchiveInputStream sourceStream = new JarArchiveInputStream(in);
             JarArchiveOutputStream targetStream = new JarArchiveOutputStream(out)) {

            this.addEntryDir(targetStream, BOOT_INF_CLASSES, System.currentTimeMillis());

            JarArchiveEntry entry;

            while ((entry = sourceStream.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    // 如果缓存的目录与被迁移的目录一致, 则不迁移(解决迁移后的 jar 中存在空目录)
                    if (this.orginalMainClassdirectories.contains(entry.getName()) || entry.getName().contains(this.orginalMainClassPath)) {
                        continue;
                    }
                    this.addEntryDir(targetStream, entry.getName(), entry.getTime());
                } else if (entry.getName().endsWith(JAR)) {
                    this.transformJarEntity(sourceStream, targetStream, entry);
                } else if (entry.getName().equals(MANIFEST_MF)) {
                    this.transformManifestFile(sourceStream, targetStream, entry);
                } else if (entry.getName().endsWith(CLASS)) {
                    this.transformClass(sourceStream, targetStream, entry);
                } else {
                    this.transformOthers(sourceStream, targetStream, entry, entry.getName());
                }
                targetStream.closeArchiveEntry();
            }

            // 将 classpath 下的 dev.dong4j.zeka.maven.plugin.boot.boost 所有 classes 全部写入到 jar
            IOKit.embed(BOOST_CLASS_PATH, targetStream);
            // 将 classpath 下的 dev.dong4j.zeka.maven.plugin.boot.loader 所有 classes 全部写入到 jar
            IOKit.embed(LOADER_CLASS_PATH, targetStream);

            targetStream.finish();
        }
    }

    /**
     * Transform others *
     *
     * @param zis   zis
     * @param zos   zos
     * @param entry entry
     * @param name  name
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void transformOthers(JarArchiveInputStream zis, JarArchiveOutputStream zos, @NotNull JarArchiveEntry entry, String name)
        throws IOException {
        this.addEntryDir(zos, name, entry.getTime());
        IOKit.transfer(zis, zos);
    }

    /**
     * Transform class *
     *
     * @param zis   zis
     * @param zos   zos
     * @param entry entry
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void transformClass(JarArchiveInputStream zis, JarArchiveOutputStream zos, @NotNull JarArchiveEntry entry)
        throws IOException {
        String name = entry.getName();
        this.transformOthers(zis, zos, entry, BOOT_INF_CLASSES + name);
    }

    /**
     * Transform manifest f ile *
     *
     * @param zis   zis
     * @param zos   zos
     * @param entry entry
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void transformManifestFile(JarArchiveInputStream zis, JarArchiveOutputStream zos, @NotNull JarArchiveEntry entry)
        throws IOException {
        Manifest manifest = new Manifest(zis);
        Attributes attributes = manifest.getMainAttributes();

        // 删除 Class-Path 属性
        Manifest newManifest = new Manifest();
        Attributes newAttributes = newManifest.getMainAttributes();
        for (Map.Entry<Object, Object> attr : attributes.entrySet()) {
            if (CLASS_PATH.equals(attr.getKey().toString())) {
                continue;
            }
            newAttributes.putValue(attr.getKey().toString(), attr.getValue().toString());
        }

        newAttributes.putValue(START_CLASS_ATTRIBUTE, newAttributes.getValue(MAIN_CLASS_ATTRIBUTE));
        newAttributes.putValue(MAIN_CLASS_ATTRIBUTE, BOOT_LAUNCHER);
        newAttributes.putValue(BOOT_CLASSES_ATTRIBUTE, BOOT_INF_CLASSES);
        newAttributes.putValue(BOOT_LIB_ATTRIBUTE, "lib/");

        this.addEntryDir(zos, entry.getName(), entry.getTime());
        newManifest.write(zos);
    }

    /**
     * Transform jar entity *
     *
     * @param zis   zis
     * @param zos   zos
     * @param entry entry
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void transformJarEntity(JarArchiveInputStream zis, @NotNull JarArchiveOutputStream zos, @NotNull JarArchiveEntry entry)
        throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        CheckedOutputStream cos = new CheckedOutputStream(bos, new CRC32());
        IOKit.transfer(zis, cos);
        JarArchiveEntry jarArchiveEntry = new JarArchiveEntry(entry.getName());
        jarArchiveEntry.setMethod(JarArchiveEntry.STORED);
        jarArchiveEntry.setSize(bos.size());
        jarArchiveEntry.setTime(entry.getTime());
        jarArchiveEntry.setCrc(cos.getChecksum().getValue());
        zos.putArchiveEntry(jarArchiveEntry);
        ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
        IOKit.transfer(bis, zos);
    }

    /**
     * Add entry dir *
     *
     * @param zos       zos
     * @param entryName boot inf classes
     * @param time      time
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void addEntryDir(@NotNull JarArchiveOutputStream zos, String entryName, long time) throws IOException {
        JarArchiveEntry bootClassDir = new JarArchiveEntry(entryName);
        bootClassDir.setTime(time);
        zos.putArchiveEntry(bootClassDir);
    }
}
