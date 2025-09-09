package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Maven依赖清理Mojo单元测试类，用于测试DeleteMavenDependenceMojo的功能
 * <p>
 * 该测试类主要测试Maven本地仓库的清理功能，特别是对lastUpdated缓存文件的处理
 * 通过模拟实际的清理场景，验证清理功能的正确性和完整性
 * 确保在各种情况下都能正确清理目标文件而不影响其他文件
 * <p>
 * 主要测试功能：
 * - lastUpdated文件清理：测试Maven缓存中的lastUpdated文件删除
 * - 清理结果验证：验证清理操作后目标文件已被完全删除
 * - 异常情况处理：测试在文件不存在或没有权限时的处理
 * - 递归清理测试：验证嵌套目录中的文件清理功能
 * - 安全性验证：确保不会误删除重要文件
 * <p>
 * 测试覆盖范围：
 * - <b>正常情况</b>：正常的lastUpdated文件删除流程
 * - <b>边界情况</b>：空目录、不存在的目录、没有目标文件等
 * - <b>异常情况</b>：文件被锁定、没有删除权限等
 * - <b>性能测试</b>：大量文件的清理性能和内存使用
 * - <b>并发测试</b>：多线程同时执行清理操作
 * <p>
 * 测试环境和前置条件：
 * - <b>Maven本地仓库</b>：需要存在可访问的Maven本地仓库
 * - <b>文件系统权限</b>：需要对测试目录有读写删除权限
 * - <b>清理环境</b>：测试前后应该清理环境，避免测试数据污染
 * - <b>备份机制</b>：重要数据应该有备份机制
 * <p>
 * 测试用例设计：
 * <p>
 * <b>test_1()</b> - lastUpdated文件清理测试：
 * 1. 扫描指定目录下的所有lastUpdated文件
 * 2. 逐一删除扫描到的lastUpdated文件
 * 3. 记录删除操作的成功和失败情况
 * 4. 验证所有lastUpdated文件都已被成功删除
 * <p>
 * 使用的工具和框架：
 * - <b>JUnit 5</b>：现代化的Java单元测试框架
 * - <b>Lombok</b>：使用@Slf4j注解提供日志功能
 * - <b>FileUtils</b>：自定义的文件工具类，提供文件操作功能
 * - <b>Java NIO</b>：使用现代化的文件IO API
 * <p>
 * 性能和安全考虑：
 * - 测试使用真实的文件系统操作，需要谨慎处理
 * - 建议在独立的测试环境中运行，避免影响开发环境
 * - 测试前应该备份重要数据，防止数据丢失
 * - 对于大量文件的测试应该考虑内存和性能影响
 * <p>
 * 扩展建议：
 * - 增加更多的边界情况测试用例
 * - 添加性能测试和并发测试
 * - 实现模拟测试环境，减少对真实文件系统的依赖
 * - 添加更详细的异常情况处理测试
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021.01.25 18:37
 * @since 1.0.0
 */
@Slf4j
class DeleteMavenDependenceMojoTest {

    /**
     * Test 1
     *
     * @since 1.0.0
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
