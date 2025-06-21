package dev.dong4j.arco.maven.plugin.deploy.mojo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.StrFormatter;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import dev.dong4j.arco.maven.plugin.common.ZekaStackMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.deploy.mojo.entity.Group;
import dev.dong4j.arco.maven.plugin.deploy.mojo.entity.Server;
import dev.dong4j.arco.maven.plugin.deploy.mojo.util.SSHAgent;
import dev.dong4j.arco.maven.plugin.deploy.mojo.util.TimeoutUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * <p>Description: 发布后端服务 </p>
 *
 * @author dong4j
 * @version 1.1.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.25 17:10
 * @since 1.6.0
 */
@Slf4j
abstract class ServicePublishMojo extends ZekaStackMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_PUBLISH_BATCH, defaultValue = Plugins.TURN_ON_PLUGIN)
    public boolean skip;
    /** Groups */
    @Parameter
    public List<Group> groups;

    /** publishGroupId */
    protected String publishGroupId;
    /** 部署全局开关, 防止意外部署的情况, 需要添加 -Dpublish.switch=true 来开启部署 */
    protected Boolean publishSwitch;
    /** PUBLISH_ENABLE */
    protected Boolean enbalePublish;
    /** 单机部署时指定部署环境 */
    protected String environment;
    /** 是否开启 apm */
    protected Boolean enbaleApm;
    /** HOME_PATH */
    protected String publishUploadPath;
    /** PUBLISH_PATH */
    protected String publishTargetPath;
    /** USERNAME */
    protected String username;
    /** PASSWORD */
    private String password;
    /** ssh 端口 */
    private String port;
    /** 部署运行方式: 单线程, 并行 */
    protected String runningType;
    /** publicHostsDev */
    protected String publishHostsDev;
    /** publicHostsTest */
    protected String publishHostsTest;
    /** publicHostsprev */
    protected String publishHostsPrev;
    /** Publish namespace */
    protected String publishNamespace;
    /** javaHome */
    protected String javaHome;
    /** jvmOptions */
    protected static final String JVM_OPTIONS = "-Xms128M -Xmx256M";
    /** NO_ENVIRONMENT */
    protected static final String NO_ENVIRONMENT = "N/A";
    /** ENV_DEV */
    protected static final String ENV_DEV = "dev";
    /** ENV_TEST */
    protected static final String ENV_TEST = "test";
    /** ENV_PREV */
    protected static final String ENV_PREV = "prev";

    /**
     * Execute
     *
     * @since 1.7.0
     */
    @Override
    public void execute() {
        this.init();

        if (!Boolean.TRUE.equals(this.publishSwitch)) {
            this.getLog().warn("publish.switch=false 不执行部署流程, 如果需要自动部署请添加 Maven 参数: '-Dpublish.switch=true' 启用");
            return;
        }

        this.subExecute();
    }

    /**
     * Sub execute
     *
     * @since 1.7.0
     */
    protected abstract void subExecute();

    /**
     * Init
     *
     * @since 1.6.0
     */
    protected void init() {
        // 使用 -D 获取参数
        this.publishSwitch = Boolean.parseBoolean(System.getProperty("publish.switch", "false"));
        this.username = System.getProperty("publish.username");
        this.password = System.getProperty("publish.password");

        if (StringUtils.isAnyBlank(this.username, this.password)) {
            // todo-dong4j : (2025.06.21 17:08) [兼容免密/证书]]
            throw new IllegalArgumentException("请添加 Maven 参数: '-Dpublish.username=xxx -Dpublish.password=xxx'");
        }

        this.port = System.getProperty("publish.port", "22");

        this.environment = System.getProperty("publish.env", NO_ENVIRONMENT);

        this.enbaleApm = Boolean.parseBoolean(System.getProperty("apm.enable", "false"));

        // 写在 pom.xml 的 properties 标签中
        this.enbalePublish = Boolean.parseBoolean(this.project.getProperties().getProperty("publish.enable", "true"));
        // 默认上传目录
        this.publishUploadPath = this.project.getProperties().getProperty("publish.upload.path", "/home/" + this.username);
        // 默认部署目录
        this.publishTargetPath = this.project.getProperties().getProperty("publish.target.path", "/opt/apps");
        this.runningType = this.project.getProperties().getProperty("publish.runging.type", "single");

        this.publishGroupId = this.project.getProperties().getProperty("publish.group.id", "");
        this.publishHostsDev = this.project.getProperties().getProperty("publish.hosts.dev", "");
        this.publishHostsTest = this.project.getProperties().getProperty("publish.hosts.test", "");
        this.publishHostsPrev = this.project.getProperties().getProperty("publish.hosts.prev", "");
        this.publishNamespace = System.getProperty("publish.name.space",
            this.project.getProperties().getProperty("publish.name.space", "zeke-stack"));

        this.javaHome = this.project.getProperties().getProperty("publish.java.home",
            System.getProperty("PUBLISH.JAVA_HOME", "/opt/jdk1.8.0_211/bin"));
    }

    /**
     * 如果指定了 -Dpublish.env=xxx, 则只部署 xxx 环境的服务
     *
     * @since 1.7.0
     */
    protected void processor() {
        if (NO_ENVIRONMENT.equals(this.environment)) {
            this.buildGroup(this.publishHostsDev, ENV_DEV);
            this.buildGroup(this.publishHostsTest, ENV_TEST);
            this.buildGroup(this.publishHostsPrev, ENV_PREV);
        } else {
            this.buildGroup(this.project.getProperties().getProperty("publish.hosts." + this.environment, ""),
                this.environment);
        }
    }

    /**
     * Build group
     *
     * @param hosts hosts
     * @param env   env
     * @since 1.9.0
     */
    protected void buildGroup(String hosts, String env) {
    }

    /**
     * Publish
     *
     * @param groups groups
     * @since 1.6.0
     */
    protected void publish(@NotNull List<Group> groups) {

        Map<String, File> map = new HashMap<>(8);

        Iterator<File> files = FileUtils.iterateFiles(this.targetFile, new String[]{"tar.gz"}, true);

        files.forEachRemaining(f -> {
            String name = f.getName().replace(".tar.gz", "");
            map.put(name, f);
            log.info("{} -> {}", name, f);
        });

        this.publish(groups, map);
    }

    /**
     * Publish
     *
     * @param groups groups
     * @param map    map
     * @since 1.7.3
     */
    protected void publish(@NotNull List<Group> groups, Map<String, File> map) {
        groups.stream()
            .peek(group -> {
                if (!group.isEnable()) {
                    log.warn("{}: group.enable = false, 忽略部署", group.getEnv());
                }
            })
            .filter(Group::isEnable)
            .forEach(g -> {
                String env = StringUtils.isBlank(g.getEnv()) ? "dev" : g.getEnv();
                log.info("1. 部署环境: {}", env);
                Set<Server> servers = g.getServers();
                log.info("2. 待发布服务: {}", servers);

                servers.stream()
                    .peek(server -> {
                        if (CollectionUtil.isEmpty(server.getNames())) {
                            log.warn("{}: 未配置待部署服务", server.getHost());
                        }
                    })
                    .filter(server -> CollectionUtil.isNotEmpty(server.getNames()))
                    .forEach(server -> {
                        SSHAgent sshAgent = this.buildAgent(server.getHost());
                        Stream<String> serverStream = server.getNames().stream();
                        if (this.runningType.equals("parallel")) {
                            serverStream = server.getNames().parallelStream();
                        }
                        serverStream
                            .filter(name -> map.get(name) != null)
                            .peek(name -> log.info("3. 开始执行部署流程: {}: {} -> {}",
                                Thread.currentThread().getName(),
                                name,
                                server.getHost()))
                            .forEach(name -> this.uploadAndPublish(sshAgent,
                                name,
                                map.get(name),
                                this.publishGroupId,
                                env));

                        sshAgent.close();
                    });

            });
    }

    /**
     * Build agent
     *
     * @param hostName host name
     * @return the ssh agent
     * @since 1.6.0
     */
    @SneakyThrows
    protected @NotNull SSHAgent buildAgent(String hostName) {
        SSHAgent sshAgent = new SSHAgent();
        sshAgent.initSession(hostName, this.username, this.password, this.port);
        return sshAgent;
    }

    /**
     * Upload and publish
     *
     * @param sshAgent        ssh 客户端
     * @param publishFileName 部署包名
     * @param file            待部署的服务压缩包
     * @param groupId         服务分组名
     * @param env             部署环境
     * @since 1.6.0
     */
    @SneakyThrows
    protected void uploadAndPublish(@NotNull SSHAgent sshAgent, String publishFileName, File file, String groupId, String env) {
        StopWatch stopWatch = StopWatch.createStarted();

        this.generalStep(sshAgent, publishFileName, file, groupId, env);

        this.betweenStep(sshAgent, publishFileName, file, groupId, env);

        stopWatch.stop();

        this.bottomStep(sshAgent, file, stopWatch, this.javaHome);

    }

    /**
     * General step
     *
     * @param sshAgent        ssh agent
     * @param publishFileName 待部署压缩包名(不含后置)
     * @param file            待部署的压缩包(不含时间戳)
     * @param groupId         项目所属模块
     * @param env             部署环境
     * @since 1.7.3
     */
    @SneakyThrows
    protected void generalStep(SSHAgent sshAgent, String publishFileName, File file, String groupId, String env) {
        if (StringUtils.isBlank(groupId)) {
            throw new IllegalArgumentException("publish.group.id 未配置");
        }
        sshAgent.execCommand("3.1 输出当前环境变量", "sudo -E -u zekastack echo $PATH");

        this.fixLogDirectoryPermission(sshAgent, env, publishFileName);
        // 上传到 /home/{user}/{env}/{group} 目录
        sshAgent.transferFile(file, this.publishUploadPath + "/" + env + "/" + groupId);
        // 创建部署目录
        sshAgent.execCommand("3.3. 创建部署目录", StrFormatter.format("sudo mkdir -p {}/{}/{}",
            this.publishTargetPath,
            env,
            groupId));
        // 删除原来的部署包
        sshAgent.execCommand("3.3.1 删除旧部署包", StrFormatter.format("sudo rm -rf {}/{}/{}/{}",
            this.publishTargetPath,
            env,
            groupId,
            file.getName()));
        // 重命名原来的部署包, 只考虑新的包(无时间戳)
        sshAgent.execCommand("3.3.2 创建备份", StrFormatter.format("cd {}/{}/{}; sudo tar -zcf {} {} --remove-files",
            this.publishTargetPath,
            env,
            groupId,
            publishFileName
                + "-"
                + DateUtil.format(new Date(), DatePattern.PURE_DATETIME_PATTERN) + ".tar.gz",
            publishFileName));

        // 移动到 /opt/apps/env/应用名 目录
        sshAgent.execCommand("3.4 移动部署包到部署目录", StrFormatter.format("sudo mv {}/{}/{}/{} {}/{}/{}",
            this.publishUploadPath,
            env,
            groupId,
            file.getName(),
            this.publishTargetPath,
            env,
            groupId));
    }

    /**
     * Between step
     *
     * @param sshAgent        ssh agent
     * @param publishFileName publish file name
     * @param file            file
     * @param groupId         group id
     * @param env             env
     * @throws IOException io exception
     * @since 1.7.3
     */
    protected void betweenStep(@NotNull SSHAgent sshAgent,
                               String publishFileName,
                               File file,
                               String groupId,
                               String env) throws IOException {
        // 解压
        sshAgent.execCommand("3.5 解压部署包", StrFormatter.format("cd {}/{}/{}; sudo tar -zxf {}",
            this.publishTargetPath,
            env,
            groupId,
            file.getName()));
        // 授权
        sshAgent.execCommand("3.6 修改用户组",
            StrFormatter.format("sudo chown -R zekastack:zekastack {}/{}/{}/*",
                this.publishTargetPath,
                env, groupId));

        // 重启, 5 秒超时抛出 IOException, 然后使用 kill -9
        try {
            TimeoutUtil.process((Callable<Void>) () -> {
                this.runServerShell(sshAgent, groupId, env, publishFileName);
                return null;
            }, 5);

        } catch (Exception e) {

            sshAgent.execCommand("重启命令超时, 执行 kill -9",
                StrFormatter.format("sudo kill -9 `ps -ef | grep -v grep | grep {}@{} | awk '{print $2}'`",
                    publishFileName,
                    env));

            this.fixLogDirectoryPermission(sshAgent, env, publishFileName);
            this.runServerShell(sshAgent, groupId, env, publishFileName);
        }
    }

    /**
     * Bottom step
     *
     * @param sshAgent  ssh agent
     * @param file      file
     * @param stopWatch stop watch
     * @param javaHome  java home
     * @since 1.7.3
     */
    @SneakyThrows
    protected void bottomStep(SSHAgent sshAgent, File file, StopWatch stopWatch, String javaHome) {
        log.info("\n{} 部署流程结束, 耗时: {} s\n", file.getName(), stopWatch.getTime(TimeUnit.SECONDS));

        sshAgent.execCommand("已启动的服务: ",
            StrFormatter.format("sudo {}/jps -l", javaHome));
    }

    /**
     * 执行 server.sh 脚本
     *
     * @param sshAgent        ssh agent
     * @param groupId         group id
     * @param env             env
     * @param publishFileName publish file name
     * @throws IOException io exception
     * @since 1.7.0
     */
    protected void runServerShell(@NotNull SSHAgent sshAgent,
                                  String groupId,
                                  String env,
                                  String publishFileName) throws IOException {
        String cmd = "cd {}/{}/{}/{}; sudo -E -u zekastack bin/server.sh -r {} -n {} &";
        if (Boolean.TRUE.equals(this.enbaleApm)) {
            cmd = "cd {}/{}/{}/{}; sudo -E -u zekastack bin/server.sh -r {} -w -n {} &";
        }

        sshAgent.execCommand("执行部署脚本",
            StrFormatter.format(cmd,
                this.publishTargetPath,
                env,
                groupId,
                publishFileName,
                env,
                this.publishNamespace));
    }

    /**
     * 修复日志目录权限
     *
     * @param sshAgent ssh agent
     * @param env      env
     * @throws IOException io exception
     * @since 1.7.0
     */
    protected void fixLogDirectoryPermission(@NotNull SSHAgent sshAgent, String env, String publishFileName) throws IOException {
        sshAgent.execCommand("修复日志目录权限", StrFormatter.format(
            "sudo chown -R zekastack:zekastack /mnt/syslogs/zeka.stack/{}/{}/*.log",
            env,
            publishFileName));
    }
}
