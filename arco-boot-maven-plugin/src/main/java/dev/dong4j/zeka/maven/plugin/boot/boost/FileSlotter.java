package dev.dong4j.zeka.maven.plugin.boot.boost;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 文件开槽机
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 00:01
 * @since 1.0.0
 */
public abstract class FileSlotter implements Slotter {

    /**
     * Slot *
     *
     * @param src  src
     * @param dest dest
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void slot(String src, String dest) throws IOException {
        this.slot(new File(src), new File(dest));
    }

    /**
     * Slot *
     *
     * @param src  src
     * @param dest dest
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public void slot(File src, File dest) throws IOException {
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
            this.slot(in, out);
        }
    }
}
