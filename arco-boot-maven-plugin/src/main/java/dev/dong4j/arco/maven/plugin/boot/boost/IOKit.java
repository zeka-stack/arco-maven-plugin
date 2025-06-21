package dev.dong4j.arco.maven.plugin.boot.boost;

import dev.dong4j.arco.maven.plugin.common.support.Loaders;
import dev.dong4j.arco.maven.plugin.common.support.Resource;
import lombok.experimental.UtilityClass;
import org.apache.commons.compress.archivers.jar.JarArchiveEntry;
import org.apache.commons.compress.archivers.jar.JarArchiveOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;

/**
 * I/O 工具
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 00:01
 * @since 1.0.0
 */
@UtilityClass
public class IOKit {

    /**
     * 往JAR包中嵌入框架的classes
     *
     * @param ant ant
     * @param zos jar包输出流
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static void embed(String ant, JarArchiveOutputStream zos) throws IOException {
        Set<String> directories = new HashSet<>();
        Enumeration<Resource> resources = Loaders.ant().load(ant);
        while (resources.hasMoreElements()) {
            Resource resource = resources.nextElement();
            String name = resource.getName();
            String directory = name.substring(0, name.lastIndexOf('/') + 1);
            if (directories.add(directory)) {
                JarArchiveEntry xDirEntry = new JarArchiveEntry(directory);
                xDirEntry.setTime(System.currentTimeMillis());
                zos.putArchiveEntry(xDirEntry);
                zos.closeArchiveEntry();
            }
            JarArchiveEntry xJarEntry = new JarArchiveEntry(name);
            xJarEntry.setTime(System.currentTimeMillis());
            zos.putArchiveEntry(xJarEntry);
            try (InputStream ris = resource.getInputStream()) {
                transfer(ris, zos);
            }
            zos.closeArchiveEntry();
        }
    }

    /**
     * 输入流传输到输出流
     *
     * @param in  输入流
     * @param out 输出流
     * @return 传输长度 long
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static long transfer(InputStream in, OutputStream out) throws IOException {
        long total = 0;
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) != -1) {
            out.write(buffer, 0, length);
            total += length;
        }
        out.flush();
        return total;
    }

    /**
     * 从输入流中读取一行字节码
     *
     * @param in 输入流
     * @return 最前面的一行字节码 byte [ ]
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static byte[] readln(InputStream in) throws IOException {
        int b = in.read();
        if (b == -1) {
            return new byte[0];
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        while (b != -1) {
            switch (b) {
                case '\r':
                    break;
                case '\n':
                    return bos.toByteArray();
                default:
                    bos.write(b);
                    break;
            }
            b = in.read();
        }
        return bos.toByteArray();
    }

    /**
     * 往输出流中写入一行字节码
     *
     * @param out  输出流
     * @param line 一行字节码
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static void writeln(OutputStream out, byte[] line) throws IOException {
        if (line == null) {
            return;
        }
        out.write(line);
        out.write('\r');
        out.write('\n');
    }

    /**
     * 输入流传输到文件
     *
     * @param in   输入流
     * @param file 文件
     * @return 传输长度 long
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static long transfer(InputStream in, File file) throws IOException {
        try (FileOutputStream out = new FileOutputStream(file)) {
            return transfer(in, out);
        }
    }

    /**
     * reader传输到文件
     *
     * @param reader reader
     * @param file   文件
     * @return 传输长度 long
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static long transfer(Reader reader, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file);
             Writer writer = new OutputStreamWriter(out)) {
            return transfer(reader, writer);
        }
    }

    /**
     * reader传输到writer
     *
     * @param reader reader
     * @param writer writer
     * @return 传输长度 long
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    public static long transfer(Reader reader, Writer writer) throws IOException {
        long total = 0;
        char[] buffer = new char[4096];
        int length;
        while ((length = reader.read(buffer)) != -1) {
            writer.write(buffer, 0, length);
            total += length;
        }
        writer.flush();
        return total;
    }

    /**
     * 删除文件, 如果是目录将不递归删除子文件或目录, 等效于delete(file, false);
     *
     * @param file 文件/目录
     * @return 是否删除成功 boolean
     * @since 1.0.0
     */
    public static boolean delete(File file) {
        return delete(file, false);
    }

    /**
     * 删除文件, 如果是目录将递归删除子文件或目录
     *
     * @param file        文件/目录
     * @param recursively recursively
     * @return 是否删除成功 boolean
     * @since 1.0.0
     */
    @SuppressWarnings("java:S4042")
    public static boolean delete(File file, boolean recursively) {
        if (file.isDirectory() && recursively) {
            boolean deleted = true;
            File[] files = file.listFiles();
            for (int i = 0; files != null && i < files.length; i++) {
                deleted &= delete(files[i], true);
            }
            return deleted && file.delete();
        } else {
            return file.delete();
        }
    }

    /**
     * Is relative boolean
     *
     * @param path path
     * @return the boolean
     * @since 1.0.0
     */
    public static boolean isRelative(String path) {
        return !isAbsolute(path);
    }

    /**
     * Is absolute boolean
     *
     * @param path path
     * @return the boolean
     * @since 1.0.0
     */
    public static boolean isAbsolute(String path) {
        if (path.startsWith("/")) {
            return true;
        }
        Set<File> roots = new HashSet<>();
        Collections.addAll(roots, File.listRoots());
        File root = new File(path);
        while (root.getParentFile() != null) {
            root = root.getParentFile();
        }
        return roots.contains(root);
    }

    /**
     * Absolutize string
     *
     * @param path path
     * @return the string
     * @since 1.0.0
     */
    public static String absolutize(String path) {
        return normalize(isAbsolute(path) ? path : System.getProperty("user.dir") + File.separator + path);
    }

    /**
     * Normalize string
     *
     * @param path path
     * @return the string
     * @since 1.0.0
     */
    public static String normalize(String path) {
        return path.replaceAll("[/\\\\]+", "/");
    }
}
