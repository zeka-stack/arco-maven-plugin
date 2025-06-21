package dev.dong4j.zeka.maven.plugin.common.support;

import java.net.URL;
import java.util.Collection;

/**
 * ALL逻辑复合过滤器, 即所有过滤器都满足的时候才满足, 只要有一个过滤器不满足就立刻返回不满足, 如果没有过滤器的时候则认为所有过滤器都满足.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class AllFilter extends MixFilter implements Filter {

    /**
     * All filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    public AllFilter(Filter... filters) {
        super(filters);
    }

    /**
     * All filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    public AllFilter(Collection<? extends Filter> filters) {
        super(filters);
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
        Filter[] filters = this.filters.toArray(new Filter[0]);
        for (Filter filter : filters) {
            if (!filter.filtrate(name, url)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Mix all filter
     *
     * @param filter filter
     * @return the all filter
     * @since 1.0.0
     */
    @Override
    public AllFilter mix(Filter filter) {
        this.add(filter);
        return this;
    }

}
