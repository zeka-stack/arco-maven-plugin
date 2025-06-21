package dev.dong4j.arco.maven.plugin.boot.loader;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Created by gaofla on 2018/3/14.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
public class CustomerClassLoader extends URLClassLoader {
    /** customClassLoader */
    private static CustomerClassLoader customClassLoader;

    /**
     * Customer class loader
     *
     * @param parent parent
     * @since 1.5.0
     */
    private CustomerClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    /**
     * Find class by class name
     *
     * @param className class name
     * @return the class
     * @throws ClassNotFoundException class not found exception
     * @since 1.5.0
     */
    public Class findClassByClassName(String className) throws ClassNotFoundException {
        return this.findClass(className);
    }

    /**
     * 加载类
     *
     * @param fullName full name
     * @param jco      jco
     * @return class class
     * @since 1.5.0
     */
    public Class loadClass(String fullName, JavaClassObject jco) {
        byte[] classData = jco.getBytes();
        return this.defineClass(fullName, classData, 0, classData.length);
    }

    /**
     * 获取相同的类加载器实例
     *
     * @param parent parent
     * @return default same custom class loader
     * @since 1.5.0
     */
    public static CustomerClassLoader getDefaultSameCustomClassLoader(ClassLoader parent) {
        if (customClassLoader == null) {
            try {
                synchronized (CustomerClassLoader.class) {
                    if (customClassLoader == null) {
                        customClassLoader = new CustomerClassLoader(parent);
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return customClassLoader;
    }
}
