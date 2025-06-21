package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021.01.25 18:37
 * @since 1.8.0
 */
@Slf4j
class DeleteMavenDependenceMojoTest {

    /**
     * Test 1
     *
     * @since 1.8.0
     */
    @Test
    void test_1() {
        File file = new File("/Users/dong4j/.m2/repository/dev/dong4j");
        Collection<File> listFiles = FileUtils.listFiles(file, new String[]{"lastUpdated"}, true);
        listFiles.forEach(f -> {
            try {
                Files.delete(f.toPath());
                log.info("delete: {} ", f);
            } catch (IOException e) {
                log.error("delete: {} ", f);
            }
        });

        Assertions.assertTrue(FileUtils.listFiles(file, new String[]{"lastUpdated"}, true).isEmpty());
    }

}
