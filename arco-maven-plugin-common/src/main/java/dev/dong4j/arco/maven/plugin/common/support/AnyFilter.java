package dev.dong4j.arco.maven.plugin.common.support;

import java.net.URL;
import java.util.Collection;

/**
 * ANY逻辑复合过滤器, 即任意一个过滤器满足时就满足, 当没有过滤器的时候则认为没有过滤器满足, 也就是不满足.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class AnyFilter extends MixFilter implements Filter {

    /**
     * Any filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    public AnyFilter(Filter... filters) {
        super(filters);
    }

    /**
     * Any filter
     *
     * @param filters filters
     * @since 1.0.0
     */
    public AnyFilter(Collection<? extends Filter> filters) {
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
            if (filter.filtrate(name, url)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Mix any filter
     *
     * @param filter filter
     * @return the any filter
     * @since 1.0.0
     */
    @Override
    public AnyFilter mix(Filter filter) {
        this.add(filter);
        return this;
    }
}
