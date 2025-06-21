package dev.dong4j.zeka.maven.plugin.boot.loader;

import javax.tools.SimpleJavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

/**
 * Created by gaofla on 2018/3/14.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
public class JavaClassObject extends SimpleJavaFileObject {

    /** Bos */
    protected final ByteArrayOutputStream bos =
        new ByteArrayOutputStream();


    /**
     * Java class object
     *
     * @param name name
     * @param kind kind
     * @since 1.5.0
     */
    public JavaClassObject(String name, Kind kind) {
        super(URI.create("string:///" + name.replace('.', '/')
            + kind.extension), kind);
    }


    /**
     * Get bytes
     *
     * @return the byte [ ]
     * @since 1.5.0
     */
    public byte[] getBytes() {
        return this.bos.toByteArray();
    }

    /**
     * Open output stream
     *
     * @return the output stream
     * @throws IOException io exception
     * @since 1.5.0
     */
    @Override
    public OutputStream openOutputStream() throws IOException {
        return this.bos;

    }
}
