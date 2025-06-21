package dev.dong4j.arco.maven.plugin.common.support;

import java.net.URL;
import java.util.regex.Pattern;

/**
 * 正则表达式过滤器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class RegexFilter implements Filter {
    /** Pattern */
    private final Pattern pattern;

    /**
     * Regex filter
     *
     * @param regex regex
     * @since 1.0.0
     */
    public RegexFilter(String regex) {
        this(Pattern.compile(regex));
    }

    /**
     * Regex filter
     *
     * @param pattern pattern
     * @since 1.0.0
     */
    public RegexFilter(Pattern pattern) {
        if (pattern == null) {
            throw new IllegalArgumentException("pattern must not be null");
        }
        this.pattern = pattern;
    }

    /**
     * Filtrate boolean
     *
     * @param name name
     * @param url  url
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean filtrate(String name, URL url) {
        return this.pattern.matcher(name).matches();
    }
}
