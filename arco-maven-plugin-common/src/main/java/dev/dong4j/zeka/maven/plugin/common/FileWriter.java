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
 * Maven插件文件写入器，提供模板文件处理、内容替换和文件生成功能
 * <p>
 * 该类用于处理各种文件写入场景，支持从classpath资源、本地文件读取内容，
 * 并支持动态内容替换和模板处理。统一使用UTF-8编码处理，自动创建目标目录和文件。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 19:08
 * @since 1.0.0
 */
@Slf4j
@SuppressWarnings("all")
public final class FileWriter {
    /**
     * UTF-8字符集常量，用于统一文件编码处理
     *
     * @since 1.0.0
     */
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    /**
     * 输出文件对象，指定文件写入的目标位置
     *
     * @since 1.0.0
     */
    private final File outputFile;

    /**
     * 内容替换映射表，存储所有的占位符替换规则
     *
     * @since 1.0.0
     */
    private final Map<String, String> replaceMap;

    /**
     * 创建文件写入器，使用空的替换映射表
     *
     * @param outputFile 输出文件对象
     * @since 1.0.0
     */
    @Contract(pure = true)
    public FileWriter(File outputFile) {
        this(outputFile, Maps.newHashMap());
    }

    /**
     * 创建文件写入器，支持自定义的内容替换规则
     *
     * @param outputFile 输出文件对象
     * @param replaceMap 内容替换映射表
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
     * 将Properties对象写入到目标文件中
     *
     * @param properties 需要写入的Properties对象
     * @throws IOException 文件写入错误时抛出
     * @since 1.0.0
     */
    public void write(@NotNull Properties properties) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(this.outputFile)) {
            properties.store(outputStream, "Properties");
        }
    }

    /**
     * 从类路径读取文件并写入到目标文件
     *
     * @param file 类路径中的文件路径
     * @since 1.0.0
     */
    public void write(String file) {
        write(file, false);
    }

    /**
     * 从类路径读取文件并支持选择不同的文件写入模式
     *
     * @param file         类路径中的文件路径
     * @param sameFileName 是否使用动态文件名模式
     * @since 1.0.0
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

    /**
     * 在目标目录下创建与源文件同名的文件，并写入处理后的内容
     *
     * @param fileName 文件名
     * @param content  文件内容
     * @throws IOException 文件写入错误时抛出
     * @since 1.0.0
     */
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
     * 直接读取本地文件并写入到目标文件
     *
     * @param file 本地文件对象
     * @throws IOException 文件读取或写入错误时抛出
     * @since 1.0.0
     */
    public void write(File file) throws IOException {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            String content = IOUtils.toString(inputStream, UTF_8);
            this.writeContent(content);
        }
    }

    /**
     * 直接将指定内容写入到目标文件
     *
     * @param content 文件内容
     * @throws IOException 文件写入错误时抛出
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
     * 检查文件是否存在，不存在则递归创建目录和文件
     *
     * @param file 目标文件
     * @throws IOException 文件创建错误时抛出
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
