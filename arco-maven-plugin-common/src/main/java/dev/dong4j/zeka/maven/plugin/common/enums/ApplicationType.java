package dev.dong4j.zeka.maven.plugin.common.enums;

/**
 * Maven插件应用类型枚举，定义了支持的Spring应用架构模式
 * <p>
 * 该枚举类用于标识和区分不同类型的Spring应用架构，为Maven插件提供精确的应用类型判断能力
 * 主要用于在构建过程中识别项目的技术栈和架构模式，以便采用相应的构建策略和配置
 * <p>
 * 支持的应用类型包括：
 * <ul>
 *     <li>NONE - 普通Java应用或未识别的应用类型</li>
 *     <li>BOOT - Spring Boot单体应用架构</li>
 *     <li>CLOUD - Spring Cloud微服务应用架构</li>
 * </ul>
 * <p>
 * 该枚举在Maven插件的应用类型检测、依赖分析、打包策略选择等场景中起到关键作用
 * 通过准确识别应用类型，插件可以自动选择最适合的构建配置和优化策略
 * <p>
 * 使用场景：
 * <ul>
 *     <li>Maven插件中的应用类型自动检测</li>
 *     <li>构建策略的条件判断和分支处理</li>
 *     <li>依赖管理和版本控制的差异化处理</li>
 *     <li>打包和部署配置的自动化选择</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.29 23:30
 * @since 1.0.0
 */
public enum ApplicationType {
    /** None application type */
    NONE,
    /** Boot application type */
    BOOT,
    /** Cloud application type */
    CLOUD,

}
