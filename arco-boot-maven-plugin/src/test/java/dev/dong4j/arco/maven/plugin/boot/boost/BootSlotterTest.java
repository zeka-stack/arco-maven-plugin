package dev.dong4j.arco.maven.plugin.boot.boost;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.30 16:34
 * @since 1.0.0
 */
@Slf4j
class BootSlotterTest {

    /**
     * Test 1
     *
     * @since 1.0.0
     */
    @Test
    void test_1() {
        String orginalMainClass = "sample.launcher.SampleLauncherApplication";
        String path = orginalMainClass.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        Assertions.assertEquals("sample/launcher/SampleLauncherApplication", path);

        Set<String> directories = new HashSet<>();

        File file = new File(path);

        for (File parentFile = file.getParentFile(); parentFile != null; parentFile = parentFile.getParentFile()) {
            String directory = parentFile.getPath() + "/";
            directories.add(directory);
        }

        log.info("{}", directories);
    }

    @Test
    void test_2() {
        String orginalMainClass1 = "dev.dong4j.arco.maven.plugin.boot";
        Assertions.assertEquals("dev/dong4j/arco/maven/plugin/boot", orginalMainClass1.replaceAll("\\.",
            Matcher.quoteReplacement(File.separator)));
        String orginalMainClass2 = "dev.dong4j.arco.maven.plugin.boot";
        Assertions.assertEquals("dev/dong4j/arco/maven/plugin/boot", orginalMainClass2.replace(".",
            Matcher.quoteReplacement(File.separator)));
    }
}
