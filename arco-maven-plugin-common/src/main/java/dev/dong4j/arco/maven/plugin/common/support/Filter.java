package dev.dong4j.arco.maven.plugin.common.support;

import java.net.URL;

/**
 * 资源过滤器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public interface Filter {

    /**
     * 过滤资源
     *
     * @param name 资源名称, 即相对路径
     * @param url  资源URL地址
     * @return true : 加载  false: 不加载
     * @since 1.0.0
     */
    boolean filtrate(String name, URL url);

}
