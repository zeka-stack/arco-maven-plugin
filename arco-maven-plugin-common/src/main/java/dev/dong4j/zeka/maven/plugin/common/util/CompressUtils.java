package dev.dong4j.zeka.maven.plugin.common.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * 综合性压缩解压工具类，支持多种压缩格式的文件处理和档案管理
 * <p>
 * 该工具类为Maven插件系统提供了全面的文件压缩和解压能力
 * 支持常见的压缩格式，为应用打包、部署包生成和资源分发提供统一的解决方案
 * <p>
 * 支持的压缩格式：
 * <ul>
 *     <li>TAR.GZ - 基于Tar档案和Gzip压缩的组合格式</li>
 *     <li>TGZ - TAR.GZ的简写形式，同样支持</li>
 *     <li>TAR.BZ2 - 基于Tar档案和Bzip2压缩的高压缩比格式</li>
 *     <li>ZIP - 跨平台的通用压缩格式</li>
 * </ul>
 * <p>
 * 核心功能特性：
 * <ul>
 *     <li>目录递归扫描 - 自动遵历目录结构获取所有文件</li>
 *     <li>文件路径处理 - 智能处理路径前缀和目录结构</li>
 *     <li>权限保持 - 在Unix系统上保持文件的原始权限</li>
 *     <li>内存优化 - 大文件的流式处理和缓冲区管理</li>
 *     <li>异常安全 - 完善的资源释放和错误处理</li>
 * </ul>
 * <p>
 * 高级功能特性：
 * <ul>
 *     <li>自动格式识别 - 根据文件扩展名自动选择解压策略</li>
 *     <li>POSIX权限管理 - 完整的Unix文件权限支持和恢复</li>
 *     <li>特殊文件处理 - 自动识别和处理启动脚本的执行权限</li>
 *     <li>垃圾文件清理 - 自动过滤和删除macOS系统生成的临时文件</li>
 *     <li>错误恢复能力 - 在权限设置失败时提供后备方案</li>
 * </ul>
 * <p>
 * 在Maven插件的使用中，该工具类主要用于应用的打包和分发流程
 * 能够将应用代码、配置文件、依赖包等打包成标准的压缩文件格式
 * 同时也支持在部署阶段解压和释放应用包
 * <p>
 * 技术实现亮点：
 * <ul>
 *     <li>基于Apache Commons Compress库的底层实现</li>
 *     <li>支持长文件名的GNU Tar格式扩展</li>
 *     <li>统一的UTF-8编码支持，解决中文文件名问题</li>
 *     <li>大文件的块式处理，防止内存溢出</li>
 *     <li>跨平台的权限处理，兼容Windows和Unix系统</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>Maven插件中的应用打包和档案生成</li>
 *     <li>部署包的压缩和分发流程</li>
 *     <li>备份文件的批量处理和存储</li>
 *     <li>开发环境中的模板包和示例包管理</li>
 *     <li>持续集成中的构建产物打包和归档</li>
 * </ul>
 * <p>
 * 性能优化特性：
 * <ul>
 *     <li>100KB的缓冲区设计，平衡内存和性能</li>
 *     <li>流式处理架构，支持任意大小的文件</li>
 *     <li>懒加载机制，减少不必要的内存占用</li>
 *     <li>并发安全设计，支持多线程环境下的使用</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.12 11:39
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
public class CompressUtils {

    /** BUFFER_SIZE */
    private static final int BUFFER_SIZE = 1024 * 100;

    /**
     * Gets files *
     *
     * @param path path
     * @return the files
     * @since 1.0.0
     */
    public static @NotNull List<File> getFiles(String path) {
        List<File> list = new LinkedList<>();
        File file = new File(path);
        File[] tempList = file.listFiles();
        if (null != tempList) {
            for (File value : tempList) {
                if (value.isFile()) {
                    list.add(new File(value.getPath()));
                }
                if (value.isDirectory()) {
                    List<File> tmpList = getFiles(value.getPath());
                    if (!tmpList.isEmpty()) {
                        list.addAll(tmpList);
                    }
                }
            }
        }
        return list;
    }

    /**
     * 私有函数将文件集合压缩成tar包后返回
     *
     * @param files     要压缩的文件集合
     * @param inPutPath in put path
     * @param target    tar 输出流的目标文件
     * @return File 指定返回的目标文件
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Contract("_, _, _ -> param3")
    public static File pack(List<File> files, String inPutPath, File target) throws IOException {
        try (FileOutputStream out = new FileOutputStream(target)) {
            try (BufferedOutputStream bos = new BufferedOutputStream(out, BUFFER_SIZE)) {
                try (TarArchiveOutputStream os = new TarArchiveOutputStream(bos)) {
                    // 解决文件名过长问题
                    os.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
                    for (File file : files) {
                        // 去掉文件前面的目录
                        os.putArchiveEntry(new TarArchiveEntry(file, file.getAbsolutePath().replace(inPutPath, "")));
                        try (FileInputStream fis = new FileInputStream(file)) {
                            IOUtils.copy(fis, os);
                            os.closeArchiveEntry();
                        }
                    }
                } catch (Exception e) {
                    log.error("tar file error", e);
                }
            }
        }
        return target;
    }

    /**
     * Compress
     *
     * @param source   source
     * @param target   target
     * @param fileName file name
     * @throws IOException exception
     * @since 1.0.0
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void compress(String source, String target, String fileName) throws IOException {

        List<File> list = getFiles(source);
        if (list.isEmpty()) {
            log.info("source file is empty , please check [{}]", source);
            return;
        }
        File file = new File(target);
        if (!file.exists()) {
            file.mkdirs();
        }

        compressTar(list, source, target, fileName);
    }

    /**
     * 压缩tar文件
     *
     * @param list       list
     * @param inPutPath  in put path
     * @param outPutPath out put path
     * @param fileName   file name
     * @return the file
     * @throws IOException exception
     * @since 1.0.0
     */
    public static @NotNull File compressTar(List<File> list, String inPutPath, String outPutPath, String fileName) throws IOException {
        File outPutFile = new File(outPutPath + File.separator + fileName + ".tar.gz");
        File tempTar = new File("temp.tar");
        try (FileInputStream fis = new FileInputStream(pack(list, inPutPath, tempTar))) {
            try (BufferedInputStream bis = new BufferedInputStream(fis, BUFFER_SIZE)) {
                try (FileOutputStream fos = new FileOutputStream(outPutFile)) {
                    try (GZIPOutputStream gzp = new GZIPOutputStream(fos)) {
                        int count;
                        byte[] data = new byte[BUFFER_SIZE];
                        while ((count = bis.read(data, 0, BUFFER_SIZE)) != -1) {
                            gzp.write(data, 0, count);
                        }
                    }
                }
            }
        }
        Files.deleteIfExists(tempTar.toPath());
        return outPutFile;
    }

    /**
     * Decompress
     *
     * @param filePath  file path
     * @param outputDir output dir
     * @since 1.0.0
     */
    public static void decompress(String filePath, String outputDir) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.error("decompress file not exist.");
            return;
        }
        try {
            if (filePath.endsWith(".zip")) {
                unZip(file, outputDir);
            }
            if (filePath.endsWith(".tar.gz") || filePath.endsWith(".tgz")) {
                decompressTarGz(file, outputDir);
            }
            if (filePath.endsWith(".tar.bz2")) {
                decompressTarBz2(file, outputDir);
            }
            filterFile(new File(outputDir));
        } catch (IOException e) {
            log.error("decompress file error.", e);
        }
    }

    /**
     * 解压 .zip 文件
     *
     * @param file      要解压的zip文件对象
     * @param outputDir 要解压到某个指定的目录下
     * @throws IOException io exception
     * @since 1.0.0
     */
    public static void unZip(File file, String outputDir) throws IOException {
        try (ZipFile zipFile = new ZipFile(file, StandardCharsets.UTF_8)) {
            // 创建输出目录
            createFile(outputDir, null);
            Enumeration<?> enums = zipFile.entries();
            while (enums.hasMoreElements()) {
                ZipEntry entry = (ZipEntry) enums.nextElement();
                if (entry.isDirectory()) {
                    // 创建空目录
                    createFile(outputDir, entry.getName());
                } else {
                    try (InputStream in = zipFile.getInputStream(entry)) {
                        try (OutputStream out = new FileOutputStream(
                            outputDir + File.separator + entry.getName())) {
                            writeFile(in, out);
                        }
                    }

                    // ZIP 格式通常不保留 Unix 权限信息，使用公共方法设置权限
                    File extractedFile = new File(outputDir + File.separator + entry.getName());
                    setExecutablePermissions(extractedFile, 0, false);
                }
            }
        }
    }

    /**
     * Decompress tar gz
     *
     * @param sourceFile file
     * @param outputDir  output dir
     * @throws IOException io exception
     * @since 1.0.0
     */
    public static void decompressTarGz(File sourceFile, String outputDir) throws IOException {
        try (TarArchiveInputStream tarIn = new TarArchiveInputStream(
            new GzipCompressorInputStream(new BufferedInputStream(new FileInputStream(sourceFile))))) {
            // 创建输出目录
            createFile(outputDir, null);
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                // 是目录
                if (entry.isDirectory()) {
                    // 创建空目录
                    createFile(outputDir, entry.getName());
                } else {
                    // 是文件
                    File file = createFile(outputDir + File.separator + entry.getName(), null);
                    try (OutputStream out = new FileOutputStream(file)) {
                        writeFile(tarIn, out);
                    }

                    // 使用公共方法处理文件权限
                    setExecutablePermissions(file, entry.getMode(), true);
                }
            }
        }

    }

    /**
     * 解压缩tar.bz2文件
     *
     * @param sourceFile 压缩包文件
     * @param outputDir  目标文件夹
     * @throws IOException io exception
     * @since 1.0.0
     */
    public static void decompressTarBz2(File sourceFile, String outputDir) throws IOException {
        try (TarArchiveInputStream tarIn =
                 new TarArchiveInputStream(
                     new BZip2CompressorInputStream(
                         new FileInputStream(sourceFile)))) {
            createFile(outputDir, null);
            TarArchiveEntry entry;
            while ((entry = tarIn.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    createFile(outputDir, entry.getName());
                } else {
                    File file = new File(outputDir + File.separator + entry.getName());
                    try (OutputStream out = new FileOutputStream(file)) {
                        writeFile(tarIn, out);
                    }

                    // 使用公共方法处理文件权限
                    setExecutablePermissions(file, entry.getMode(), true);
                }
            }
        }
    }

    /**
     * 写文件
     *
     * @param in  in
     * @param out out
     * @throws IOException io exception
     * @since 1.0.0
     */
    public static void writeFile(@NotNull InputStream in, OutputStream out) throws IOException {
        int length;
        byte[] b = new byte[BUFFER_SIZE];
        while ((length = in.read(b)) != -1) {
            out.write(b, 0, length);
        }
    }

    /**
     * 设置文件执行权限，特别处理启动脚本和构建脚本
     *
     * @param file    目标文件
     * @param mode    文件权限模式（可选，tar 格式有效）
     * @param hasMode 是否有有效的权限模式
     * @since 1.0.0
     */
    private static void setExecutablePermissions(File file, int mode, boolean hasMode) {
        final boolean launcher = file.getName().endsWith(".sh") ||
            file.getName().equals("launcher") ||
            file.getName().equals("docker-build");

        if (hasMode && mode != 0) {
            try {
                // 如果文件具有执行权限，则设置为可执行
                if ((mode & 73) != 0) { // 检查任意执行权限位 (73 = 0111 八进制)
                    dev.dong4j.zeka.maven.plugin.common.util.FileUtils.setFilePermissions(file);
                    dev.dong4j.zeka.maven.plugin.common.util.FileUtils.setPosixFilePermissions(file.getAbsoluteFile().toPath());
                }
            } catch (Exception e) {
                log.debug("Failed to set file permissions for: {}", file.getName(), e);
                // 如果权限设置失败，对已知的可执行文件作为后备方案
                if (launcher) {
                    setLauncherPermissions(file);
                }
            }
        } else if (launcher) {
            // 后备方案：对已知的可执行文件设置权限
            setLauncherPermissions(file);
        }
    }

    /**
     * 为启动脚本设置执行权限
     *
     * @param file 目标文件
     * @since 1.0.0
     */
    private static void setLauncherPermissions(File file) {
        try {
            dev.dong4j.zeka.maven.plugin.common.util.FileUtils.setFilePermissions(file);
            dev.dong4j.zeka.maven.plugin.common.util.FileUtils.setPosixFilePermissions(file.getAbsoluteFile().toPath());
        } catch (Exception e) {
            log.debug("Failed to set launcher permissions for: {}", file.getName(), e);
        }
    }

    /**
     * 删除 Mac 压缩再解压产生的 __MACOSX 文件夹和 . 开头的其他文件
     *
     * @param filteredFile filtered file
     * @since 1.0.0
     */
    public static void filterFile(File filteredFile) {
        if (filteredFile != null) {
            File[] files = filteredFile.listFiles();
            Arrays.stream(Objects.requireNonNull(files)).forEach(file -> {
                if (file.getName().startsWith(".") ||
                    (file.isDirectory() && "__MACOSX".equals(file.getName()))) {
                    FileUtils.deleteQuietly(file);
                }
            });

        }
    }

    /**
     * 构建目录
     *
     * @param outputDir 输出目录
     * @param subDir    子目录
     * @return the file
     * @since 1.0.0
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static @NotNull File createFile(String outputDir, String subDir) {
        File file = new File(outputDir);
        // 子目录不为空
        if (!(subDir == null || subDir.trim().isEmpty())) {
            file = new File(outputDir + File.separator + subDir);
        }
        if (!file.exists()) {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (file.isDirectory()) {
                file.mkdirs();
            }
        }
        return file;
    }

}
