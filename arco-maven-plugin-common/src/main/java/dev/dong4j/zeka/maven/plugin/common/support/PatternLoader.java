package dev.dong4j.zeka.maven.plugin.common.support;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 模式匹配资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public abstract class PatternLoader extends DelegateLoader implements Loader {

    /**
     * Pattern loader
     *
     * @param delegate delegate
     * @since 1.0.0
     */
    protected PatternLoader(Loader delegate) {
        super(delegate);
    }

    /**
     * 加载匹配模式表达式的所有资源, 由于模式表达式中有可能表达了是否递归加载的含义,
     * 所以缺省情况下recursively参数会被忽略, 取而代之的是{@link PatternLoader#recursively(String)}的返回值,
     * 如果字类实现的模式表达式并不能表达是否递归加载的含义, 需要重写该方法以满足更多定制化的需求.
     * 另外当filter参数不为null时, 由模式表达式推导出的过滤器将会和filter参数混合成一个{@link AllFilter}混合过滤器
     *
     * @param pattern     模式表达式
     * @param recursively 递归加载
     * @param filter      过滤器
     * @return 所有匹配模式表达式的资源 enumeration
     * @throws IOException I/O 异常
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String pattern, boolean recursively, Filter filter) throws IOException {
        Filter matcher = this.filter(pattern);
        AllFilter allFilter = new AllFilter();
        if (matcher != null) {
            allFilter.add(matcher);
        }
        if (filter != null) {
            allFilter.add(filter);
        }
        return this.delegate.load(
            this.path(pattern),
            this.recursively(pattern),
            allFilter
        );
    }

    /**
     * 根据资源表达式推导出资源根路径
     *
     * @param pattern 资源表达式
     * @return 资源根路径 string
     * @since 1.0.0
     */
    protected abstract String path(String pattern);

    /**
     * 根据资源表达式推导出是否从根路径开始递归加载
     *
     * @param pattern 资源表达式
     * @return 是否从根路径开始递归加载 boolean
     * @since 1.0.0
     */
    protected abstract boolean recursively(String pattern);

    /**
     * 根据资源表达式推导出资源过滤器
     *
     * @param pattern 资源表达式
     * @return 出资源过滤器 filter
     * @since 1.0.0
     */
    protected abstract Filter filter(String pattern);
}
