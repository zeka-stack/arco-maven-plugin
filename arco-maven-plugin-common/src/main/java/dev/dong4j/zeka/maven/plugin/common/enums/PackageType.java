package dev.dong4j.zeka.maven.plugin.common.enums;

/**
 * Maven项目包类型枚举，定义了Maven项目的打包输出格式
 * <p>
 * 该枚举类用于区分Maven项目的不同打包类型，为Maven插件提供明确的项目性质判断
 * 在Maven构建系统中，不同的打包类型对应不同的项目结构和部署策略
 * <p>
 * 支持的包类型包括：
 * <ul>
 *     <li>JAR - Java应用程序包，包含可执行代码和资源文件</li>
 *     <li>POM - 项目对象模型包，不包含实际代码，主要用于依赖管理</li>
 * </ul>
 * <p>
 * JAR类型项目为可执行的Java应用程序，包含编译后的class文件、资源文件等
 * POM类型项目通常作为父项目或聚合项目，主要用于统一管理依赖版本和构建配置
 * <p>
 * 在Maven插件的使用中，该枚举帮助插件精确判断项目的打包类型
 * 从而选择相应的构建策略、依赖处理方式和输出格式
 * <p>
 * 使用场景：
 * <ul>
 *     <li>Maven插件中的项目类型检测和判断</li>
 *     <li>构建流程中的条件分支处理</li>
 *     <li>依赖管理和传递性的差异化处理</li>
 *     <li>项目构建结果的格式化和输出控制</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.29 23:30
 * @since 1.0.0
 */
public enum PackageType {
    /** Jar package type */
    JAR,
    /** Pom package type */
    POM
}
