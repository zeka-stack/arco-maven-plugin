package dev.dong4j.zeka.maven.plugin.common;

import cn.hutool.core.collection.CollectionUtil;
import com.google.common.collect.Maps;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 19:08
 * @since 1.0.0
 */
@Slf4j
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
        write(file, false);
    }

    /**
     * @param file         文件
     * @param sameFileName 是否写入相同的文件名(会覆盖 outputFile)
     */
    public void write(String file, boolean sameFileName) {
        URL url = FileUtils.class.getClassLoader().getResource(file);
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                String content = IOUtils.toString(inputStream, UTF_8);
                if (sameFileName) {
                    // 使用 file 创建 File, 然后获取文件名, 传入 writeSameContent
                    File tempFile = new File(file);
                    String fileName = tempFile.getName(); // 获取文件名
                    this.writeSameContent(fileName, content);
                } else {
                    this.writeContent(content);
                }
            } catch (IOException ignored) {
                throw new IllegalStateException("[INFO] 文件拷贝失败: file = " + file);
            }
        }
    }

    public void writeSameContent(String fileName, String content) throws IOException {
        if (CollectionUtil.isNotEmpty(this.replaceMap)) {
            for (Map.Entry<String, String> entry : this.replaceMap.entrySet()) {
                content = content.replace(entry.getKey(), entry.getValue());
            }
        }

        // 获取 this.outputFile 的目录名, 然后拼接 fileName, 创建一个新的 File, 即最终写入的文件
        File parentDir = this.outputFile.getParentFile();
        File targetFile = new File(parentDir, fileName);

        try (FileOutputStream outputStream = new FileOutputStream(targetFile)) {
            log.info("生成文件: {}", targetFile);
            IOUtils.copy(new StringReader(content), outputStream, UTF_8);
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
            log.info("生成文件: {}", this.outputFile);
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
