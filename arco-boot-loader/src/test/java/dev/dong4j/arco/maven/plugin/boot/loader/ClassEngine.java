package dev.dong4j.arco.maven.plugin.boot.loader;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gaofla on 2018/3/14.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 15:23
 * @since 1.5.0
 */
public class ClassEngine {

    /** Parent class loader */
    private URLClassLoader parentClassLoader;
    /** customClassCompiler */
    private static ClassEngine customClassCompiler = new ClassEngine();

    /**
     * Class engine
     *
     * @since 1.5.0
     */
    private ClassEngine() {
        this.parentClassLoader = (URLClassLoader) this.getClass().getClassLoader();
    }


    /**
     * Gets instance *
     *
     * @return the instance
     * @since 1.5.0
     */
    public static ClassEngine getInstance() {
        if (customClassCompiler == null) {
            try {
                synchronized (ClassEngine.class) {
                    if (customClassCompiler == null) {
                        customClassCompiler = new ClassEngine();
                    }
                }
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return customClassCompiler;
    }

    /**
     * 编译并加载类
     *
     * @param className       class name
     * @param javaCode        java code
     * @param javaCodeVersion java code version
     * @param newClassLoader  new class loader
     * @return class class
     * @throws Exception exception
     * @since 1.5.0
     */
    public Class<?> compileAndLoadClass(String className, String javaCode, String javaCodeVersion, boolean newClassLoader) throws Exception {
        // TODO newClassLoader 每次可以新new一个classloader来加载
        // 类名= 原来的名字+ "_版本号"
        String classNameSuffix = "_" + javaCodeVersion;

        return this.compileAndLoadClass(className, javaCode, classNameSuffix);
    }

    /**
     * 真正的编译和类加载实现
     *
     * @param className       class name
     * @param javaCode        java code
     * @param classNameSuffix class name suffix
     * @return class class
     * @throws Exception exception
     * @since 1.5.0
     */
    public Class<?> compileAndLoadClass(String className, String javaCode, String classNameSuffix) throws Exception {
        Class clz;

        String newClassName = className + classNameSuffix;
        // Step 1: 类名替换
        String newJavaCode = this.getNewJavaCode(javaCode, className, classNameSuffix);
        // Step 2: 编译代码
        ClassFileManager classFileManager = this.compile(newClassName, newJavaCode);
        // Step 3: 加载类
        clz = this.loadClass(classFileManager, newClassName,
            CustomerClassLoader.getDefaultSameCustomClassLoader(ClassEngine.getInstance().getParentClassLoader()));

        return clz;
    }

    /**
     * 加载类
     *
     * @param fileManager       file manager
     * @param className         class name
     * @param customClassLoader custom class loader
     * @return class class
     * @since 1.5.0
     */
    private Class<?> loadClass(ClassFileManager fileManager, String className, CustomerClassLoader customClassLoader) {
        JavaClassObject jco = fileManager.getMainJavaClassObject();
        Class clz = customClassLoader.loadClass(className, jco);

        return clz;
    }

    /**
     * 更换类名
     *
     * @param originJavaCode  origin java code
     * @param className       class name
     * @param classNameSuffix class name suffix
     * @return new java code
     * @since 1.5.0
     */
    private String getNewJavaCode(String originJavaCode, String className, String classNameSuffix) {
        Pattern pattern = Pattern.compile("([^A-Za-z0-9_])" + className + "(?![A-Za-z0-9_]+)");
        Matcher matcher = pattern.matcher(originJavaCode);
        boolean find = matcher.find();
        StringBuffer sb = new StringBuffer();
        while (find) {
            matcher.appendReplacement(sb, matcher.group() + classNameSuffix);
            find = matcher.find();
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 编译类名
     *
     * @param fullClassName full class name
     * @param javaCode      java code
     * @return class file manager
     * @throws IllegalAccessException illegal access exception
     * @throws InstantiationException instantiation exception
     * @since 1.5.0
     */
    private ClassFileManager compile(String fullClassName, String javaCode) throws IllegalAccessException, InstantiationException {

        // 获取系统编译器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        // 建立DiagnosticCollector对象
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

        // 建立用于保存被编译文件名的对象
        // 每个文件被保存在一个从JavaFileObject继承的类中
        ClassFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(diagnostics, null, null));

        List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
        jfiles.add(new CharSequenceJavaFileObject(fullClassName, javaCode));

        // 使用编译选项可以改变默认编译行为.编译选项是一个元素为String类型的Iterable集合
        List<String> options = new ArrayList<String>();
        options.add("-encoding");
        options.add("UTF-8");

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, jfiles);
        // 编译源程序不成功
        if (!task.call()) {
            System.out.println(compileError(diagnostics));
            return null;
        }

        return fileManager;
    }

    /**
     * 编译错误信息
     *
     * @param diagnostics diagnostics
     * @return string string
     * @since 1.5.0
     */
    private static String compileError(DiagnosticCollector<JavaFileObject> diagnostics) {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
            sb.append(compileError(diagnostic)).append("\n");
        }
        return sb.toString();
    }

    /**
     * 编译错误信息
     *
     * @param diagnostic diagnostic
     * @return string string
     * @since 1.5.0
     */
    private static String compileError(Diagnostic<?> diagnostic) {
        StringBuilder sb = new StringBuilder();
        sb.append("Code:[" + diagnostic.getCode() + "]\n");
        sb.append("Kind:[" + diagnostic.getKind() + "]\n");
        sb.append("Position:[" + diagnostic.getPosition() + "]\n");
        sb.append("Start Position:[" + diagnostic.getStartPosition() + "]\n");
        sb.append("End Position:[" + diagnostic.getEndPosition() + "]\n");
        sb.append("Source:[" + diagnostic.getSource() + "]\n");
        sb.append("Message:[" + diagnostic.getMessage(null) + "]\n");
        sb.append("LineNumber:[" + diagnostic.getLineNumber() + "]\n");
        sb.append("ColumnNumber:[" + diagnostic.getColumnNumber() + "]\n");
        return diagnostic.toString();
    }

    /**
     * Gets parent class loader *
     *
     * @return the parent class loader
     * @since 1.5.0
     */
    public URLClassLoader getParentClassLoader() {
        return this.parentClassLoader;
    }

    /**
     * Sets parent class loader *
     *
     * @param parentClassLoader parent class loader
     * @since 1.5.0
     */
    public void setParentClassLoader(URLClassLoader parentClassLoader) {
        this.parentClassLoader = parentClassLoader;
    }
}





