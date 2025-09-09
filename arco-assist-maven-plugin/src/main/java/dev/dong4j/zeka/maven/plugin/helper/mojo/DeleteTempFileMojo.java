package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import java.io.Serial;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 代码质量检查临时文件清理Mojo，用于删除CheckStyle和PMD等工具生成的临时文件
 * <p>
 * 该Mojo在Maven源码生成阶段执行，主要用于清理代码质量检查工具产生的临时文件
 * 防止这些临时文件干扰源码管理或占用不必要的磁盘空间
 * 默认情况下处于启用状态，自动清理指定类型的临时文件
 * <p>
 * 主要特性：
 * - 自动清理：默认启用，自动清理指定类型的临时文件
 * - 多类型支持：支持CheckStyle、PMD等多种代码质量检查工具
 * - 可配置性：支持通过参数控制是否执行清理操作
 * - 目录适配：自动在项目构建目录中查找并清理临时文件
 * - 线程安全：支持并发构建环境
 * <p>
 * 支持清理的文件类型：
 * - PMD目录和文件：包括pmd目录和pmd.xml文件
 * - CheckStyle目录和文件：包括checkstyle目录和相关配置文件
 * - 配置文件：checkstyle-checker.xml、checkstyle-suppressions.xml
 * <p>
 * 使用场景：
 * - 清理开发过程中产生的代码质量检查报告文件
 * - 防止临时文件被意外提交到版本控制系统
 * - 保持项目目录的清洁和整洁
 * - CI/CD流水线中的环境清理
 * - IDE和Maven构建的一致性保证
 * <p>
 * 执行配置：
 * - 默认阶段：generate-sources（源码生成阶段）
 * - 目标名称：delete-temp-file
 * - 默认状态：启用（TURN_ON_PLUGIN）
 * - 线程安全：支持
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dongshijie@gmail.com"
 * @date 2020.05.05 21:10
 * @since 1.0.0
 */
@Mojo(name = "delete-temp-file", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class DeleteTempFileMojo extends ZekaMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_DELETE_TEMP_FILE, defaultValue = Plugins.TURN_ON_PLUGIN)
    private boolean skip;

    /** TEMP_FILES */
    private static final List<String> TEMP_FILES = new ArrayList<String>() {
        @Serial
        private static final long serialVersionUID = -7121985380840710018L;

        {
            this.add("pmd");
            this.add("checkstyle");
            this.add("pmd.xml");
            this.add("checkstyle-checker.xml");
            this.add("checkstyle-suppressions.xml");
        }
    };

    /**
     * Execute
     *
     * @since 1.0.0
     */
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("delete-temp-file is skipped");
            return;
        }

        TEMP_FILES.forEach(tempFile -> FileUtils.deleteFiles(FileUtils.appendPath(this.getBuildDirectory(), tempFile)));

    }
}
