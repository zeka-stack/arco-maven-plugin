package dev.dong4j.arco.maven.plugin.common;

import lombok.experimental.UtilityClass;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.12 16:08
 * @since 1.5.0
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
    public static final String SKIP_BUILD_ACTIVE_FILE = "build.profile.active.file.skip";
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

}
