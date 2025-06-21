package dev.dong4j.zeka.maven.plugin.boot.loader;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
public class ClassLoadStudy {
    /**
     * Main
     *
     * @param args args
     * @throws Exception exception
     * @since 1.5.0
     */
    public static void main(String[] args) throws Exception {
        HotDeploy hot = new HotDeploy("Dynamic.Task");
        hot.monitor();
        while (true) {
            TimeUnit.SECONDS.sleep(2);
            hot.getTask().run();
        }
    }
}

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
class HotDeploy {
    /** instance */
    private static volatile Runnable instance;
    /** File name */
    private final String FILE_NAME;
    /** Class name */
    private final String CLASS_NAME;

    /**
     * Hot deploy
     *
     * @param name name
     * @since 1.5.0
     */
    public HotDeploy(String name) {
        this.CLASS_NAME = name; // 类的完全限定名
        name = name.replaceAll("\\.", "/") + ".class";
        this.FILE_NAME = (this.getClass().getResource("/") + name).substring(6); // 判断class文件修改时间使用, substring(6)去掉开头的file:/
    }

    /**
     * 获取一个任务
     *
     * @return the task
     * @since 1.5.0
     */
    public Runnable getTask() {
        if (instance == null) { // 双重检查锁, 单例, 线程安全
            synchronized (HotDeploy.class) {
                if (instance == null) {
                    try {
                        instance = this.createTask();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instance;
    }

    /**
     * 创建一个任务, 重新加载 class 文件
     *
     * @return the runnable
     * @since 1.5.0
     */
    private Runnable createTask() {
        try {
            Class clazz = HotDeployClassLoader.getLoader().loadClass(this.CLASS_NAME);
            if (clazz != null) {
                return (Runnable) clazz.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 监视器, 监视class文件是否被修改过, 如果是的话, 则重新加载
     *
     * @throws IOException io exception
     * @since 1.5.0
     */
    public void monitor() throws IOException {
        Thread t = new Thread(() -> {
            try {
                long lastModified = Files.getLastModifiedTime(Paths.get(this.FILE_NAME)).toMillis();
                while (true) {
                    TimeUnit.MILLISECONDS.sleep(500);
                    long now = Files.getLastModifiedTime(Paths.get(this.FILE_NAME)).toMillis();
                    // 如果class文件被修改过了
                    if (now != lastModified) {
                        lastModified = now;
                        // 重新加载
                        instance = this.createTask();
                    }
                }
            } catch (InterruptedException | IOException e) {
                e.printStackTrace();
            }
        });
        // 守护线程
        t.setDaemon(true);
        t.start();
    }
}

/**
 * <p>Description: 自定义的类加载器</p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
class HotDeployClassLoader extends ClassLoader {
    /**
     * Find class
     *
     * @param name name
     * @return the class
     * @throws ClassNotFoundException class not found exception
     * @since 1.5.0
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            String fileName = "/" + name.replaceAll("\\.", "/") + ".class";
            InputStream is = this.getClass().getResourceAsStream(fileName);
            if (is == null) {
                throw new ClassNotFoundException(name);
            }
            byte[] b = IOUtils.toByteArray(is);
            return this.defineClass(name, b, 0, b.length);
        } catch (IOException e) {
            throw new ClassNotFoundException(name);
        }
    }

    /**
     * Gets loader *
     *
     * @return the loader
     * @since 1.5.0
     */
    public static HotDeployClassLoader getLoader() {
        return new HotDeployClassLoader();
    }
}
