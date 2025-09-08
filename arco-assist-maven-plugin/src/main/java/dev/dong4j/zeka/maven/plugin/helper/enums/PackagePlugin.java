package dev.dong4j.zeka.maven.plugin.helper.enums;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Maven打包插件枚举定义，包含各种打包和依赖管理插件
 * <p>
 * 该枚举定义了Maven构建过程中用于项目打包、依赖管理和版本控制的关键插件
 * 包括了从基本的JAR打包到复杂的Assembly打包、依赖管理和Git集成等功能
 * <p>
 * 主要特性：
 * - 多种打包方式：支持JAR、Assembly等不同的打包类型
 * - 依赖管理：提供依赖拷贝和管理功能
 * - 版本信息：集成Git提交信息的生成功能
 * - 统一管理：为所有打包相关插件提供统一的标识和管理
 * <p>
 * 支持的插件类型：
 * - Assembly：用于创建包含依赖的分发包（ZIP、TAR等）
 * - Dependency：用于依赖的复制、解包和分析
 * - JAR：用于标准JAR文件的打包和配置
 * - Git Commit：用于生成Git提交信息文件
 * <p>
 * 使用场景：
 * - 应用打包和发布流程中的插件管理
 * - CI/CD流水线中的构建配置
 * - 多模块项目的统一打包策略
 * - 企业级应用的部署包生成
 * <p>
 * 使用示例：
 * <pre>{@code
 * // 获取所有打包插件的标识
 * List<String> packagePlugins = PackagePlugin.keys();
 * 
 * // 获取特定插件的标识
 * String assemblyPlugin = PackagePlugin.ASSEMBLY.getKey();
 * String jarPlugin = PackagePlugin.JAR.getKey();
 * }</pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.11 14:13
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PackagePlugin {
    /** Assembly打包插件，用于创建包含依赖的分发包（ZIP、TAR等格式） */
    ASSEMBLY("org.apache.maven.plugins:maven-assembly-plugin"),
    /** 依赖管理插件，用于依赖的复制、解包和分析操作 */
    DEPENDENCY("org.apache.maven.plugins:maven-dependency-plugin"),
    /** JAR打包插件，用于标准JAR文件的打包和配置 */
    JAR("org.apache.maven.plugins:maven-jar-plugin"),
    /** Git提交信息插件，用于生成Git提交记录和版本信息文件 */
    GIT_COMMIT("pl.project13.maven:git-commit-id-plugin");

    /** 插件标识符，格式为 groupId:artifactId */
    private final String key;

    /**
     * 获取所有打包插件的标识符列表
     * <p>
     * 该方法遍历所有的枚举值，提取它们的插件标识符
     * 常用于批量配置或管理打包相关的Maven插件
     * <p>
     * 使用示例：
     * <pre>{@code
     * List<String> allPackagePlugins = PackagePlugin.keys();
     * // 返回: ["org.apache.maven.plugins:maven-assembly-plugin", 
     * //        "org.apache.maven.plugins:maven-dependency-plugin",
     * //        "org.apache.maven.plugins:maven-jar-plugin",
     * //        "pl.project13.maven:git-commit-id-plugin"]
     * }</pre>
     *
     * @return 包含所有打包插件标识符的列表
     * @since 1.0.0
     */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>();
        Arrays.stream(PackagePlugin.values()).forEach(p -> keys.add(p.getKey()));
        return keys;
    }
}
