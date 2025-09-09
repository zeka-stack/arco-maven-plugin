package dev.dong4j.zeka.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendException;
import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import java.io.File;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;

/**
 * 前端构建工具Maven插件的抽象基类，为所有前端技术栈插件提供统一的基础能力
 * <p>
 * 该抽象类封装了前端构建工具的公共功能和配置项，为各种前端技术栈的Maven插件实现提供统一的基础架构
 * 集成了frontend-maven-plugin的核心功能，支持Node.js、npm、Yarn等前端工具链的统一管理
 * <p>
 * 核心功能特性：
 * <ul>
 *     <li>测试阶段控制 - 支持在测试阶段跳过前端构建任务</li>
 *     <li>失败容错 - 支持在测试阶段忽略构建失败继续执行</li>
 *     <li>工作目录管理 - 灵活配置前端代码和工具的工作目录</li>
 *     <li>安装目录管理 - 支持自定义Node.js和npm的安装目录</li>
 *     <li>环境变量支持 - 允许传递额外的环境变量给构建进程</li>
 * </ul>
 * <p>
 * 生命周期管理：
 * <ul>
 *     <li>智能阶段检测 - 自动识别当前执行阶段是否为测试阶段</li>
 *     <li>条件执行 - 根据配置参数和阶段类型决定是否执行构建任务</li>
 *     <li>异常处理 - 完善的错误处理机制和失败恢复能力</li>
 *     <li>日志管理 - 统一的日志输出和错误报告机制</li>
 * </ul>
 * <p>
 * 该抽象类特别适用于现代前端开发流程中的打包、构建、测试等环节
 * 通过Maven插件的方式将前端工具链集成到Java项目的构建流程中，实现统一的构建和部署
 * <p>
 * 技术集成特点：
 * <ul>
 *     <li>集成frontend-maven-plugin提供成熟的前端工具支持</li>
 *     <li>支持Maven的标准参数和生命周期管理</li>
 *     <li>集成仓库缓存解析器提供依赖管理能力</li>
 *     <li>支持多种前端技术栈和工具链的扩展</li>
 * </ul>
 * <p>
 * 使用场景：
 * <ul>
 *     <li>前后端分离架构中的前端代码构建和集成</li>
 *     <li>React、Vue、Angular等SPA应用的Maven构建支持</li>
 *     <li>微服务项目中的管理后台页面构建</li>
 *     <li>持续集成中的自动化前端构建流程</li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 19:28
 * @since 1.0.0
 */
public abstract class AbstractFrontendMojo extends AbstractMojo {

    /** Execution */
    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    protected MojoExecution execution;

    /**
     * Whether you should skip while running in the test phase (default is false)
     */
    @Parameter(property = "skipTests", required = false, defaultValue = "false")
    protected Boolean skipTests;

    /**
     * Set this to true to ignore a failure during testing. Its use is NOT RECOMMENDED, but quite convenient on
     * occasion.
     *
     * @since 1.4
     */
    @Parameter(property = "maven.test.failure.ignore", defaultValue = "false")
    protected boolean testFailureIgnore;

    /**
     * The base directory for running all Node commands. (Usually the directory that contains package.json)
     */
    @Parameter(defaultValue = "${basedir}", property = "workingDirectory", required = false)
    protected File workingDirectory;

    /**
     * The base directory for installing node and npm.
     */
    @Parameter(property = "installDirectory", required = false)
    protected File installDirectory;

    /**
     * Additional environment variables to pass to the build.
     */
    @Parameter
    protected Map<String, String> environmentVariables;

    /** Project */
    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    /** Repository system session */
    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
    private RepositorySystemSession repositorySystemSession;

    /**
     * Determines if this execution should be skipped.
     *
     * @return the boolean
     * @since 1.0.0
     */
    private boolean skipTestPhase() {
        return this.skipTests && this.isTestingPhase();
    }

    /**
     * Determines if the current execution is during a testing phase (e.g., "test" or "integration-test").
     *
     * @return the boolean
     * @since 1.0.0
     */
    private boolean isTestingPhase() {
        String phase = this.execution.getLifecyclePhase();
        return "test".equals(phase) || "integration-test".equals(phase);
    }

    /**
     * Execute
     *
     * @param factory factory
     * @throws FrontendException frontend exception
     * @since 1.0.0
     */
    protected abstract void execute(FrontendPluginFactory factory) throws FrontendException;

    /**
     * Implemented by children to determine if this execution should be skipped.
     *
     * @return the boolean
     * @since 1.0.0
     */
    protected abstract boolean skipExecution();

    /**
     * Execute
     *
     * @throws MojoFailureException mojo failure exception
     * @since 1.0.0
     */
    @Override
    @SuppressWarnings("java:S3776")
    public void execute() throws MojoFailureException {
        if (this.testFailureIgnore && !this.isTestingPhase()) {
            this.getLog().info("testFailureIgnore property is ignored in non test phases");
        }
        if (!(this.skipTestPhase() || this.skipExecution())) {
            if (this.installDirectory == null) {
                this.installDirectory = this.workingDirectory;
            }
            try {
                this.execute(new FrontendPluginFactory(this.workingDirectory, this.installDirectory,
                    new RepositoryCacheResolver(this.repositorySystemSession)));
            } catch (TaskRunnerException e) {
                if (this.testFailureIgnore && this.isTestingPhase()) {
                    this.getLog().error("There are test failures.\nFailed to run task: " + e.getMessage(), e);
                } else {
                    throw new MojoFailureException("Failed to run task", e);
                }
            } catch (FrontendException e) {
                throw MojoUtils.toMojoFailureException(e);
            }
        } else {
            this.getLog().info("Skipping execution.");
        }
    }

}
