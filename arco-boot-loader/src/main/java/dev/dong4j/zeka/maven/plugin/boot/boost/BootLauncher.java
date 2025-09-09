package dev.dong4j.zeka.maven.plugin.boot.boost;

import dev.dong4j.zeka.maven.plugin.boot.loader.PropertiesLauncher;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.Archive;
import dev.dong4j.zeka.maven.plugin.boot.loader.archive.ExplodedArchive;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

/**
 * Spring Boot JAR增强启动器，支持插件化和补丁加载机制
 * <p>
 * 该启动器继承自Spring Boot的PropertiesLauncher，提供了增强的类加载机制
 * 支持在运行时动态加载补丁（patch）和插件（plugin）目录中的JAR文件
 * 实现了热更新、插件化部署和灵活的扩展机制
 * <p>
 * 主要特性：
 * - 补丁支持：支持动态加载补丁目录中的JAR文件
 * - 插件支持：支持动态加载插件目录中的JAR文件
 * - 优先级控制：patch和plugin目录中的JAR优先级高于lib目录
 * - 灵活配置：支持通过命令行参数配置路径
 * - 完全兼容：完全兼容Spring Boot原生启动机制
 * <p>
 * 支持的命令行参数：
 * - <b>--slot.root</b>：指定根目录路径，默认为当前工作目录
 * - <b>--slot.path</b>：指定附加的类路径，可多次指定
 * <p>
 * 使用示例：
 * <pre>
 * # 指定根目录和补丁、插件目录
 * java -jar myapp.jar \
 *   --slot.root=/opt/myapp/ \
 *   --slot.path=patch/ \
 *   --slot.path=plugin/ \
 *   --spring.profiles.active=prod
 *
 * # 使用默认配置（当前目录下的patch和plugin目录）
 * java -jar myapp.jar --slot.path=patch/ --slot.path=plugin/
 * </pre>
 * <p>
 * 目录结构示例：
 * <pre>
 * /opt/myapp/
 * ├── myapp.jar              # 主程序 JAR
 * ├── patch/                 # 补丁目录（高优先级）
 * │   ├── bugfix-1.0.jar
 * │   └── hotfix-2.0.jar
 * ├── plugin/                # 插件目录
 * │   ├── redis-plugin.jar
 * │   └── kafka-plugin.jar
 * └── lib/                   # 原始依赖目录（低优先级）
 *     ├── spring-boot.jar
 *     └── other-deps.jar
 * </pre>
 * <p>
 * 类加载优先级（从高到低）：
 * 1. <b>patch目录</b>：用于热修复和紧急补丁
 * 2. <b>plugin目录</b>：用于功能扩展和插件加载
 * 3. <b>lib目录</b>：原始应用依赖库
 * <p>
 * 应用场景：
 * - 生产环境的热修复和补丁更新
 * - 插件化架构的微服务应用
 * - 多租户SaaS应用的定制化部署
 * - A/B测试和灰度发布场景
 * - 快速迭代和持续集成部署
 * <p>
 * 技术实现：
 * - 基于Spring Boot PropertiesLauncher扩展
 * - 自定义ClassLoader实现动态类加载
 * - 支持JAR和目录两种归档格式
 * - 统一的异常处理和日志输出
 * <p>
 * 相关文档：
 * - <a href="https://docs.spring.io/spring-boot/docs/2.2.1.RELEASE/reference/htmlsingle/#executable-jar">Spring Boot Executable JAR</a>
 * - <a href="https://blog.csdn.net/hengyunabc/article/details/50120001">Spring Boot类加载机制</a>
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
     * @since 1.0.0
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
