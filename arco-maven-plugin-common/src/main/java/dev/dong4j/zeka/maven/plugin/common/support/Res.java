package dev.dong4j.zeka.maven.plugin.common.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 资源的一个通用实现
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:00
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class Res implements Resource {
    /** Name */
    private final String name;
    /** Url */
    private final URL url;

    /**
     * Res
     *
     * @param name name
     * @param url  url
     * @since 1.0.0
     */
    public Res(String name, URL url) {
        if (name == null) {
            throw new IllegalArgumentException("name must not be null");
        }
        if (url == null) {
            throw new IllegalArgumentException("url must not be null");
        }
        this.name = name;
        this.url = url;
    }

    /**
     * Gets name *
     *
     * @return the name
     * @since 1.0.0
     */
    @Override
    public String getName() {
        return this.name;
    }

    /**
     * Gets url *
     *
     * @return the url
     * @since 1.0.0
     */
    @Override
    public URL getUrl() {
        return this.url;
    }

    /**
     * Gets input stream *
     *
     * @return the input stream
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public InputStream getInputStream() throws IOException {
        return this.url.openStream();
    }

    /**
     * Equals boolean
     *
     * @param o o
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }

        Res that = (Res) o;

        return this.url.equals(that.url);
    }

    /**
     * Hash code int
     *
     * @return the int
     * @since 1.0.0
     */
    @Override
    public int hashCode() {
        return this.url.hashCode();
    }

    /**
     * To string string
     *
     * @return the string
     * @since 1.0.0
     */
    @Override
    public String toString() {
        return this.url.toString();
    }
}
