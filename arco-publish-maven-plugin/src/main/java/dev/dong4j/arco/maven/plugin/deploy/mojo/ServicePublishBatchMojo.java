package dev.dong4j.arco.maven.plugin.deploy.mojo;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

/**
 * <p>Description: 结合 distribution 批量部署 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 13:01
 * @since 1.6.0
 */
@Slf4j
@SuppressWarnings("all")
@Mojo(name = "publish-batch", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class ServicePublishBatchMojo extends ServicePublishMojo {

    /**
     * Execute
     *
     * @since 1.6.0
     */
    @Override
    public void subExecute() {
        if (StringUtils.isBlank(this.publishGroupId)) {
            this.getLog().warn("publish.group.id 未配置, 忽略部署.");
            return;
        }

        if (!Boolean.TRUE.equals(this.enbaleApm)) {
            this.getLog().info("可添加 Maven 参数: '-Dapm.enable=true' 启用 APM 功能 --> http://apm.server:8080");
        }

        if (this.skip) {
            this.getLog().warn("publish-batch 插件被忽略执行: "
                + "1. packaging = pom;2. 不存在 java 文件; 3. 不是启动模块(不存在 SpringBoot 启动类) ");
            return;
        }

        if (CollectionUtil.isEmpty(this.groups)) {
            this.getLog().warn("未配置部署服务相关配置, 忽略部署");
            return;
        }

        try {
            this.publish(this.groups);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
