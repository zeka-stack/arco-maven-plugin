package dev.dong4j.zeka.maven.plugin.common.support;

/**
 * ANT风格路径过滤器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class AntFilter extends RegexFilter implements Filter {
    /** SYMBOLS */
    private static final String[] SYMBOLS = {"\\", "$", "(", ")", "+", ".", "[", "]", "^", "{", "}", "|"};

    /**
     * Ant filter
     *
     * @param ant ant
     * @since 1.0.0
     */
    public AntFilter(String ant) {
        super(convert(ant));
    }

    /**
     * 将ANT风格路径表达式转换成正则表达式
     *
     * @param ant ANT风格路径表达式
     * @return 正则表达式 string
     * @since 1.0.0
     */
    private static String convert(String ant) {
        String regex = ant;
        for (String symbol : SYMBOLS) {
            regex = regex.replace(symbol, '\\' + symbol);
        }
        regex = regex.replace("?", ".{1}");
        regex = regex.replace("**/", "(.{0,}?/){0,}?");
        regex = regex.replace("**", ".{0,}?");
        regex = regex.replace("*", "[^/]{0,}?");
        while (regex.startsWith("/")) {
            regex = regex.substring(1);
        }
        while (regex.endsWith("/")) {
            regex = regex.substring(0, regex.length() - 1);
        }
        return regex;
    }

}
