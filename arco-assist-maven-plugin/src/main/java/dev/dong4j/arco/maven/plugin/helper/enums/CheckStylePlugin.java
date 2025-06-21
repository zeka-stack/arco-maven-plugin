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
 * @date 2020.03.11 15:47
 * @since 1.0.0
 */
@Getter
@AllArgsConstructor
public enum CheckStylePlugin {
    /** zip 打包配置插件 */
    PMD("org.apache.maven.plugins:maven-pmd-plugin"),
    /** 依赖拷贝插件 */
    CHECKSTYLE("org.apache.maven.plugins:maven-checkstyle-plugin");

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
        Arrays.stream(CheckStylePlugin.values()).forEach(p -> keys.add(p.getKey()));
        return keys;
    }
}
