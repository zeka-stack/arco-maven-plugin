package dev.dong4j.zeka.maven.plugin.helper.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import java.io.File;
import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 编译后标识文件生成Mojo，用于生成编译成功的标识文件
 * <p>
 * 该Mojo在Maven编译阶段执行，用于在正常编译完成后生成一个时间戳标识文件
 * 这个标识文件可以用于跟踪编译状态、编译时间或作为其他构建步骤的依赖条件
 * <p>
 * 主要特性：
 * - 编译成功标识：在编译阶段成功后生成标识文件
 * - 时间戳记录：记录编译完成的精确时间戳
 * - 可配置路径：支持自定义标识文件的生成位置
 * - 条件执行：支持通过参数控制是否执行
 * - 线程安全：支持并发构建环境
 * <p>
 * 使用场景：
 * - CI/CD流水线中的编译状态跟踪
 * - 渐进式构建中的编译缓存验证
 * - 构建工具和 IDE 的集成
 * - 自动化测试中的前置条件检查
 * - 多模块项目中的依赖编译状态管理
 * <p>
 * 执行配置：
 * - 默认阶段：compile（编译阶段）
 * - 目标名称：generate-compiled-id
 * - 线程安全：支持
 * <p>
 * 配置参数：
 * - skip：是否跳过该Mojo的执行
 * - checkFile：标识文件的生成路径
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.05.14 13:05
 * @since 1.0.0
 */
@Mojo(name = "generate-compiled-id", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class CompiledProcessorMojo extends ZekaMavenPluginAbstractMojo {
    /** 是否跳过此Mojo的执行，默认为false（不跳过） */
    @Parameter(property = Plugins.SKIP_COMPILED_ID, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /**
     * 编译标识文件的生成位置
     * <p>
     * 标识文件会在编译成功后在此路径生成
     * 文件内容为当前的时间戳，用于标识编译完成的时间
     */
    @Parameter(defaultValue = "${project.build.directory}/maven-status/maven-compiler-plugin/compile/identify/checked")
    private File checkFile;

    /**
     * 执行编译标识文件生成逻辑
     * <p>
     * 该方法会在Maven的compile阶段被调用，主要执行以下操作：
     * 1. 检查是否被配置为跳过执行
     * 2. 生成包含当前时间戳的标识文件
     * 3. 通知构建上下文刷新文件状态
     * <p>
     * 注意：使用@SneakyThrows注解处理异常，简化代码结构
     *
     * @since 1.0.0
     */
    @SneakyThrows
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("generate-compiled-id is skipped");
            return;
        }

        new FileWriter(this.checkFile).writeContent(String.valueOf(System.currentTimeMillis()));
        this.buildContext.refresh(this.checkFile);
    }
}
