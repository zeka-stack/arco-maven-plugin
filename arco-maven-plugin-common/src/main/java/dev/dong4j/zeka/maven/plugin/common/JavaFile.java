package dev.dong4j.zeka.maven.plugin.common;

import java.io.File;
import lombok.Data;

/**
 * Java文件元数据封装类，用于存储和管理Java源文件的基本信息和特征
 * <p>
 * 该类主Maven插件系统中的数据载体，用于封装Java源文件的关键信息
 * 特别针对Spring Boot应用的主类检测和识别场景设计
 * 提供了统一的数据结构和访问接口，简化了文件管理和操作流程
 * <p>
 * 主要特性：
 * - 数据封装：封装Java文件的核心信息和特征属性
 * - 类型检测：支持主类标识和启动类检测
 * - 文件关联：与实际的Java源文件对象关联
 * - 元数据管理：统一管理文件名称、类名等元数据
 * - 标准化：定义了Spring Boot相关的标准常量和约定
 * - 易用性：通过Lombok注解提供标准的getter/setter方法
 * <p>
 * 核心属性和功能：
 * <p>
 * <b>mainClass</b> - 主类标识：
 * - 标识当前Java文件是否为Spring Boot启动主类
 * - 通过注解检测自动设置（@SpringBootApplication或@EnableAutoConfiguration）
 * - 用于区分普通业务类和应用启动入口类
 * <p>
 * <b>file</b> - 文件对象关联：
 * - 保持与实际Java源文件的强关联
 * - 支持文件路径、大小、修改时间等基本信息访问
 * - 便于后续的文件操作和内容读取
 * <p>
 * <b>className</b> - 全限定类名：
 * - 存储Java类的全限定名（包括包名）
 * - 用于类加载、反射操作和配置生成
 * - 支持动态类名解析和注入
 * <p>
 * Spring Boot注解常量定义：
 * <p>
 * <b>SPRING_BOOT_APPLICATION</b>：
 * - 对应@SpringBootApplication注解
 * - Spring Boot应用的标准启动注解
 * - 集成了@Configuration、@EnableAutoConfiguration和@ComponentScan
 * - 是最常用的Spring Boot应用入口标识
 * <p>
 * <b>ENABLE_AUTOCONFIGURATION</b>：
 * - 对应@EnableAutoConfiguration注解
 * - 允许Spring Boot自动配置机制
 * - 可以与其他注解组合使用
 * - 适用于更细粒度的配置控制场景
 * <p>
 * 使用场景和应用：
 * - Maven插件中的主类检测和识别
 * - Spring Boot应用的自动化构建和部署
 * - 项目结构分析和依赖管理
 * - 代码生成工具中的模板处理
 * - IDE插件中的项目类型识别
 * - 持续集成中的应用类型判断
 * <p>
 * 实际使用示例：
 * <pre>
 * // 创建实例并设置基本信息
 * JavaFile javaFile = new JavaFile();
 * javaFile.setFile(new File("src/main/java/com/example/Application.java"));
 * javaFile.setMainClass(true);
 * javaFile.setClassName("com.example.Application");
 *
 * // 在解析器中使用
 * if (javaFile.isMainClass()) {
 *     String startClass = javaFile.getClassName();
 *     // 生成启动配置或执行主类相关逻辑
 * }
 *
 * // 在文件扫描中使用
 * List<JavaFile> javaFiles = scanner.scanAllJavaFiles();
 * JavaFile mainClass = javaFiles.stream()
 *     .filter(JavaFile::isMainClass)
 *     .findFirst()
 *     .orElse(null);
 * </pre>
 * <p>
 * 注意事项和最佳实践：
 * - 使用Lombok的@Data注解，自动生成getter/setter方法
 * - mainClass属性在文件解析过程中自动设置
 * - className应该使用全限定名，包含完整的包路径
 * - file对象应该指向存在的真实文件
 * - 在多线程环境中使用时注意线程安全性
 * - 适合作为临时数据对象使用，不建议长期持有
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 15:12
 * @since 1.0.0
 */
@Data
public class JavaFile {
    /** Main class */
    private boolean mainClass;
    /** File */
    private File file;
    /** Class name */
    private String className;
    /** SPRING_BOOT_APPLICATION */
    public static final String SPRING_BOOT_APPLICATION = "SpringBootApplication";
    /** ENABLE_AUTOCONFIGURATION */
    public static final String ENABLE_AUTOCONFIGURATION = "EnableAutoConfiguration";
}
