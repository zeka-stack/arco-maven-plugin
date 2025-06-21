package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import cn.hutool.core.collection.CollectionUtil;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;

/**
 * <p>Description: 独立部署 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 13:01
 * @since 1.6.0
 */
@Slf4j
@SuppressWarnings("all")
@Mojo(name = "publish-single", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class ServicePublishSingleMojo extends ServicePublishMojo implements PublishConfigAnalyse {

    /** Final name */
    @Parameter(defaultValue = "${project.build.finalName}")
    private String finalName;

    /** Skip */
    @Parameter(property = Plugins.SKIP_PUBLISH_SINGLE, defaultValue = Plugins.TURN_OFF_PLUGIN)
    public boolean skip;

    /**
     * Execute
     *
     * @since 1.6.0
     */
    @SneakyThrows
    @Override
    public void subExecute() {

        if (StringUtils.isBlank(this.publishGroupId)) {
            this.getLog().warn("publish.group.id 未配置, 忽略部署.");
            return;
        }

        if (!Boolean.TRUE.equals(this.enbaleApm)) {
            this.getLog().info("可添加 Maven 参数: '-Dapm.enable=true' 启用 APM 功能 --> http://apm.server:8080");
        }

        if (this.skip || !enbalePublish) {
            this.getLog().warn("publish-single 插件被忽略执行: "
                + "1. packaging = pom; 2. 不存在 java 文件; 3. 不是启动模块(不存在 SpringBoot 启动类) ");
            return;
        }

        if (CollectionUtil.isNotEmpty(this.groups)) {
            this.getLog().error("部署单个服务不再支持使用 configuration.groups, 请直接使用 publish.hosts.${env}. (env: dev, test, prev)");
            return;
        }

        if (NO_ENVIRONMENT.equals(environment)) {
            this.getLog().warn("可使用 -Dpublish.env=prev 部署到预演环境, 如果未指定将遍历 publish.hosts.${env} 并部署到多个环境, "
                + "可避免修改 pom 配置. (env: dev, test, prev)");
        }

        // 不使用配置的 groups
        this.groups = new ArrayList<>();
        this.processor();
        this.publish(this.groups);
    }

    /**
     * Build group
     *
     * @param hosts hosts
     * @param env   env
     * @since 1.9.0
     */
    @Override
    protected void buildGroup(String hosts, String env) {
        this.analyse(this.groups, this.finalName, hosts, env, this.getLog());
    }
}
