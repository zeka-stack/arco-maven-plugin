package dev.dong4j.arco.maven.plugin.common;

import lombok.Data;

import java.io.File;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.3
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
