package dev.dong4j.arco.maven.plugin.helper.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.11 14:13
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum PackagePlugin {
    /** zip 打包配置插件 */
    ASSEMBLY("org.apache.maven.plugins:maven-assembly-plugin"),
    /** 依赖拷贝插件 */
    DEPENDENCY("org.apache.maven.plugins:maven-dependency-plugin"),
    /** jar 打包插件 */
    JAR("org.apache.maven.plugins:maven-jar-plugin"),
    /** git 提交记录文件生成 */
    GIT_COMMIT("pl.project13.maven:git-commit-id-plugin");

    /** Key */
    private final String key;

    /**
     * Keys list
     *
     * @return the list
     * @since 1.0.0
     */
    public static List<String> keys() {
        List<String> keys = new ArrayList<>();
        Arrays.stream(PackagePlugin.values()).forEach(p -> keys.add(p.getKey()));
        return keys;
    }
}
