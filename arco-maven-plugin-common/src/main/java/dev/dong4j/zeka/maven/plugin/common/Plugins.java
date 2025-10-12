package dev.dong4j.zeka.maven.plugin.common;

import lombok.experimental.UtilityClass;

/**
 * Maven插件常量配置类，集中定义了所有Maven插件相关的配置常量和控制参数
 * <p>
 * 该类作为Maven插件框架的全局配置中心，统一管理各种插件的控制开关、配置参数和标识常量
 * 提供了统一的命名规范和参数约定，简化了插件开发和维护工作
 * 通过集中式管理避免了配置分散和重复定义的问题
 * <p>
 * 主要特性：
 * - 常量集中管理：所有插件相关常量的统一定义和管理
 * - 命名规范统一：遵循一致的命名约定和规范
 * - 类型安全：使用常量定义避免魔法字符串的问题
 * - 易于维护：集中修改和更新常量配置
 * - 工具类设计：使用Lombok @UtilityClass注解保证不可实例化
 * <p>
 * 常量分类和组织结构：
 * <p>
 * <b>插件控制开关常量</b>：
 * - TURN_OFF_PLUGIN ("true")：关闭插件的通用配置值
 * - TURN_ON_PLUGIN ("false")：启用插件的通用配置值
 * <p>
 * <b>项目类型和模块标识</b>：
 * - MODULE_TYPE：模块类型标识属性名
 * - SCOPE_COMPILE：编译作用域标识
 * - START_CLASS_SUFFIX：启动类后缀标识
 * <p>
 * <b>依赖检测和应用类型识别</b>：
 * - CLOUD_DEPENDENCY_FALG：Spring Cloud依赖检测标识
 * - BOOT_DEPENDENCY_FALG：Spring Boot依赖检测标识
 * - NCAOS_CONFIG_DEPENDENCY_FALG：Nacos配置中心依赖检测标识
 * <p>
 * <b>代码质量检查插件控制</b>：
 * - SKIP_CHECKSTYLE：控制CheckStyle插件的启用和禁用
 * - SKIP_PMD：控制PMD插件的启用和禁用
 * <p>
 * <b>构建和打包插件控制</b>：
 * - SKIP_ASSEMBLY：控制Assembly打包插件
 * - SKIP_ASSEMBLY_CONFIG：控制Assembly配置文件生成
 * - SKIP_JAR_REPACKAGE：控制JAR重打包功能
 * - SKIP_MAKESELF：控制自解压包生成
 * <p>
 * <b>项目信息和属性生成</b>：
 * - SKIP_BUILD_INFO：控制构建信息文件生成
 * - SKIP_BUILD_ACTIVE_FILE：控制Profile活跃文件生成
 * - SKIP_BUILD_MAINCLASS_PROPERTY：控制主类属性设置
 * - SKIP_COMPILED_ID：控制编译标识生成
 * <p>
 * <b>部署和发布插件控制</b>：
 * - SKIP_LAUNCH_SCRIPT：控制启动脚本生成
 * - SKIP_DOCKERFILE_SCRIPT：控制Dockerfile生成
 * - SKIP_PUBLISH_SINGLE：控制单个项目发布
 * - SKIP_PUBLISH_BATCH：控制批量项目发布
 * <p>
 * <b>工具和辅助插件控制</b>：
 * - SKIP_GITCOMMITID：控制Git提交信息插件
 * - SKIP_DELETE_TEMP_FILE：控制临时文件清理功能
 * <p>
 * 使用模式和最佳实践：
 * <p>
 * <b>在Maven插件中使用</b>：
 * <pre>
 * // 在Mojo中使用插件控制开关
 * @Parameter(property = Plugins.SKIP_BUILD_INFO, defaultValue = Plugins.TURN_OFF_PLUGIN)
 * private boolean skip;
 *
 * // 在依赖检测中使用标识常量
 * boolean isCloudApp = artifacts.stream()
 *     .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.CLOUD_DEPENDENCY_FALG));
 * </pre>
 * <p>
 * <b>在配置文件中使用</b>：
 * <pre>
 * // 在pom.xml中配置插件开关
 * &lt;properties&gt;
 *     &lt;checkstyle.skip&gt;true&lt;/checkstyle.skip&gt;
 *     &lt;build.info.skip&gt;false&lt;/build.info.skip&gt;
 * &lt;/properties&gt;
 *
 * // 命令行参数方式
 * mvn clean package -Dcheckstyle.skip=true
 * </pre>
 * <p>
 * 设计原则和约定：
 * - 所有SKIP_*开头的常量用于控制插件的启用和禁用
 * - 使用"true"/"false"字符串而不是布尔值，方便命令行参数传递
 * - _FALG后缀的常量用于依赖检测和类型识别
 * - _SUFFIX后缀的常量用于名称拼接和标识构造
 * - 使用final和static保证常量的不可变性
 * <p>
 * 扩展和维护建议：
 * - 新增插件控制常量时应遵循现有命名规范
 * - 定期清理不再使用的常量定义
 * - 注意保持向后兼容性，谨慎修改已有常量值
 * - 建议为新的功能模块增加相应的常量分组
 * - 考虑使用枚举类型更好地组织和管理复杂常量
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.12 16:08
 * @since 1.0.0
 */
@UtilityClass
public final class Plugins {
    /** MODULE_TYPE */
    public static final String MODULE_TYPE = "arco-maven-plugin:module.type";
    /** 关闭插件 skip = true */
    public static final String TURN_OFF_PLUGIN = "true";
    /** 使用插件 skip = false */
    public static final String TURN_ON_PLUGIN = "false";
    /** SCOPE_COMPILE */
    public static final String SCOPE_COMPILE = "compile";
    /** CLOUD_DEPENDENCY_FALG */
    public static final String CLOUD_DEPENDENCY_FALG = "spring-cloud-context";
    /** BOOT_DEPENDENCY_FALG */
    public static final String BOOT_DEPENDENCY_FALG = "spring-boot";
    /** NCAOS_CONFIG_DEPENDENCY_FALG */
    public static final String NCAOS_CONFIG_DEPENDENCY_FALG = "spring-cloud-starter-alibaba-nacos-config";
    /** SKIP_CHECKSTYLE */
    public static final String SKIP_CHECKSTYLE = "checkstyle.skip";
    /** SKIP_PMD */
    public static final String SKIP_PMD = "pmd.skip";
    /** SKIP_ASSEMBLY */
    public static final String SKIP_ASSEMBLY = "assembly.skipAssembly";
    /** SKIP_GITCOMMITID */
    public static final String SKIP_GITCOMMITID = "maven.gitcommitid.skip";
    /** SKIP_BUILD_INFO */
    public static final String SKIP_BUILD_INFO = "build.info.skip";
    /** SKIP_BUILD_MAINCLASS_PROPERTY */
    public static final String SKIP_BUILD_MAINCLASS_PROPERTY = "build.mainclass.property.skip";
    /** SKIP_ASSEMBLY_CONFIG */
    public static final String SKIP_ASSEMBLY_CONFIG = "assembly.config.skip";
    /** SKIP_LAUNCH_SCRIPT */
    public static final String SKIP_LAUNCH_SCRIPT = "launch.script.skip";
    /** SKIP_DOCKERFILE_SCRIPT */
    public static final String SKIP_DOCKERFILE_SCRIPT = "dockerfile.skip";
    /** SKIP_COMPILED_ID */
    public static final String SKIP_COMPILED_ID = "compiled.id.skip";
    /** SKIP_JAR_REPACKAGE */
    public static final String SKIP_JAR_REPACKAGE = "jar.repackage.skip";
    /** SKIP_MAKESELF */
    public static final String SKIP_MAKESELF = "makeself.skip";
    /** SKIP_DELETE_TEMP_FILE */
    public static final String SKIP_DELETE_TEMP_FILE = "delete.temp.file.skip";
    /** SKIP_PUBLISH_SINGLE */
    public static final String SKIP_PUBLISH_SINGLE = "publish-single.skip";
    /** SKIP_PUBLISH_BATCH */
    public static final String SKIP_PUBLISH_BATCH = "publish-batch.skip";
    /** 启动类后缀 */
    public static final String START_CLASS_SUFFIX = "_START_CLASS";

}
