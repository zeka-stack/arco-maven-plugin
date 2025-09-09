package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import java.io.File;
import java.util.Collections;
import java.util.List;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Webpack 构建工具 Maven 插件
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 23:54
 * @since 1.0.0
 */
@Mojo(name = "webpack", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public final class WebpackMojo extends AbstractFrontendMojo {

    /** Webpack 命令参数，默认为空（只执行 "webpack" 命令） */
    @Parameter(property = "frontend.webpack.arguments")
    private String arguments;

    /** 需要检查变更的触发文件列表，默认为 webpack.config.js */
    @Parameter(property = "triggerfiles")
    private List<File> triggerfiles;

    /** 包含前端文件的源目录，用于检查文件变更 */
    @Parameter(property = "srcdir")
    private File srcdir;

    /** Webpack 输出目录，用于在 Eclipse 中刷新文件状态 */
    @Parameter(property = "outputdir")
    private File outputdir;

    /** 是否跳过插件执行 */
    @Parameter(property = "skip.webpack", defaultValue = "${skip.webpack}")
    private boolean skip;

    /** 构建上下文 */
    @Component
    private BuildContext buildContext;

    /**
     * 判断是否跳过当前插件的执行
     *
     * @return true 如果需要跳过执行
     * @since 1.0.0
     */
    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    /**
     * 执行 Webpack 构建任务，支持增量构建和文件刷新
     *
     * @param factory 前端插件工厂
     * @throws TaskRunnerException 任务执行失败时抛出
     * @since 1.0.0
     */
    @Override
    public synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        if (this.shouldExecute()) {
            factory.getWebpackRunner().execute(this.arguments, this.environmentVariables);

            if (this.outputdir != null) {
                this.getLog().info("Refreshing files after webpack: " + this.outputdir);
                this.buildContext.refresh(this.outputdir);
            }
        } else {
            this.getLog().info("Skipping webpack as no modified files in " + this.srcdir);
        }
    }

    /**
     * 根据文件变更情况判断是否需要执行 Webpack 任务
     *
     * @return true 如果需要执行
     * @since 1.0.0
     */
    private boolean shouldExecute() {
        if (this.triggerfiles == null || this.triggerfiles.isEmpty()) {
            this.triggerfiles = Collections.singletonList(new File(this.workingDirectory, "webpack.config.js"));
        }

        return MojoUtils.shouldExecute(this.buildContext, this.triggerfiles, this.srcdir);
    }

}
