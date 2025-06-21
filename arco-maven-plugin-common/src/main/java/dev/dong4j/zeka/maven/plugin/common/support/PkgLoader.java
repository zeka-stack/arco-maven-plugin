package dev.dong4j.zeka.maven.plugin.common.support;

import java.io.IOException;
import java.util.Enumeration;

/**
 * 包名表达式资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class PkgLoader extends DelegateLoader implements Loader {

    /**
     * Pkg loader
     *
     * @since 1.0.0
     */
    public PkgLoader() {
        this(new StdLoader());
    }

    /**
     * Pkg loader
     *
     * @param classLoader class loader
     * @since 1.0.0
     */
    public PkgLoader(ClassLoader classLoader) {
        this(new StdLoader(classLoader));
    }

    /**
     * Pkg loader
     *
     * @param delegate delegate
     * @since 1.0.0
     */
    public PkgLoader(Loader delegate) {
        super(delegate);
    }

    /**
     * Load enumeration
     *
     * @param pkg         pkg
     * @param recursively recursively
     * @param filter      filter
     * @return the enumeration
     * @throws IOException io exception
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String pkg, boolean recursively, Filter filter) throws IOException {
        String path = pkg.replace('.', '/');
        return this.delegate.load(path, recursively, filter);
    }
}
