package dev.dong4j.zeka.maven.plugin.common.support;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * 过滤器工具类
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public abstract class Filters {
    /**
     * Filters
     *
     * @since 1.9.0
     */
    private Filters() {
    }

    /**
     * 永远返回true的过滤器
     */
    public static final Filter ALWAYS = (name, url) -> true;

    /**
     * 永远返回false的过滤器
     */
    public static final Filter NEVER = (name, url) -> false;

    /**
     * 创建多个子过滤器AND连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器AND连接的混合过滤器 filter
     * @since 1.0.0
     */
    @Contract("_ -> new")
    public static @NotNull Filter all(Filter... filters) {
        return new AllFilter(filters);
    }

    /**
     * 创建多个子过滤器AND连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器AND连接的混合过滤器 filter
     * @since 1.0.0
     */
    @Contract("_ -> new")
    public static @NotNull Filter all(Collection<? extends Filter> filters) {
        return new AllFilter(filters);
    }

    /**
     * 创建多个子过滤器AND连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器AND连接的混合过滤器 filter
     * @since 1.0.0
     */
    public static @NotNull Filter and(Filter... filters) {
        return all(filters);
    }

    /**
     * 创建多个子过滤器AND连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器AND连接的混合过滤器 filter
     * @since 1.0.0
     */
    public static @NotNull Filter and(Collection<? extends Filter> filters) {
        return all(filters);
    }

    /**
     * 创建多个子过滤器OR连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器OR连接的混合过滤器 filter
     * @since 1.0.0
     */
    @Contract("_ -> new")
    public static @NotNull Filter any(Filter... filters) {
        return new AnyFilter(filters);
    }

    /**
     * 创建多个子过滤器OR连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器OR连接的混合过滤器 filter
     * @since 1.0.0
     */
    @Contract("_ -> new")
    public static @NotNull Filter any(Collection<? extends Filter> filters) {
        return new AnyFilter(filters);
    }

    /**
     * 创建多个子过滤器OR连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器OR连接的混合过滤器 filter
     * @since 1.0.0
     */
    public static @NotNull Filter or(Filter... filters) {
        return any(filters);
    }

    /**
     * 创建多个子过滤器OR连接的混合过滤器
     *
     * @param filters 子过滤器
     * @return 多个子过滤器OR连接的混合过滤器 filter
     * @since 1.0.0
     */
    public static @NotNull Filter or(Collection<? extends Filter> filters) {
        return any(filters);
    }

}
