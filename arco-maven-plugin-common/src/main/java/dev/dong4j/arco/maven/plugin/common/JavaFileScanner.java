package dev.dong4j.arco.maven.plugin.common;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import lombok.SneakyThrows;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.13 19:17
 * @since 1.0.0
 */
@SuppressWarnings("all")
public interface JavaFileScanner {

    /**
     * Get java file list list
     *
     * @param project project
     * @return the list
     * @since 1.0.0
     */
    default List<File> getJavaFileList(@NotNull MavenProject project) {
        List<String> roots = project.getCompileSourceRoots();
        List<File> allFiles = new ArrayList<>();
        roots.forEach(r -> allFiles.addAll(this.scanFile(allFiles, r)));
        return allFiles;
    }

    /**
     * 获取所有 java 源文件
     *
     * @param fileList file list
     * @param filePath file path
     * @return the list
     * @since 1.0.0
     */
    default List<File> scanFile(List<File> fileList, String filePath) {
        File dir = new File(filePath);
        // 递归查找到所有的 java 源文件
        File[] listFiles = dir.listFiles();
        if (listFiles == null) {
            return Collections.emptyList();
        }

        for (File file : listFiles) {
            if (file.isDirectory()) {
                this.scanFile(fileList, file.getAbsolutePath());
            } else {
                if (file.getName().endsWith("java")) {
                    fileList.add(file);
                }
            }
        }
        return fileList;
    }

    /**
     * No java file boolean
     *
     * @param project project
     * @return the boolean
     * @since 1.0.0
     */
    default boolean noJavaFile(@NotNull MavenProject project) {
        List<File> files = this.getJavaFileList(project);
        return files.size() == 0;
    }

    /**
     * Is deploy model
     *
     * @param project project
     * @return the boolean
     * @since 1.5.0
     */
    default boolean isDeployModel(MavenProject project) {
        return this.mainClass(project) != null;
    }

    /**
     * Main class java file
     *
     * @param project project
     * @return the java file
     * @since 1.0.0
     */
    default JavaFile mainClass(MavenProject project) {
        // 源代码主目录
        List<File> allFiles = this.getJavaFileList(project);

        if (allFiles.size() > 0) {
            JavaFile mainJavaFile = this.parse(allFiles);
            if (mainJavaFile.isMainClass()) {
                return mainJavaFile;
            }
        }
        return null;
    }

    /**
     * Parse *
     *
     * @param allFiles all files
     * @return the java file
     * @since 1.0.0
     */
    @SneakyThrows
    default @NotNull JavaFile parse(@NotNull List<File> allFiles) {
        JavaFile javaFile = new JavaFile();

        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setDoNotAssignCommentsPrecedingEmptyLines(true)
            .setAttributeComments(false);

        JavaParser javaParser = new JavaParser(configuration);
        for (File file : allFiles) {
            javaFile.setFile(file);

            if (javaFile.isMainClass()) {
                break;
            }

            try {
                CompilationUnit parse = handleResult(javaParser.parse(file));
                parse.accept(new ClassOrInterfaceVisitor(), javaFile);
            } catch (Exception ignored) {
            }

        }

        return javaFile;
    }

    /**
     * Handle result t
     *
     * @param <T>    parameter
     * @param result result
     * @return the t
     * @since 1.0.0
     */
    @NotNull
    static <T extends Node> T handleResult(@NotNull ParseResult<T> result) {
        if (result.isSuccessful() && result.getResult().isPresent()) {
            return result.getResult().get();
        }
        throw new ParseProblemException(result.getProblems());
    }

    /**
     * <p>Description: </p>
     *
     * @author dong4j
     * @version 1.0.3
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.03.07 15:12
     * @since 1.0.0
     */
    class ClassOrInterfaceVisitor extends VoidVisitorAdapter<JavaFile> {

        /**
         * 解析 class 上是否有 @SpringBootApplication 或 @EnableAutoConfiguration
         *
         * @param declaration declaration
         * @param javaFile    java file
         * @since 1.0.0
         */
        @Override
        public void visit(@NotNull ClassOrInterfaceDeclaration declaration, JavaFile javaFile) {
            List<AnnotationExpr> annotationList = declaration.getAnnotations();
            for (AnnotationExpr annotation : annotationList) {
                if (JavaFile.SPRING_BOOT_APPLICATION.equals(annotation.getNameAsString())
                    || JavaFile.ENABLE_AUTOCONFIGURATION.equals(annotation.getNameAsString())) {

                    javaFile.setMainClass(true);
                    Optional<String> fullyQualifiedName = declaration.getFullyQualifiedName();
                    fullyQualifiedName.ifPresent(javaFile::setClassName);
                    break;
                }
            }

            super.visit(declaration, javaFile);
        }
    }

}
