package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.thread.ThreadUtil;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.entity.Group;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.util.SSHAgent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: v4 spring boot 项目部署 </p>
 *
 * @author dong4j
 * @version 1.1.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021.01.29 01:10
 * @since 1.7.3
 */
@Slf4j
@Mojo(name = "v4-publish-single", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class V4ServicePublishSingleMojo extends ServicePublishSingleMojo {
    /** PACKAGE_SUFFIX */
    private static final String PACKAGE_SUFFIX = ".jar";

    /**
     * Publish
     *
     * @param groups groups
     * @since 1.7.3
     */
    @Override
    protected void publish(@NotNull List<Group> groups) {

        Map<String, File> map = new HashMap<>(8);

        Iterator<File> files = FileUtils.iterateFiles(this.targetFile, new String[]{"jar"}, true);
        files.forEachRemaining(f -> {
            String name = f.getName();
            String noSuffixFileName = name.replaceAll(PACKAGE_SUFFIX, "");
            map.put(noSuffixFileName, f);
            log.info("{} -> {}", name, noSuffixFileName);
        });

        this.publish(groups, map);
    }

    /**
     * Upload and publish
     *
     * @param sshAgent        ssh agent
     * @param publishFileName publish file name
     * @param file            file
     * @param groupId         group id
     * @param env             env
     * @since 1.7.3
     */
    @SneakyThrows
    @Override
    protected void betweenStep(@NotNull SSHAgent sshAgent,
                               String publishFileName,
                               File file,
                               String groupId,
                               String env) {
        // 创建部署目录
        sshAgent.execCommand("3.5. 创建 dubbo 缓存目录", StrFormatter.format("sudo mkdir -p {}/{}/dubbo-registry",
            this.publishTargetPath,
            groupId));
        // 授权
        sshAgent.execCommand("3.5.1 修改用户组",
            StrFormatter.format("sudo chown -R zekastack:zekastack {}/{}/*", this.publishTargetPath, groupId));

        this.runServerShell(sshAgent, groupId, env, publishFileName);

    }

    /**
     * Run server shell
     *
     * @param sshAgent        ssh agent
     * @param groupId         group id
     * @param env             env
     * @param publishFileName publish file name
     * @throws IOException io exception
     * @since 1.7.3
     */
    @Override
    protected void runServerShell(@NotNull SSHAgent sshAgent,
                                  String groupId,
                                  String env,
                                  String publishFileName) throws IOException {

        // 干掉原来的进程
        sshAgent.execCommand("3.6 停止老进程",
            StrFormatter.format("sudo kill -9 `ps -ef | grep -v grep | grep {}"
                    + PACKAGE_SUFFIX
                    + " | awk '{print $2}'`",
                publishFileName,
                env));

        ThreadUtil.safeSleep(3000);

        String cmd = "cd {}/{}; sudo -E -u zekastack  nohup {}/java {} -jar {}.jar >/dev/null 2>&1 &";
        if (this.enbaleApm) {
            cmd = "cd {}/{}; sudo -E -u zekastack nohup {}/java {}"
                + " -javaagent:/opt/skywalking/agent/skywalking-agent.jar"
                + " -Dskywalking.agent.service_name=" + publishFileName + "@" + env + " -jar {} >/dev/null 2>&1 &";
        }

        sshAgent.execCommand("执行部署脚本",
            StrFormatter.format(cmd,
                this.publishTargetPath,
                groupId,
                this.javaHome,
                JVM_OPTIONS,
                publishFileName), false);
    }

    /**
     * 修复日志目录权限
     *
     * @param sshAgent ssh agent
     * @param env      env
     * @throws IOException io exception
     * @since 1.7.0
     */
    @Override
    protected void fixLogDirectoryPermission(@NotNull SSHAgent sshAgent, String env, String groupId) throws IOException {
        sshAgent.execCommand("修复日志目录权限", "sudo chown -R zekastack:zekastack /mnt/syslogs/tomcat/*");
    }
}
