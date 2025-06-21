package dev.dong4j.zeka.maven.plugin.common.support;

import java.io.IOException;
import java.util.Enumeration;

/**
 * ANT风格路径资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class AntLoader extends PatternLoader implements Loader {

    /**
     * Ant loader
     *
     * @since 1.0.0
     */
    public AntLoader() {
        this(new StdLoader());
    }

    /**
     * Ant loader
     *
     * @param classLoader class loader
     * @since 1.0.0
     */
    public AntLoader(ClassLoader classLoader) {
        this(new StdLoader(classLoader));
    }

    /**
     * Ant loader
     *
     * @param delegate delegate
     * @since 1.0.0
     */
    public AntLoader(Loader delegate) {
        super(delegate);
    }

    /**
     * Load enumeration
     *
     * @param pattern     pattern
     * @param recursively recursively
     * @param filter      filter
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String pattern, boolean recursively, Filter filter) throws IOException {
        if (Math.max(pattern.indexOf('*'), pattern.indexOf('?')) < 0) {
            return this.delegate.load(pattern, recursively, filter);
        } else {
            return super.load(pattern, recursively, filter);
        }
    }

    /**
     * Path string
     *
     * @param ant ant
     * @return the string
     * @since 1.0.0
     */
    @Override
    protected String path(String ant) {
        int index = Integer.MAX_VALUE - 1;
        if (ant.contains("*") && ant.indexOf('*') < index) {
            index = ant.indexOf('*');
        }
        if (ant.contains("?") && ant.indexOf('?') < index) {
            index = ant.indexOf('?');
        }
        return ant.substring(0, ant.lastIndexOf('/', index) + 1);
    }

    /**
     * Recursively boolean
     *
     * @param ant ant
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    protected boolean recursively(String ant) {
        return true;
    }

    /**
     * Filter filter
     *
     * @param ant ant
     * @return the filter
     * @since 1.0.0
     */
    @Override
    protected Filter filter(String ant) {
        return new AntFilter(ant);
    }
}
