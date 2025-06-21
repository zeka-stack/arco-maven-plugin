package dev.dong4j.zeka.maven.plugin.boot.boost;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 开槽机
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 00:01
 * @since 1.0.0
 */
public interface Slotter {

    /**
     * 开槽, 将目标文件开槽输出至目标文件.
     *
     * @param src  源文件
     * @param dest 目标文件
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    void slot(String src, String dest) throws IOException;

    /**
     * 开槽, 将目标文件开槽输出至目标文件.
     *
     * @param src  源文件
     * @param dest 目标文件
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    void slot(File src, File dest) throws IOException;

    /**
     * 开槽, 将输入流开槽输出至输出流.
     *
     * @param in  输入流
     * @param out 输出流
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    void slot(InputStream in, OutputStream out) throws IOException;

}
