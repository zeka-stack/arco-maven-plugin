package dev.dong4j.zeka.maven.plugin.boot.loader;

import java.net.URI;
import javax.tools.SimpleJavaFileObject;

/**
 * Created by gaofla on 2018/3/14.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.0.0
 */
public class CharSequenceJavaFileObject extends SimpleJavaFileObject {

    /** Content */
    private final CharSequence content;


    /**
     * Char sequence java file object
     *
     * @param className class name
     * @param content   content
     * @since 1.0.0
     */
    public CharSequenceJavaFileObject(String className,
                                      CharSequence content) {
        super(URI.create("string:///" + className.replace('.', '/')
            + Kind.SOURCE.extension), Kind.SOURCE);
        this.content = content;
    }

    /**
     * Gets char content *
     *
     * @param ignoreEncodingErrors ignore encoding errors
     * @return the char content
     * @since 1.0.0
     */
    @Override
    public CharSequence getCharContent(
        boolean ignoreEncodingErrors) {
        return this.content;
    }
}
