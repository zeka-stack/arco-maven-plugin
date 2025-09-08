package dev.dong4j.zeka.maven.plugin.helper.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * CheckStyle和PMD代码质量检查插件枚举定义
 * <p>
 * 该枚举定义了Maven构建过程中用于代码质量检查的插件类型
 * 主要包括PMD静态代码分析插件和CheckStyle代码风格检查插件
 * 用于统一管理和配置代码质量检查工具
 * <p>
 * 主要特性：
 * - 插件标识管理：为每个代码质量检查插件提供统一的标识符
 * - 批量操作支持：提供获取所有插件标识的便捷方法
 * - 类型安全：通过枚举确保插件类型的正确性
 * - 易于扩展：可以方便地添加新的代码质量检查插件
 * <p>
 * 支持的插件类型：
 * - PMD：静态代码分析工具，用于检测潜在的代码问题
 * - CheckStyle：代码风格检查工具，用于确保代码符合编码规范
 * <p>
 * 使用场景：
 * - Maven构建过程中的插件禁用和启用控制
 * - 代码质量检查插件的统一配置管理
 * - 构建脚本中的插件类型识别和处理
 * - 开发工具集成中的插件管理
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取所有代码质量检查插件的标识
 * List<String> pluginKeys = CheckStylePlugin.keys();
 *
 * // 获取特定插件的标识
 * String pmdPlugin = CheckStylePlugin.PMD.getKey();
 * String checkStylePlugin = CheckStylePlugin.CHECKSTYLE.getKey();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.11 15:47
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CheckStylePlugin {
    /** PMD静态代码分析插件，用于检测潜在的代码问题和不良编程习惯 */
    PMD("org.apache.maven.plugins:maven-pmd-plugin"),
    /** CheckStyle代码风格检查插件，用于确保代码符合编码规范 */
    CHECKSTYLE("org.apache.maven.plugins:maven-checkstyle-plugin");

    /** 插件标识符，格式为 groupId:artifactId */
    private final String key;

    /**
     * 获取所有代码质量检查插件的标识符列表
     * <p>
     * 该方法遍历所有的枚举值，提取它们的插件标识符
     * 常用于批量禁用或启用代码质量检查插件
     * <p>
     * 使用示例：
     * <pre>{@code
     * List<String> allPlugins = CheckStylePlugin.keys();
     * // 返回: ["org.apache.maven.plugins:maven-pmd-plugin",
     * //        "org.apache.maven.plugins:maven-checkstyle-plugin"]
     * }</pre>
     *
     * @return 包含所有代码质量检查插件标识符的列表
     * @since 1.0.0
     */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>();
        Arrays.stream(CheckStylePlugin.values()).forEach(p -> keys.add(p.getKey()));
        return keys;
    }
}
