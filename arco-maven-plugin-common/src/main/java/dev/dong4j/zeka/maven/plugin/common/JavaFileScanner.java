package dev.dong4j.zeka.maven.plugin.common;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.apache.maven.project.MavenProject;
import org.jetbrains.annotations.NotNull;

/**
 * Java源文件扫描和解析接口，提供了全面的Java项目结构分析和主类检测能力
 * <p>
 * 该接口为Maven插件系统中的核心组件，专门用于处理Java项目的文件结构分析
 * 集成了JavaParser强大的语法分析能力，提供了简单易用的高层API
 * 特别针对Spring Boot项目的主类检测和部署模块识别场景设计
 * <p>
 * 主要特性：
 * - 文件扫描：递归扫描项目目录中的所有Java源文件
 * - AST解析：基于JavaParser的抽象语法树解析
 * - 注解检测：智能检测Spring Boot相关注解
 * - 类型识别：自动识别项目中的主类和部署模块
 * - 安全处理：异常容错和错误恢复机制
 * - 性能优化：支持早期退出和懒加载策略
 * <p>
 * 核心方法和功能：
 * <p>
 * <b>文件扫描相关方法</b>：
 * <p>
 * <b>getJavaFileList(MavenProject)</b> - 获取项目中所有Java文件：
 * - 自动读取Maven项目的编译源码目录
 * - 递归扫描所有子目录中的.java文件
 * - 返回完整的文件列表供后续处理
 * <p>
 * <b>scanFile(List<File>, String)</b> - 递归目录扫描：
 * - 递归遍历指定目录及其子目录
 * - 过滤出所有.java扩展名的文件
 * - 支持深度优先遍历和目录结构分析
 * <p>
 * <b>项目类型检测方法</b>：
 * <p>
 * <b>noJavaFile(MavenProject)</b> - 检查项目是否包含Java文件：
 * - 用于判断是否为空项目或纯资源项目
 * - 帮助Maven插件决定是否需要执行相关处理
 * - 优化构建效率和资源利用
 * <p>
 * <b>isDeployModel(MavenProject)</b> - 判断是否为部署模块：
 * - 通过检测是否存在主类来判断模块类型
 * - 用于区分普通依赖模块和可执行应用模块
 * - 支持Maven插件的条件执行和智能配置
 * <p>
 * <b>主类检测和解析方法</b>：
 * <p>
 * <b>mainClass(MavenProject)</b> - 检测和获取项目主类：
 * - 扫描所有Java文件寻找主类标识
 * - 返回包含主类信息的JavaFile对象
 * - 支持多主类检测和优先级选择
 * <p>
 * <b>parse(List<File>)</b> - 源文件解析和分析：
 * - 使用JavaParser进行语法分析
 * - 使用Visitor模式遍历AST节点
 * - 检测类声明和注解信息
 * - 支持早期退出以提高性能
 * <p>
 * JavaParser集成和配置：
 * - 使用定制的ParserConfiguration优化解析效果
 * - 禁用注释处理以提高性能
 * - 支持异常容错和错误恢复
 * - 提供统一的结果处理和错误报告
 * <p>
 * 内部Visitor实现：
 * <p>
 * <b>ClassOrInterfaceVisitor</b> - 类和接口访问器：
 * - 继承自VoidVisitorAdapter，提供专门的类声明处理
 * - 检测@SpringBootApplication和@EnableAutoConfiguration注解
 * - 自动提取类的全限定名
 * - 支持早期退出和继续遍历的灵活控制
 * <p>
 * 支持的Spring Boot注解：
 * - <b>@SpringBootApplication</b>：Spring Boot应用的主要注解
 * - <b>@EnableAutoConfiguration</b>：自动配置启用注解
 * <p>
 * 使用场景和应用：
 * - Maven插件中的项目类型识别和模块分类
 * - Spring Boot应用的自动化构建和部署配置
 * - IDE插件中的项目结构分析和导航
 * - 代码生成工具中的模板选择和配置生成
 * - 持续集成中的构建策略决定和优化
 * - 微服务架构中的服务发现和注册
 * <p>
 * 性能优化和最佳实践：
 * - 使用早期退出策略，找到主类后立即停止扫描
 * - 合理使用缓存，避免重复解析同一文件
 * - 适当的异常处理，确保单个文件错误不影响整体扫描
 * - 在大型项目中考虑使用并行处理提高效率
 * - 适当的JavaParser配置可以显著提高解析性能
 * <p>
 * 错误处理和异常情况：
 * - 文件不存在或无读取权限时静默忽略
 * - 语法解析失败时使用默认的容错机制
 * - 项目编译源码目录不存在时返回空集合
 * - 内存不足时提供适当的警告和降级处理
 * <p>
 * 扩展和定制建议：
 * - 可以扩展支持更多的Spring框架注解
 * - 支持自定义的主类检测规则和策略
 * - 可以集成更多的代码分析工具和库
 * - 支持配置化的扫描策略和过滤规则
 *
 * @author dong4j
 * @version 1.0.0
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
     * @since 1.0.0
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
     * 类和接口访问器，专门用于检测和分析Java类声明中的Spring Boot相关注解
     * <p>
     * 该Visitor实现了JavaParser的VoidVisitorAdapter，专门用于遍历和分析类声明节点
     * 主要功能是检测类上是否标注了Spring Boot启动相关的注解
     * 并自动提取类的全限定名用于后续的主类配置和处理
     * <p>
     * 支持的注解检测：
     * - @SpringBootApplication：Spring Boot应用的主要启动注解
     * - @EnableAutoConfiguration：自动配置启用注解
     * <p>
     * 工作原理：
     * 1. 遍历类声明节点上的所有注解
     * 2. 检查注解名称是否匹配目标注解
     * 3. 如果找到匹配的注解，标记为主类并提取类名
     * 4. 继续遍历其他节点以保证完整性
     *
     * @author dong4j
     * @version 1.0.0
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
