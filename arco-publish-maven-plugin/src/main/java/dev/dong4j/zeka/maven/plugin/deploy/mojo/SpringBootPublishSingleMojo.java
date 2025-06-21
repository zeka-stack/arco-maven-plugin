package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>Description: 原始的 springboot 项目部署
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021 -08-15 19:03
 * @since 1.9.0
 */
@Slf4j
public class SpringBootPublishSingleMojo extends ServicePublishMojo implements PublishConfigAnalyse {

    /**
     * Execute
     *
     * @since 1.9.0
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

    }

}
