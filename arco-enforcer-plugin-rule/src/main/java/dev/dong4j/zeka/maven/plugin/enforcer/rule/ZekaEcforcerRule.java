package dev.dong4j.zeka.maven.plugin.enforcer.rule;

import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.enforcer.rule.api.EnforcerRule;
import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.enforcer.rule.api.EnforcerRuleHelper;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.rtinfo.RuntimeInformation;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.jetbrains.annotations.NotNull;

/**
 * Maven Enforcer自定义规则实现，用于在构建过程中强制执行特定的项目规范和约束
 * <p>
 * 该类实现了Maven Enforcer Plugin的EnforcerRule接口，提供了自定义的构建规则校验机制
 * 可以用于检查依赖关系、版本兼容性、编码规范等各种项目规范
 * 当检查失败时可以中断构建过程，确保项目质量和一致性
 * <p>
 * 主要特性：
 * - 自定义规则：可以实现各种项目特定的校验规则
 * - 构建控制：规则失败时可以中断整个构建过程
 * - 上下文访问：可以访问完整的Maven构建上下文和组件
 * - 缓存支持：支持结果缓存以提高构建性能
 * - 灵活配置：支持通过参数控制规则行为
 * <p>
 * 实现的核心方法：
 * <p>
 * <b>execute(EnforcerRuleHelper)</b>：
 * - 执行具体的规则校验逻辑
 * - 通过helper获取Maven项目、会话、组件等信息
 * - 可以访问项目属性、依赖、配置等各种数据
 * - 校验失败时抛出EnforcerRuleException异常
 * <p>
 * <b>getCacheId()</b>：
 * - 返回规则的唯一标识符，用于缓存管理
 * - 应该基于规则参数计算唯一的哈希值
 * - 相同参数的规则应该返回相同ID
 * <p>
 * <b>isCacheable()</b>：
 * - 返回规则是否可以被缓存
 * - 可缓存的规则能够显著提高构建性能
 * - 依赖外部状态的规则不应该被缓存
 * <p>
 * <b>isResultValid(EnforcerRule)</b>：
 * - 验证缓存的结果是否仍然有效
 * - 可以执行额外的校验确保结果准确性
 * - 通常用于双重检查缓存结果
 * <p>
 * 可访问的Maven上下文信息：
 * - <b>MavenProject</b>：当前项目的所有信息（依赖、属性、配置等）
 * - <b>MavenSession</b>：当前构建会话的全局信息
 * - <b>ArtifactResolver</b>：依赖解析器，用于查找和解析依赖
 * - <b>RuntimeInformation</b>：Maven运行时环境信息
 * - <b>表达式求值</b>：支持${project.xxx}等表达式求值
 * <p>
 * 常见的应用场景：
 * - <b>依赖冲突检查</b>：检测和防止依赖版本冲突
 * - <b>版本规范检查</b>：强制执行特定的版本命名规范
 * - <b>代码质量门禁</b>：检查代码覆盖率、复杂度等指标
 * - <b>安全规范检查</b>：禁止使用不安全的依赖或配置
 * - <b>企业规范校验</b>：强制执行公司级的编码和架构规范
 * <p>
 * 配置示例：
 * <pre>
 * &lt;plugin&gt;
 *     &lt;groupId&gt;org.apache.maven.plugins&lt;/groupId&gt;
 *     &lt;artifactId&gt;maven-enforcer-plugin&lt;/artifactId&gt;
 *     &lt;executions&gt;
 *         &lt;execution&gt;
 *             &lt;goals&gt;
 *                 &lt;goal&gt;enforce&lt;/goal&gt;
 *             &lt;/goals&gt;
 *             &lt;configuration&gt;
 *                 &lt;rules&gt;
 *                     &lt;myCustomRule implementation="dev.dong4j.zeka.maven.plugin.enforcer.rule.ZekaEcforcerRule"&gt;
 *                         &lt;shouldIfail&gt;false&lt;/shouldIfail&gt;
 *                     &lt;/myCustomRule&gt;
 *                 &lt;/rules&gt;
 *             &lt;/configuration&gt;
 *         &lt;/execution&gt;
 *     &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 * <p>
 * 使用场景：
 * - CI/CD流水线中的质量门禁控制
 * - 大型项目的依赖管理和规范统一
 * - 开源项目的质量保证和兼容性检查
 * - 企业内部的编码规范和架构约束
 * - 安全规范的自动化检查和强制执行
 * <p>
 * 扩展建议：
 * - TODO: 实现依赖冲突的自动检测和报告
 * - TODO: 增加可配置的规则参数和阈值
 * - TODO: 支持更多的Maven组件和服务访问
 * - TODO: 提供更友好的错误信息和修复建议
 * <p>
 * 相关文档：
 * - <a href="http://maven.apache.org/enforcer/enforcer-rules/index.html">Maven Enforcer Plugin Rules</a>
 * - <a href="http://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html">Writing Custom Rules</a>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.01.26 20:50
 * @since 1.0.0
 */
@SuppressWarnings("all")
public class ZekaEcforcerRule implements EnforcerRule {
    /** Should ifail */
    private boolean shouldIfail = false;

    /**
     * Execute *
     *
     * @param helper helper
     * @throws EnforcerRuleException enforcer rule exception
     * @since 1.0.0
     */
    @Override
    public void execute(@NotNull EnforcerRuleHelper helper) throws EnforcerRuleException {
        Log log = helper.getLog();

        try {
            // get the various expressions out of the helper.
            MavenProject project = (MavenProject) helper.evaluate("${project}");
            MavenSession session = (MavenSession) helper.evaluate("${session}");
            String target = (String) helper.evaluate("${project.build.directory}");
            String artifactId = (String) helper.evaluate("${project.artifactId}");

            // retreive any component out of the session directly
            ArtifactResolver resolver = helper.getComponent(ArtifactResolver.class);
            RuntimeInformation rti = helper.getComponent(RuntimeInformation.class);

            log.info("Retrieved Target Folder: " + target);
            log.info("Retrieved ArtifactId: " + artifactId);
            log.info("Retrieved Project: " + project);
            log.info("Retrieved RuntimeInfo: " + rti);
            log.info("Retrieved Session: " + session);
            log.info("Retrieved Resolver: " + resolver);

        } catch (ComponentLookupException e) {
            throw new EnforcerRuleException("Unable to lookup a component " + e.getLocalizedMessage(), e);
        } catch (ExpressionEvaluationException e) {
            throw new EnforcerRuleException("Unable to lookup an expression " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * 如果规则是可缓存的,则当参数或条件发生更改时,必须返回唯一的id,这将导致结果不同.
     * 多个缓存结果基于其id存储.
     * 最简单的方法是返回根据参数值计算的散列.
     * 如果规则不可缓存,则此处的结果不重要,您可以返回任何内容.
     *
     * @return the cache id
     * @since 1.0.0
     */
    @Override
    public String getCacheId() {
        // no hash on boolean...only parameter so no hash is needed.
        return "" + this.shouldIfail;
    }

    /**
     * 这告诉系统结果是否可以缓存.
     * 请记住,在分叉构建和其他操作期间,可以为同一个项目多次执行给定的规则.
     * 这意味着,即使是从一个项目更改到另一个项目的内容,在某些情况下仍然可以缓存.
     *
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean isCacheable() {
        return false;
    }

    /**
     * 如果规则是可缓存的,并且在缓存中找到相同的id,则存储的结果将传递给此方法,以允许对结果进行双重检查.
     * 大多数情况下,这可以通过生成唯一的id来完成,但有时需要查询helper返回的对象的结果.
     * 例如,可以将某些对象存储在规则中,然后稍后查询它们.
     *
     * @param arg0 arg 0
     * @return the boolean
     * @since 1.0.0
     */
    @Override
    public boolean isResultValid(@NotNull EnforcerRule arg0) {
        return false;
    }
}
