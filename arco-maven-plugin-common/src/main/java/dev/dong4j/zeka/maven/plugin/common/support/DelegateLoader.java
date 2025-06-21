package dev.dong4j.zeka.maven.plugin.common.support;

/**
 * 委派的资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public abstract class DelegateLoader extends ResourceLoader implements Loader {
    /** Delegate */
    protected final Loader delegate;

    /**
     * Delegate loader
     *
     * @param delegate delegate
     * @since 1.0.0
     */
    protected DelegateLoader(Loader delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate must not be null");
        }
        this.delegate = delegate;
    }

}
