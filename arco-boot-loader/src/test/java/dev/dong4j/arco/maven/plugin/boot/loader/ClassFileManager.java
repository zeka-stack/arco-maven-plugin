package dev.dong4j.arco.maven.plugin.boot.loader;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gaofla on 2018/3/14.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
public class ClassFileManager extends ForwardingJavaFileManager {

    /**
     * 编译存储的类包括子类
     */
    private final List<JavaClassObject> javaClassObjectList;

    /**
     * Class file manager
     *
     * @param standardManager standard manager
     * @since 1.5.0
     */
    public ClassFileManager(StandardJavaFileManager
                                standardManager) {
        super(standardManager);
        this.javaClassObjectList = new ArrayList<JavaClassObject>();
    }

    /**
     * Gets main java class object *
     *
     * @return the main java class object
     * @since 1.5.0
     */
    public JavaClassObject getMainJavaClassObject() {
        if (this.javaClassObjectList != null && this.javaClassObjectList.size() > 0) {
            int size = this.javaClassObjectList.size();
            return this.javaClassObjectList.get((size - 1));
        }
        return null;
    }

    /**
     * Gets inner class java class object *
     *
     * @return the inner class java class object
     * @since 1.5.0
     */
    public List<JavaClassObject> getInnerClassJavaClassObject() {
        if (this.javaClassObjectList != null && this.javaClassObjectList.size() > 0) {
            int size = this.javaClassObjectList.size();
            if (size == 1) {
                return null;
            }
            return this.javaClassObjectList.subList(0, size - 1);
        }
        return null;
    }

    /**
     * Gets java file for output *
     *
     * @param location  location
     * @param className class name
     * @param kind      kind
     * @param sibling   sibling
     * @return the java file for output
     * @throws IOException io exception
     * @since 1.5.0
     */
    @Override
    public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location,
                                               String className, JavaFileObject.Kind kind, FileObject sibling)
        throws IOException {
        JavaClassObject jclassObject = new JavaClassObject(className, kind);
        this.javaClassObjectList.add(jclassObject);
        return jclassObject;
    }
}
