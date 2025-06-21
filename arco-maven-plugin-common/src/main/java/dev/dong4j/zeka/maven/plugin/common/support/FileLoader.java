package dev.dong4j.zeka.maven.plugin.common.support;

import org.jetbrains.annotations.Contract;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 文件资源加载器
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:02
 * @since 1.0.0
 */
public class FileLoader extends ResourceLoader implements Loader {
    /** Context */
    private final URL context;
    /** Root */
    private final File root;

    /**
     * File loader
     *
     * @param root root
     * @throws IOException io exception
     * @since 1.0.0
     */
    public FileLoader(File root) throws IOException {
        this(root.toURI().toURL(), root);
    }

    /**
     * File loader
     *
     * @param fileURL file url
     * @since 1.0.0
     */
    @Contract("null -> fail")
    public FileLoader(URL fileURL) {
        this(fileURL, new File(Uris.decode(fileURL.getPath(), Charset.defaultCharset())));
    }

    /**
     * File loader
     *
     * @param context context
     * @param root    root
     * @since 1.0.0
     */
    @Contract("null, _ -> fail; !null, null -> fail")
    public FileLoader(URL context, File root) {
        if (context == null) {
            throw new IllegalArgumentException("context must not be null");
        }
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        this.context = context;
        this.root = root;
    }

    /**
     * Load enumeration
     *
     * @param path        path
     * @param recursively recursively
     * @param filter      filter
     * @return the enumeration
     * @since 1.0.0
     */
    @Override
    public Enumeration<Resource> load(String path, boolean recursively, Filter filter) {
        return new Enumerator(this.context, this.root, path, recursively, filter != null ? filter : Filters.ALWAYS);
    }

    /**
     * <p>Description: >/p>
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.04.30 01:02
     * @since 1.0.0
     */
    @SuppressWarnings("java:S1150")
    private static class Enumerator extends ResourceEnumerator implements Enumeration<Resource> {
        /** Context */
        private final URL context;
        /** Recursively */
        private final boolean recursively;
        /** Filter */
        private final Filter filter;
        /** Queue */
        private final Queue<File> queue;

        /**
         * Enumerator
         *
         * @param context     context
         * @param root        root
         * @param path        path
         * @param recursively recursively
         * @param filter      filter
         * @since 1.0.0
         */
        Enumerator(URL context, File root, String path, boolean recursively, Filter filter) {
            this.context = context;
            this.recursively = recursively;
            this.filter = filter;
            this.queue = new LinkedList<>();
            File file = new File(root, path);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                for (int i = 0; files != null && i < files.length; i++) {
                    this.queue.offer(files[i]);
                }
            } else {
                this.queue.offer(file);
            }
        }

        /**
         * Has more elements boolean
         *
         * @return the boolean
         * @since 1.0.0
         */
        @Override
        @SuppressWarnings("java:S3776")
        public boolean hasMoreElements() {
            if (this.next != null) {
                return true;
            }
            while (!this.queue.isEmpty()) {
                File file = this.queue.poll();

                if (!file.exists()) {
                    continue;
                }

                if (file.isFile()) {
                    try {
                        String name = this.context.toURI().relativize(file.toURI()).toString();
                        URL url = new URL(this.context, name);
                        if (this.filter.filtrate(name, url)) {
                            this.next = new Res(name, url);
                            return true;
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
                if (file.isDirectory() && this.recursively) {
                    File[] files = file.listFiles();
                    for (int i = 0; files != null && i < files.length; i++) {
                        this.queue.offer(files[i]);
                    }
                    return this.hasMoreElements();
                }
            }

            return false;
        }

    }

}
