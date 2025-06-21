package dev.dong4j.zeka.maven.plugin.common.support;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * 资源对象
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public interface Resource {

    /**
     * 资源名称
     *
     * @return 资源名称 name
     * @since 1.0.0
     */
    String getName();

    /**
     * 资源URL地址
     *
     * @return URL地址 url
     * @since 1.0.0
     */
    URL getUrl();

    /**
     * 资源输入流
     *
     * @return 输入流 input stream
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    InputStream getInputStream() throws IOException;

}
