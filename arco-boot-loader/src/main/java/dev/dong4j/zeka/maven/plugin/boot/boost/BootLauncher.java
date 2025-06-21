package dev.dong4j.zeka.maven.plugin.boot.boost;

import dev.dong4j.zeka.maven.plugin.boot.loader.PropertiesLauncher;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.Archive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.ExplodedArchive;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * <p>Description: Spring Boot JAR 增强启动器
 * <p>
 * https://docs.spring.io/spring-boot/docs/2.2.1.RELEASE/reference/htmlsingle/#executable-jar
 * https://blog.csdn.net/hengyunabc/article/details/50120001
 * {@code
 * --slot.root=${DEPLOY_DIR}/ \
 * --slot.path=patch/ \
 * --slot.path=plugin/ \
 * }*
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 03:40
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class BootLauncher extends PropertiesLauncher {
    /** SLOT_ROOT */
    private static final String SLOT_ROOT = "--slot.root=";
    /** SLOT_PATH */
    private static final String SLOT_PATH = "--slot.path=";

    /** Root */
    private final String root;
    /** Paths */
    private final List<String> paths;

    /**
     * Boot launcher
     *
     * @param root  root
     * @param paths paths
     * @since 1.0.0
     */
    @Contract("null, _ -> fail")
    public BootLauncher(String root, List<String> paths) {
        if (root == null) {
            throw new NullPointerException("root must not be null");
        }
        if (paths == null) {
            paths = Collections.emptyList();
        }
        this.root = root;
        this.paths = paths;
    }

    /**
     * java -jar 是最先调用的方法, 最后会通过反射调用业务启动类的 main()
     *
     * @param args args
     * @throws Exception exception
     * @since 1.0.0
     */
    public static void main(String @NotNull [] args) throws Exception {
        String root = System.getProperty("user.dir");
        List<String> paths = new ArrayList<>();
        List<String> arguments = new ArrayList<>();
        for (String arg : args) {
            if (arg.startsWith(SLOT_ROOT)) {
                root = arg.substring(SLOT_ROOT.length());
            } else if (arg.startsWith(SLOT_PATH)) {
                String path = arg.substring(SLOT_PATH.length());
                paths.add(path);
            } else {
                arguments.add(arg);
            }
        }
        new BootLauncher(root, paths).launch(arguments.toArray(new String[0]));
    }

    /**
     * 重写 {@link PropertiesLauncher#createClassLoader(java.util.List)}, 添加 {@link BootLauncher#SLOT_PATH} 指定的 patch 和 plugin 内 jar
     *
     * @param archives archives
     * @return the class loader
     * @throws Exception exception
     * @since 1.5.0
     */
    @Override
    protected ClassLoader createClassLoader(@NotNull List<Archive> archives) throws Exception {
        List<Archive> extendArchives = new ArrayList<>();
        this.paths.forEach(p -> {
            try {
                List<Archive> classPathArchives = this.getClassPathArchives(this.root + p);
                for (Archive archive : classPathArchives) {
                    if (archive instanceof ExplodedArchive) {
                        List<Archive> nested = new ArrayList<>(archive.getNestedArchives(new ArchiveEntryFilter()));
                        nested.add(0, archive);
                        extendArchives.addAll(nested);
                    } else {
                        extendArchives.add(archive);
                    }
                }
            } catch (Exception e) {
                this.debug(e.getMessage());
            }
        });

        Set<Archive> allArchives = new LinkedHashSet<>(archives.size() + extendArchives.size());
        // patch 和 plugin 的 jar 优先级高于 lib
        allArchives.addAll(extendArchives);
        allArchives.addAll(archives);
        return super.createClassLoader(new ArrayList<>(allArchives));
    }

}
