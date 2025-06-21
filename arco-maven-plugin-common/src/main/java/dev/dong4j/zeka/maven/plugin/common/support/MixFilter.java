package dev.dong4j.zeka.maven.plugin.common.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 复合过滤器,实际上内部维护一个过滤器的{@link LinkedHashSet}集合, 提供添加/删除以及链式拼接的方法来混合多个子过滤器, 该过滤器的具体逻辑由子类拓展.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @see AllFilter
 * @see AnyFilter
 * @since 1.0.0
 */
public abstract class MixFilter implements Filter {
    /** Filters */
    protected final Set<Filter> filters;

    /**
     * Mix filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    protected MixFilter(Filter... filters) {
        this(Arrays.asList(filters));
    }

    /**
     * Mix filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    protected MixFilter(Collection<? extends Filter> filters) {
        this.filters = filters != null ? new LinkedHashSet<>(filters) : new LinkedHashSet<>();
    }

    /**
     * 添加过滤器
     *
     * @param filter 过滤器
     * @return 添加成功 : true    否则: false 即代表重复添加
     * @since 1.0.0
     */
    public boolean add(Filter filter) {
        return this.filters.add(filter);
    }

    /**
     * 删除过滤器
     *
     * @param filter 过滤器
     * @return 删除成功 : true    否则: false 即代表已不存在
     * @since 1.0.0
     */
    public boolean remove(Filter filter) {
        return this.filters.remove(filter);
    }

    /**
     * 支持采用链式调用的方式混合多个过滤器, 其内部调用{@link MixFilter#add(Filter)}且返回this.
     * 该方法设计成abstract其用意是强制子类将方法的返回值类型替换成自身类型.
     *
     * @param filter 过滤器
     * @return this mix filter
     * @since 1.0.0
     */
    public abstract MixFilter mix(Filter filter);

}
