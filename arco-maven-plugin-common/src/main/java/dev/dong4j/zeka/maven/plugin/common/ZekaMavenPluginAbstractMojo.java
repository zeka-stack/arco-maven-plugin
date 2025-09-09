package dev.dong4j.zeka.maven.plugin.common;

import dev.dong4j.zeka.maven.plugin.common.enums.ApplicationType;
import dev.dong4j.zeka.maven.plugin.common.util.ReflectionUtils;
import java.io.File;
import java.util.Set;
import lombok.Getter;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Maven插件抽象基类，为所有自定义Maven插件提供统一的基础能力和公共功能
 * <p>
 * 该类继承自Maven的AbstractMojo，集成了Maven构建上下文、项目信息、文件系统操作等核心能力。
 * 提供了简化的API和工具方法，支持属性管理和Spring Boot/Cloud应用类型检测。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.03.07 11:34
 * @since 1.0.0
 */
public abstract class ZekaMavenPluginAbstractMojo extends AbstractMojo {
    /** Maven 项目对象，提供对当前处理项目的元数据访问 */
    @Getter
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    public MavenProject project;
    /** Maven 执行会话对象，提供对全局构建环境的访问 */
    @Getter
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    public MavenSession session;
    /**
     * 项目构建输出目录路径，通常指向target目录
     * <p>
     * 该字段指定了Maven项目构建过程中所有输出文件的根目录路径
     * 包括编译后的class文件、打包生成的jar/war文件、测试报告、生成的资源文件等
     * 插件可以使用该路径来确定输出文件的存放位置和读取构建产物
     * <p>
     * 典型用途：
     * <ul>
     *     <li>确定插件生成文件的输出位置</li>
     *     <li>读取编译后的class文件进行分析</li>
     *     <li>访问打包生成的应用程序文件</li>
     *     <li>创建临时文件和中间处理结果</li>
     *     <li>集成其他构建工具的输出</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Getter
    @Parameter(readonly = true, defaultValue = "${project.build.directory}")
    public String buildDirectory;
    /**
     * 项目根目录路径，指向包含pom.xml文件的目录
     * <p>
     * 该字段提供了项目根目录的绝对路径，是项目中所有相对路径的基准点
     * 通过该路径可以访问项目的源代码、资源文件、配置文件等所有项目内容
     * 插件在处理项目文件时通常以该目录作为起始点进行路径计算
     * <p>
     * 主要应用：
     * <ul>
     *     <li>构建项目内文件的绝对路径</li>
     *     <li>遍历和扫描项目目录结构</li>
     *     <li>相对路径的基准点和锚点</li>
     *     <li>项目文件读取和写入的根路径</li>
     *     <li>与外部工具集成时的工作目录</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Getter
    @Parameter(readonly = true, defaultValue = "${project.basedir}")
    public String basedir;
    /**
     * 目标文件对象，用于指定插件的输出目录或特定输出文件
     * <p>
     * 该字段提供了一个可配置的文件对象，插件可以根据需要将其用作输出目录或特定文件的引用
     * 默认指向项目的构建目录，但可以通过插件配置进行自定义
     * 支持绝对路径和相对路径，相对路径将基于项目根目录进行解析
     * <p>
     * 使用场景：
     * <ul>
     *     <li>指定插件生成文件的输出位置</li>
     *     <li>配置特定文件的读取或写入路径</li>
     *     <li>作为文件操作的目标位置</li>
     *     <li>集成外部工具时的文件参数</li>
     *     <li>支持用户自定义的文件路径配置</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Getter
    @Parameter(defaultValue = "${project.build.directory}")
    public File targetFile;
    /**
     * 构建上下文对象，提供增量构建和文件变更监听支持
     * <p>
     * 该组件集成了Plexus增量构建框架，提供了文件变更检测和增量编译的能力
     * 通过该对象可以判断文件是否发生变更，从而避免不必要的重复处理，提升构建效率
     * 特别适用于大型项目中的性能优化和开发体验提升
     * <p>
     * 核心功能：
     * <ul>
     *     <li>文件变更检测 - 跟踪和识别项目中变更的文件</li>
     *     <li>增量编译支持 - 只处理变更的文件和受影响的依赖</li>
     *     <li>构建状态跟踪 - 记录和管理构建过程的状态信息</li>
     *     <li>IDE集成支持 - 与Eclipse、IntelliJ IDEA等IDE的增量构建集成</li>
     *     <li>性能优化 - 通过网选处理提升整体构建性能</li>
     * </ul>
     * <p>
     * 使用方式：
     * <ul>
     *     <li>调用hasDelta()方法检查文件是否变更</li>
     *     <li>使用isIncremental()判断是否处于增量构建模式</li>
     *     <li>通过refresh()通知IDE刷新项目状态</li>
     *     <li>使用newFileOutputStream()创建文件输出流</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Component
    public BuildContext buildContext;
    /**
     * Maven项目辅助工具，提供项目构建产物的附加和管理功能
     * <p>
     * 该组件为Maven插件提供了向项目附加额外构建产物的能力，支持多种文件类型和分类器
     * 通过该工具可以将插件生成的文件附加到项目中，使其成为正式的项目产物
     * 支持安装和部署阶段的自动处理，无需手动干预
     * <p>
     * 主要功能：
     * <ul>
     *     <li>文件附加 - 将插件生成的文件附加到项目产物列表</li>
     *     <li>分类器支持 - 支持使用分类器标识不同类型的构建产物</li>
     *     <li>元数据管理 - 管理附加文件的元数据和属性信息</li>
     *     <li>版本协调 - 确保附加文件的版本与主项目保持一致</li>
     *     <li>部署集成 - 自动将附加文件包含在部署和发布流程中</li>
     * </ul>
     * <p>
     * 常见使用场景：
     * <ul>
     *     <li>附加源码jar包（classifier: sources）</li>
     *     <li>附加Javadoc文档包（classifier: javadoc）</li>
     *     <li>附加可执行程序包（classifier: executable）</li>
     *     <li>附加配置文件包（classifier: config）</li>
     *     <li>附加特定平台的原生库文件</li>
     * </ul>
     *
     * @since 1.0.0
     */
    @Component
    public MavenProjectHelper projectHelper;

    /**
     * 设置项目属性值，提供统一的属性管理和调试日志功能
     * <p>
     * 该方法封装了Maven项目属性的设置操作，提供了统一的属性管理接口
     * 在调试模式下会输出详细的日志信息，方便问题排查和调试
     * 设置的属性将成为项目的一部分，可以在后续的构建阶段和其他插件中访问
     * <p>
     * 使用场景：
     * <ul>
     *     <li>在插件执行过程中设置动态属性</li>
     *     <li>传递插件计算结果给后续阶段</li>
     *     <li>设置配置参数和环境变量</li>
     *     <li>存储插件状态和中间结果</li>
     *     <li>为模板引擎提供变量替换</li>
     * </ul>
     * <p>
     * 注意事项：
     * <ul>
     *     <li>属性名称应遵循命名规范，建议使用点号分隔</li>
     *     <li>属性值应考虑空值和特殊字符的处理</li>
     *     <li>调试日志会显示属性的完整信息，注意敏感数据</li>
     *     <li>属性设置是线程安全的，但并发访问时需谨慎</li>
     * </ul>
     *
     * @param name  属性名称，不能为空，建议使用点号分隔的命名方式
     * @param value 属性值，可以为空，将会转换为字符串存储
     * @since 1.0.0
     */
    public void defineProperty(String name, String value) {
        if (this.getLog().isDebugEnabled()) {
            this.getLog().debug("define property " + name + " = \"" + value + "\"");
        }

        this.project.getProperties().put(name, value);
    }

    /**
     * Maven插件执行入口方法，子类需要覆盖该方法实现具体的插件逻辑
     * <p>
     * 该方法是Maven插件框架的标准入口点，在插件被调用时会自动执行
     * 基类中提供了空实现，子类可以根据具体需求进行覆盖和扩展
     * 提供了统一的异常处理机制，支持执行失败和业务失败两种类型的异常
     * <p>
     * 实现指南：
     * <ul>
     *     <li>覆盖该方法并实现具体的插件功能</li>
     *     <li>使用getLog()方法输出日志信息</li>
     *     <li>合理利用父类提供的属性和工具方法</li>
     *     <li>正确处理异常情况并提供有意义的错误信息</li>
     *     <li>考虑插件的幂等性和可重复执行性</li>
     * </ul>
     * <p>
     * 异常类型说明：
     * <ul>
     *     <li>MojoExecutionException - 插件执行过程中的技术错误</li>
     *     <li>MojoFailureException - 插件业务逻辑的失败错误</li>
     * </ul>
     * <p>
     * 生命周期和执行时机：
     * <ul>
     *     <li>该方法在Maven插件的指定阶段被调用</li>
     *     <li>执行时机由@Mojo注解的defaultPhase属性决定</li>
     *     <li>可以通过-Dgoals参数手动指定执行目标</li>
     * </ul>
     *
     * @throws MojoExecutionException 当插件执行过程中发生技术错误时抛出，如文件操作失败、网络错误等
     * @throws MojoFailureException   当插件业务逻辑失败时抛出，如校验失败、条件不满足等
     * @since 1.0.0
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

    }

    /**
     * 通过分析项目依赖自动推断应用类型，支持Spring Boot和Spring Cloud架构的智能识别
     * <p>
     * 该方法实现了基于依赖分析的应用类型自动识别算法，无需手动配置即可准确判断项目的技术架构
     * 通过检查项目中的特定依赖，能够智能识别不同类型的Spring应用架构
     * 为插件的条件化执行和差异化处理提供了关键的信息基础
     * <p>
     * 识别算法详解：
     * <p>
     * <b>Spring Cloud + Nacos架构检测</b>：
     * <ul>
     *     <li>检查spring-cloud-context依赖的存在</li>
     *     <li>验证依赖作用域为compile且非可选</li>
     *     <li>同时检查spring-cloud-starter-alibaba-nacos-config依赖</li>
     *     <li>满足条件时返回ApplicationType.CLOUD</li>
     * </ul>
     * <p>
     * <b>Spring Boot架构检测</b>：
     * <ul>
     *     <li>检查spring-boot依赖的存在</li>
     *     <li>验证依赖作用域为compile且非可选</li>
     *     <li>满足条件时返回ApplicationType.BOOT</li>
     * </ul>
     * <p>
     * <b>默认情况处理</b>：
     * <ul>
     *     <li>如果上述条件都不满足，返回ApplicationType.NONE</li>
     *     <li>表示无法识别或非Spring类型的应用</li>
     * </ul>
     * <p>
     * 技术实现特点：
     * <ul>
     *     <li>使用反射机制获取Maven项目的私有依赖集合</li>
     *     <li>采用Stream API进行高效的依赖过滤和匹配</li>
     *     <li>严格检查依赖的作用域和可选性属性</li>
     *     <li>支持多条件组合的复合判断逻辑</li>
     * </ul>
     * <p>
     * 常见使用场景：
     * <ul>
     *     <li>根据应用类型选择不同的构建策略</li>
     *     <li>在打包插件中根据架构选择不同的模板</li>
     *     <li>为不同类型的应用生成不同的启动脚本</li>
     *     <li>在部署插件中根据应用类型配置不同的部署参数</li>
     * </ul>
     * <p>
     * 注意事项：
     * <ul>
     *     <li>该方法依赖于项目的依赖解析结果，应在适当的生命周期阶段调用</li>
     *     <li>检测结果可能会随着项目依赖的变化而变化</li>
     *     <li>对于复杂的模块化项目，应考虑每个模块的具体情况</li>
     *     <li>反射操作可能在不同的Maven版本中具有不同的行为</li>
     * </ul>
     *
     * @return 应用类型枚举值，可能的值为ApplicationType.CLOUD、ApplicationType.BOOT或ApplicationType.NONE
     * @since 1.0.0
     */
    @SuppressWarnings("unchecked")
    protected ApplicationType deduceFromDependencies() {
        // 通过反射获取当前项目的所有依赖
        Set<Artifact> resolvedArtifacts = (Set<Artifact>) ReflectionUtils.getFieldVal(this.project,
            "resolvedArtifacts",
            false);

        boolean cloudType = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.CLOUD_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        boolean bootType = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.BOOT_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        boolean enableNacosConfig = resolvedArtifacts.stream()
            .anyMatch(artifact -> artifact.getArtifactId().equals(Plugins.NCAOS_CONFIG_DEPENDENCY_FALG)
                && artifact.getScope().equals(Plugins.SCOPE_COMPILE)
                && !artifact.isOptional());

        if (cloudType && enableNacosConfig) {
            return ApplicationType.CLOUD;
        }
        return bootType ? ApplicationType.BOOT : ApplicationType.NONE;
    }
}
