package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import cn.hutool.core.collection.CollectionUtil;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.entity.Group;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.entity.Server;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>Description: 部署配置解析 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021.08.05 14:14
 * @since 1.9.0
 */
public interface PublishConfigAnalyse {

    /**
     * Analyse
     *
     * @param groups    groups
     * @param finalName final name
     * @param hosts     hosts
     * @param env       env
     * @param log       log
     * @since 1.7.0
     */
    default void analyse(List<Group> groups, String finalName, String hosts, String env, Log log) {
        if (StringUtils.isNotBlank(hosts)) {
            Group group = new Group();
            group.setEnv(env);
            group.setEnable(true);
            Set<String> devHosts = Stream.of(hosts.split(",")).map(StringUtils::stripToEmpty).collect(Collectors.toSet());
            if (CollectionUtil.isNotEmpty(devHosts)) {
                Set<Server> serverList = new HashSet<>();
                devHosts.forEach(host -> {
                    if (StringUtils.isNotBlank(host)) {
                        Server server = new Server();
                        server.setHost(host.trim());
                        server.setNames(Collections.singletonList(finalName));
                        serverList.add(server);
                    }
                });
                group.setServers(serverList);
            }
            groups.add(group);
        } else {
            log.warn("-Dpublish.env 参数错误: " + env + " 不存在, 忽略部署.");
        }
    }
}
