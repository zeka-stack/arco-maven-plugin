package dev.dong4j.zeka.maven.plugin.deploy.mojo;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ZipUtil;
import dev.dong4j.zeka.maven.plugin.common.util.FileUtils;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.entity.Group;
import dev.dong4j.zeka.maven.plugin.deploy.mojo.util.SSHAgent;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>Description: 发布项目文档服务
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021 -08-15 17:05
 * @since 1.9.0
 */
@Slf4j
@Mojo(name = "publish-doc", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class DocPublishMojo extends ServicePublishMojo implements PublishConfigAnalyse {
    /** 打包后的名称 */
    private String packageName;
    /** 部门名 */
    private String departmentName;
    /** 项目名 */
    private String projectName;
    /** dock 版本号 */
    @Parameter(defaultValue = "${project.version}")
    private String version;
    /** DEFAULT_TAG */
    private static final String DEFAULT_TAG = "doc";
    /** 不需要被打包的文件 */
    private static final String IGNORE_FILE = ".gitignore .flattened-pom.xml .DS_Store pom.xml publish.sh";

    /**
     * Execute
     *
     * @since 1.9.0
     */
    @Override
    public void subExecute() {
        // 重写默认上传目录
        this.publishUploadPath = this.project.getProperties()
            .getProperty("publish.upload.path",
                "/home/"
                    + System.getProperty("publish.username", "publisher")
                    + "/" + DEFAULT_TAG);

        this.departmentName = this.project.getProperties().getProperty("publish.department.name", "");

        // 知识库版本号, 删除 -SNAPSHOT 或 .release 后缀
        this.version = this.project.getProperties()
            .getProperty("publish.project.version", this.version)
            .replace("-SNAPSHOT", "")
            .replace(".release", "");

        Assert.notBlank(this.departmentName, "publish.department.name 必须配置");
        this.projectName = this.project.getProperties().getProperty("publish.project.name", "");
        Assert.notBlank(this.departmentName, "publish.project.name 必须配置");

        // 知识库默认部署目录为 /mnt/zeka-stack/wiki
        this.publishTargetPath = this.project.getProperties().getProperty("publish.target.path", "/mnt/zeka-stack/wiki");
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
     * 只处理 public.hosts.doc 配置, 默认部署到 192.168.2.5
     *
     * @since 1.9.0
     */
    @Override
    protected void processor() {
        this.buildGroup(this.project.getProperties().getProperty("publish.hosts.doc", "192.168.2.5"), DEFAULT_TAG);
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
     * Publish
     *
     * @param groups groups
     * @since 1.9.0
     */
    @Override
    protected void publish(@NotNull List<Group> groups) {
        File distFile = new File(this.project.getBasedir().getAbsolutePath());
        // 将 distFile 打包成 version.zip 压缩包, version 需要删除 -SNAPSHOT 后缀
        String zipPath = FileUtils.appendPath(distFile.getParent(), this.version + ".zip");

        // 忽略部分目录或文件, 不打包到 zip 中
        File srcFile = FileUtil.file(distFile.getPath());
        File zipFile = FileUtil.file(zipPath);
        File zip = ZipUtil.zip(zipFile,
            CharsetUtil.CHARSET_UTF_8,
            false,
            pathname -> (!pathname.isDirectory() || !pathname.getName().equals(".git"))
                && !IGNORE_FILE.contains(pathname.getName()),
            srcFile);


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

        // 部署完成后删除压缩包
        FileUtils.deleteFiles(zipPath);
    }

    /**
     * Upload and publish
     *
     * @param sshAgent        ssh 客户端
     * @param publishFileName 部署包名
     * @param file            待部署的服务压缩包
     * @param groupId         服务分组名
     * @param env             部署环境
     * @since 1.9.0
     */
    @SneakyThrows
    @Override
    protected void uploadAndPublish(@NotNull SSHAgent sshAgent, String publishFileName, File file, String groupId, String env) {

        StopWatch stopWatch = StopWatch.createStarted();

        String deployPath = StrFormatter.format("{}/{}/{}/{}",
            this.publishTargetPath,
            this.departmentName,
            this.projectName,
            this.version);
        // 删除原来的文件
        sshAgent.execCommand("1. 删除原来的文件", StrFormatter.format("{} {}",
            "sudo rm -rf",
            deployPath));

        // 创建部署目录
        sshAgent.execCommand("2. 创建部署目录", StrFormatter.format("{} {}",
            "sudo mkdir -p ",
            deployPath));

        // 上传到 /home/{user}/doc 目录
        sshAgent.transferFile(file, this.publishUploadPath);

        // 移动到 publishTargetPath 目录
        sshAgent.execCommand("3. 移动部署包到部署目录", StrFormatter.format("sudo mv {}/{} {}",
            this.publishUploadPath,
            file.getName(),
            deployPath));

        // 解压
        sshAgent.execCommand("4. 解压部署包", StrFormatter.format("cd {}; sudo unzip {} >/dev/null 2>&1",
            deployPath,
            file.getName()));

        // 授权
        sshAgent.execCommand("5. 改用户组",
            StrFormatter.format("sudo chown -R zekastack:zekastack {}", this.publishTargetPath));

        log.info("\n{} 部署流程结束, 耗时: {} s\n", file.getName(), stopWatch.getTime(TimeUnit.SECONDS));

        stopWatch.stop();
    }

}
