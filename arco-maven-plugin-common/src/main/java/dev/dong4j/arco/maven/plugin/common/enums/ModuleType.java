package dev.dong4j.arco.maven.plugin.common.enums;

/**
 * <p>Description: 模块类型 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.07.05 14:39
 * @since 1.5.0
 */
public enum ModuleType {
    /** maven 管理模块, packaging 为 pom */
    POM,
    /** packaging 为 jar, 但是无 java 源码 */
    EMPTY,
    /** 普通的 jar 模块, 作为其他模块的依赖 */
    DEPEND,
    /** 能够被部署的模块, 存在启动类 */
    DELOPY
}
