package dev.dong4j.arco.maven.plugin.common;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Maps;
import dev.dong4j.arco.maven.plugin.common.util.FileUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 19:08
 * @since 1.0.0
 */
@SuppressWarnings("all")
public final class FileWriter {
    /** UTF_8 */
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    /** Output file */
    private final File outputFile;

    /** Replace map */
    private final Map<String, String> replaceMap;

    /**
     * Creates a new {@code BuildPropertiesWriter} that will write to the given
     * {@code outputFile}.
     *
     * @param outputFile the output file
     * @since 1.0.0
     */
    @Contract(pure = true)
    public FileWriter(File outputFile) {
        this(outputFile, Maps.newHashMap());
    }

    /**
     * Creates a new {@code BuildPropertiesWriter} that will write to the given
     * {@code outputFile}.
     *
     * @param outputFile the output file
     * @param replaceMap replace map
     * @since 1.0.0
     */
    @SneakyThrows
    @Contract(pure = true)
    public FileWriter(File outputFile, Map<String, String> replaceMap) {
        this.outputFile = outputFile;
        this.replaceMap = replaceMap;
        this.createFileIfNecessary(this.outputFile);
    }

    /**
     * Write build properties.
     *
     * @param properties properties
     * @throws IOException the io exception
     * @since 1.0.0
     */
    public void write(@NotNull Properties properties) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
            properties.store(outputStream, "Properties");
        }
    }

    /**
     * 写文件
     *
     * @param file file
     * @since 1.0.0
     */
    public void write(String file) {
        URL url = FileUtils.class.getClassLoader().getResource(file);
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                String content = IOUtils.toString(inputStream, UTF_8);
                this.writeContent(content);
            } catch (IOException ignored) {
                throw new IllegalStateException("[INFO] 文件拷贝失败: file = " + file);
            }
        }
    }


    /**
     * 直接使用 file 写入 outputFile
     *
     * @param file file
     * @throws IOException io exception
     * @since 1.0.0
     */
    public void write(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            String content = IOUtils.toString(inputStream, UTF_8);
            this.writeContent(content);
        }
    }

    /**
     * 直接使用 content 写入 outputFile
     *
     * @param content content
     * @throws IOException io exception
     * @since 1.0.0
     */
    public void writeContent(String content) throws IOException {
        if (CollectionUtil.isNotEmpty(this.replaceMap)) {
            for (Map.Entry<String, String> entry : this.replaceMap.entrySet()) {
                content = content.replace(entry.getKey(), entry.getValue());
            }
        }
        try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
            IOUtils.copy(new StringReader(content), outputStream, UTF_8);
        }
    }

    /**
     * 文件不存在则递归创建目录和文件
     *
     * @param file file
     * @throws IOException io exception
     * @since 1.0.0
     */
    private void createFileIfNecessary(@NotNull File file) throws IOException {
        if (file.exists()) {
            return;
        }
        File parent = file.getParentFile();
        if (!parent.isDirectory() && !parent.mkdirs()) {
            throw new IllegalStateException(
                "Cannot create parent directory for '" + this.outputFile.getAbsolutePath() + "'");
        }
        if (!file.createNewFile()) {
            throw new IllegalStateException("Cannot create target file '" + this.outputFile.getAbsolutePath() + "'");
        }
    }
}
