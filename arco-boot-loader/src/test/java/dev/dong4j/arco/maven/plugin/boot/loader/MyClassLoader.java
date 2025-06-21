package dev.dong4j.arco.maven.plugin.boot.loader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
@SuppressWarnings("all")
public class MyClassLoader extends ClassLoader {

    /** Root path */
    private final String rootPath;
    /** Clazz */
    // 记录需要让当前类加载器加载的类
    private final List<String> clazz;

    /**
     * My class loader
     *
     * @param rootPath   root path
     * @param clazzPaths 某个路径下
     * @throws IOException io exception
     * @since 1.5.0
     */
    public MyClassLoader(String rootPath, String... clazzPaths) throws IOException {

        this.rootPath = rootPath;
        this.clazz = new ArrayList<>();
        for (String clazzPath : clazzPaths) {
            this.loadClassPath(new File(clazzPath));
        }

    }

    /**
     * Load class path
     *
     * @param file file
     * @throws IOException io exception
     * @since 1.5.0
     */
    private void loadClassPath(File file) throws IOException {
        if (file.isDirectory()) {
            for (File file1 : file.listFiles()) {
                this.loadClassPath(file1);
            }
        } else {

            String fileName = file.getName();
            String filePath = file.getPath();
            String endName = fileName.substring(fileName.lastIndexOf(".") + 1);
            if ("class".equals(endName)) {
                InputStream inputStream = new FileInputStream(file);
                byte[] data = new byte[(int) file.length()];
                inputStream.read(data);

                String className = this.filePathToClassName(filePath);
                this.clazz.add(className);
                this.defineClass(className, data, 0, data.length);

            }
        }
    }

    /**
     * File path to class name
     *
     * @param filePath file path
     * @return the string
     * @since 1.5.0
     */
    private String filePathToClassName(String filePath) {
        String className = filePath.replace(this.rootPath, "").replaceAll("\\\\", ".");
        className = className.substring(0, className.lastIndexOf("."));
        className = className.substring(1);
        return className;

    }

    /**
     * Load class
     *
     * @param name name
     * @return the class
     * @throws ClassNotFoundException class not found exception
     * @since 1.5.0
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {

        Class<?> c = this.findLoadedClass(name);
        if (c == null) {
            // 不需要我们加载
            if (!this.clazz.contains(name)) {
                c = getSystemClassLoader().loadClass(name);
            } else {
                throw new ClassNotFoundException("找不到该class");
            }
        }

        return c;
    }

    /**
     * Main
     *
     * @param args args
     * @throws Exception exception
     * @since 1.5.0
     */
    public static void main(String[] args) throws Exception {
        while (true) {
            String rootPath = MyClassLoader.class.getResource("/").getPath().replaceAll("%20", " ");
            rootPath = new File(rootPath).getPath();

            MyClassLoader myClassLoader = new MyClassLoader(rootPath, rootPath + "\\dev\\dong4j");
            Class<?> aClass = myClassLoader.loadClass("dev.dong4j.arco.maven.plugin.boot.loader.TestClass");
            System.out.println(aClass.getClassLoader());
            Object o = aClass.newInstance();
            aClass.getMethod("hello").invoke(o);

            Thread.sleep(2000);
        }

    }
}
