package dev.dong4j.zeka.maven.plugin.common.support;

import org.jetbrains.annotations.Contract;

/**
 * 正则表达式资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class RegexLoader extends PatternLoader implements Loader {

    /**
     * Regex loader
     *
     * @since 1.0.0
     */
    public RegexLoader() {
        this(new StdLoader());
    }

    /**
     * Regex loader
     *
     * @param classLoader class loader
     * @since 1.0.0
     */
    public RegexLoader(ClassLoader classLoader) {
        this(new StdLoader(classLoader));
    }

    /**
     * Regex loader
     *
     * @param delegate delegate
     * @since 1.0.0
     */
    @Contract("null -> fail")
    public RegexLoader(Loader delegate) {
        super(delegate);
    }

    /**
     * Path string
     *
     * @param pattern pattern
     * @return the string
     * @since 1.0.0
     */
    @Override
    protected String path(String pattern) {
        return "";
    }

    /**
     * Recursively boolean
     *
     * @param pattern pattern
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    protected boolean recursively(String pattern) {
        return true;
    }

    /**
     * Filter filter
     *
     * @param pattern pattern
     * @return the filter
     * @since 1.0.0
     */
    @Override
    protected Filter filter(String pattern) {
        return new RegexFilter(pattern);
    }
}
