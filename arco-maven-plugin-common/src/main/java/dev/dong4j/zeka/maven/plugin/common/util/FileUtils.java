package dev.dong4j.zeka.maven.plugin.common.util;

import com.google.common.base.Joiner;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.01.27 18:29
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
@SuppressWarnings("all")
public class FileUtils extends org.apache.commons.io.FileUtils {
    /** UTF_8 */
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    /**
     * 写 assembly.xml 和启动脚本文件
     *
     * @param file   the file
     * @param output the output
     * @throws IOException the io exception
     * @since 1.0.0
     */
    public static void writeFile(String file, OutputStream output) {
        URL url = FileUtils.class.getClassLoader().getResource(file);
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                writeFile(inputStream, output);
            } catch (IOException ignored) {
                log.info("[INFO] 文件拷贝失败: file =  [{}]", file);
            }
        }
    }

    /**
     * Read to string
     *
     * @param file file
     * @return the string
     * @since 1.5.0
     */
    public static String readToString(String file) {
        URL url = FileUtils.class.getClassLoader().getResource(file);
        if (url != null) {
            try (InputStream inputStream = url.openStream()) {
                return IOUtils.toString(inputStream, UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException(file + " 不存在");
    }

    /**
     * Write package file *
     *
     * @param inputStream input stream
     * @param output      output
     * @throws IOException io exception
     * @since 1.0.0
     */
    public static void writeFile(InputStream inputStream, OutputStream output) throws IOException {
        IOUtils.copy(inputStream, output);
    }

    /**
     * 判断是否存在某个 class
     *
     * @param name the name
     * @return the boolean
     * @since 1.0.0
     */
    public static boolean isPresent(String name) {
        try {
            Thread.currentThread().getContextClassLoader().loadClass(name);
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }

    /**
     * 拼接不同平台下的文件路径, 末尾不含路径分隔符, 不会删除第一个 path 的 File.separator(如果有的话)
     * String path1 = FileUtils.appendPath("a", "b", "c");
     * String path2 = FileUtils.appendPath("a", "b/", "c");
     * String path3 = FileUtils.appendPath("a", "/b/", "/c");
     * String path4 = FileUtils.appendPath("a/", "/b/", "/c");
     * 都将输出 a/b/c
     * String path5 = FileUtils.appendPath("/a", "/b/", "/c");
     * 将输出 /a/b/c
     *
     * @param paths paths
     * @return the string
     * @since 1.3.0
     */
    public static @NotNull String appendPath(@NotNull String... paths) {
        // 删除前缀后后缀
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].endsWith(File.separator)) {
                paths[i] = paths[i].substring(0, paths[i].lastIndexOf(File.separator));
            }
            // 第一个路径不删除前缀
            if (i == 0) {
                continue;
            }
            if (paths[i].startsWith(File.separator)) {
                paths[i] = paths[i].substring(paths[i].indexOf(File.separator) + 1);
            }
        }

        return String.join(File.separator, paths);
    }

    /**
     * 删除空目录
     *
     * @param dir 将要删除的目录路径
     * @since 1.3.0
     */
    public static void doDeleteEmptyDir(String dir) {
        boolean success = (new File(dir)).delete();
        if (success) {
            log.debug("Successfully deleted empty directory: [{}]", dir);
        } else {
            log.debug("Failed to delete empty directory: [{}]", dir);
        }
    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     * @since 1.3.0
     */
    public static boolean deleteDir(@NotNull File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            // 递归删除目录中的子目录下
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空, 可以删除
        return dir.delete();
    }

    /**
     * 递归删除指定目录及文件
     *
     * @param first first
     * @param more  more
     * @return the boolean
     * @since 1.0.0
     */
    @SneakyThrows
    public static boolean deleteFiles(String first, String... more) {
        Path start = Paths.get(first, more);
        if (start.toFile().exists()) {
            Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException e) {
                    if (e == null) {
                        try {
                            Files.delete(dir);
                        } catch (IOException ex) {
                        }
                        return FileVisitResult.CONTINUE;
                    }
                    // 如果存在异常, 则说明文件不存在
                    return FileVisitResult.SKIP_SUBTREE;
                }
            });
        }

        return true;
    }

    /**
     * 设置所有者对于此抽象路径名执行权限
     *
     * @param file file
     * @since 1.5.0
     */
    public static void setFilePermissions(@NotNull File file) {
        if (!file.setExecutable(true, true)) {
            log.error(Joiner.on(" ").join("Unable to set executable:", file.getName()));
        } else {
            log.debug(Joiner.on(" ").join("Set executable for", file.getName()));
        }
    }

    /**
     * 为脚本设置可执行权限
     *
     * @param path path
     * @since 1.5.0
     */
    public static void setPosixFilePermissions(Path path) {
        Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwxr-xr--");

        try {
            Files.setPosixFilePermissions(path, permissions);
            log.debug(Joiner.on(" ").join("Set Posix File Permissions for", path, "as", permissions));
        } catch (IOException e) {
            log.error("Failed attempted Posix permissions", e);
        } catch (UnsupportedOperationException e) {
            // Attempting but don't care about status if it fails
            log.debug("Failed attempted Posix permissions", e);
        }
    }
}
