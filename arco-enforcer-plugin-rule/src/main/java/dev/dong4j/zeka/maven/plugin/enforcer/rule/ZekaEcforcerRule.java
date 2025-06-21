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
 * <p>Description: maven-enforcer-plugin 自定义规则 </p>
 * http://maven.apache.org/enforcer/enforcer-rules/index.html
 * http://maven.apache.org/enforcer/enforcer-api/writing-a-custom-rule.html
 * todo-dong4j : (2020年05月10日 10:21 下午) [检查到依赖冲突则编译失败]
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
