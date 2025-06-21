package dev.dong4j.zeka.maven.plugin.boot.loader;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 启动程序用来调用主方法的实用程序类, 包含 main() 的类使用线程上下文类加载器加载
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 01:00
 * @since 1.3.0
 */
@SuppressWarnings("all")
public class MainMethodRunner {
    /** START_CLASS_ARGS */
    public static final String START_CLASS_ARGS = "--start.class=";
    /** MAIN_METHOD_NAME */
    public static final String MAIN_METHOD_NAME = "main";
    /** Class loader */
    private final ClassLoader classLoader;
    /** Main class name */
    private final String mainClassName;
    /** Args */
    private final String[] args;

    /**
     * Create a new {@link MainMethodRunner} instance.
     *
     * @param classLoader class loader
     * @param mainClass   the main class
     * @param args        incoming arguments
     * @since 1.0.0
     */
    public MainMethodRunner(ClassLoader classLoader, String mainClass, String[] args) {
        this.classLoader = classLoader;
        this.mainClassName = mainClass;
        this.args = (args != null) ? args.clone() : null;
    }

    /**
     * 反射调用 mainClass 对应的 main(), 如果不存在, 则需要向上查找父类的 main(), 且需要传入 mainClassName, 由父类处理.
     *
     * @throws Exception exception
     * @since 1.0.0
     */
    public void run() throws Exception {
        // 加载应用程序主入口类
        Class<?> mainClass = this.classLoader.loadClass(this.mainClassName);

        Method method;
        for (method = null; mainClass != Object.class; mainClass = mainClass.getSuperclass()) {
            try {
                method = mainClass.getDeclaredMethod(MAIN_METHOD_NAME, String[].class);
                break;
            } catch (Exception ignored) {
            }
        }

        if (method != null) {
            Set<String> argsSet = new HashSet<>(this.args.length == 0 ? 1 : this.args.length + 1);
            argsSet.add(START_CLASS_ARGS + this.mainClassName);
            argsSet.addAll(Arrays.asList(this.args));
            method.invoke(null, new Object[]{argsSet.toArray(new String[0])});
        }
    }

}
