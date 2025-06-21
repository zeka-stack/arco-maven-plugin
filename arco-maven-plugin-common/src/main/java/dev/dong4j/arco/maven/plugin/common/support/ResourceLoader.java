package dev.dong4j.arco.maven.plugin.common.support;

import java.io.IOException;
import java.util.Enumeration;
import java.util.NoSuchElementException;

/**
 * 资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public abstract class ResourceLoader implements Loader {

    /**
     * Load enumeration
     *
     * @param path path
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path) throws IOException {
        return this.load(path, false, Filters.ALWAYS);
    }

    /**
     * Load enumeration
     *
     * @param path        path
     * @param recursively recursively
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path, boolean recursively) throws IOException {
        return this.load(path, recursively, Filters.ALWAYS);
    }

    /**
     * Load enumeration
     *
     * @param path   path
     * @param filter filter
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path, Filter filter) throws IOException {
        return this.load(path, true, filter);
    }

    /**
     * 资源枚举器
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 01:02
     * @since 1.0.0
     */
    @SuppressWarnings("java:S1150")
    protected abstract static class ResourceEnumerator implements Enumeration<Resource> {
        /** Next */
        protected Resource next;

        /**
         * Next element resource
         *
         * @return the resource
         * @since 1.0.0
         */
        @Override
        public Resource nextElement() {
            if (this.hasMoreElements()) {
                Resource resource = this.next;
                this.next = null;
                return resource;
            } else {
                throw new NoSuchElementException();
            }
        }

    }
}
