package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ZipUtil;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.entity.Group;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.util.SSHAgent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: 发布前端服务 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 17:57
 * @since 1.7.0
 */
@Slf4j
@Mojo(name = "publish-frontend", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class FrontendPublishMojo extends ServicePublishMojo implements PublishConfigAnalyse {
    /** 打包后的名称 */
    private String packageName;
    /** DEFAULT_TAG */
    private static final String DEFAULT_TAG = "frontend";

    /**
     * Execute
     *
     * @since 1.7.0
     */
    @Override
    public void subExecute() {
        if (CollectionUtil.isNotEmpty(this.groups)) {
            this.getLog().error("部署单个服务不再支持使用 configuration.groups, 请直接使用 publish.hosts.${env}. (env: dev, test, prev)");
            return;
        }

        // 重写默认上传目录
        this.publishUploadPath = this.project.getProperties()
            .getProperty("publish.upload.path",
                "/home/"
                    + this.username
                    + "/" + DEFAULT_TAG);

        // 前端默认部署目录
        this.publishTargetPath = this.project.getProperties().getProperty("publish.target.path", "/opt/" + DEFAULT_TAG);
        this.packageName = this.publishGroupId;

        try {
            // 不使用配置的 groups
            this.groups = new ArrayList<>();
            this.processor();
            this.publish(this.groups);
        } catch (Exception e) {
            log.error("", e);
        }
    }

    /**
     * Publish
     *
     * @param groups groups
     * @since 1.6.0
     */
    @Override
    protected void publish(@NotNull List<Group> groups) {
        File distFile = this.check();
        // 将 distFile 打包成 packageName.zip 压缩包
        File zip = ZipUtil.zip(distFile.getPath(),
            FileUtils.appendPath(distFile.getParent(), this.packageName + ".zip"));

        groups.stream()
            .peek(group -> {
                if (!group.isEnable()) {
                    log.warn("{}: group.enable = false, 忽略部署", group.getEnv());
                }
            })
            .filter(Group::isEnable)
            .forEach(g -> {
                String env = g.getEnv();
                g.getServers().forEach(s -> {
                    // 上传到 host 到 publish.upload.path
                    SSHAgent sshAgent = this.buildAgent(s.getHost());
                    this.uploadAndPublish(sshAgent, this.packageName, zip, "", env);
                });

            });

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
        this.analyse(this.groups, this.packageName, hosts, env, this.getLog());
    }

    /**
     * Upload and publish
     *
     * @param sshAgent        ssh 客户端
     * @param publishFileName 部署包名
     * @param file            待部署的服务压缩包
     * @param groupId         服务分组名
     * @param env             部署环境
     * @since 1.7.0
     */
    @SneakyThrows
    @Override
    protected void uploadAndPublish(@NotNull SSHAgent sshAgent, String publishFileName, File file, String groupId, String env) {

        StopWatch stopWatch = StopWatch.createStarted();
        // 创建部署目录
        sshAgent.execCommand("1. 创建部署目录", StrFormatter.format("sudo mkdir -p {}/{}",
            this.publishTargetPath,
            env));

        // 上传到 /home/{user}/{group}/{env} 目录
        sshAgent.transferFile(file, this.publishUploadPath + "/" + env);

        // 移动到 publishTargetPath 目录
        sshAgent.execCommand("2. 移动部署包到部署目录", StrFormatter.format("sudo mv {}/{}/{} {}/{}",
            this.publishUploadPath,
            env,
            file.getName(),
            this.publishTargetPath,
            env));
        // 4. 重命名原来的部署包 (xxx-时间戳)
        sshAgent.execCommand("3. 创建备份", StrFormatter.format("sudo mv {}/{}/{} {}/{}/{}",
            this.publishTargetPath,
            env,
            this.packageName,
            this.publishTargetPath,
            env,
            this.packageName
                + "-"
                + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN)));

        // 解压
        sshAgent.execCommand("4. 解压部署包", StrFormatter.format("cd {}/{}; sudo unzip -d {} {} >/dev/null 2>&1",
            this.publishTargetPath,
            env,
            this.packageName,
            file.getName()));
        // 授权
        sshAgent.execCommand("5. 改用户组",
            StrFormatter.format("sudo chown -R zekastack:zekastack {}/{}/{}",
                this.publishTargetPath,
                env,
                this.packageName));

        log.info("\n{} 部署流程结束, 耗时: {} s\n", file.getName(), stopWatch.getTime(TimeUnit.SECONDS));

        stopWatch.stop();
    }

    /**
     * Check
     *
     * @return the file
     * @since 1.7.0
     */
    private File check() {
        // 部署包相对于 pom.xml 所在的路径
        String distPath = this.project.getProperties().getProperty("dist.path");
        Assert.notBlank(distPath, "未配置 dist 路径");

        String finalDistPath = FileUtils.appendPath(this.project.getBasedir().getAbsolutePath(), distPath);
        File distFile = new File(finalDistPath);

        Assert.isTrue(distFile.exists() || distFile.isDirectory(), "未找到待部署包或不是一个文件目录: {}", finalDistPath);

        return distFile;
    }
}
