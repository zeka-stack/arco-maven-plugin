package dev.dong4j.zeka.maven.plugin.deploy.mojo.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPInputStream;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * SSH 连接代理工具类，提供远程服务器文件传输和命令执行功能
 * <p>
 * 该类基于 Ganymed SSH-2 库实现，提供了完整的 SSH 连接管理、文件传输和远程命令执行能力。
 * 支持密码认证方式，适用于 Maven 插件中的远程部署和文件操作场景。
 * <p>
 * 使用前提条件：
 * <ul>
 *     <li>目标 Linux 机器已安装并启用 SSH 服务</li>
 *     <li>SSH 配置文件 /etc/ssh/sshd_config 中设置 PasswordAuthentication yes</li>
 *     <li>确保防火墙允许 SSH 端口访问</li>
 * </ul>
 * <p>
 * 主要功能特性：
 * <ul>
 *     <li>SSH 连接建立和身份认证</li>
 *     <li>远程命令执行和结果获取</li>
 *     <li>单文件和目录的批量传输</li>
 *     <li>文件下载和内容读取</li>
 *     <li>连接状态管理和资源释放</li>
 * </ul>
 * <p>
 * 参考文档：
 * <ul>
 *     <li><a href="http://www.ganymed.ethz.ch/ssh2/FAQ.html">Ganymed SSH-2 FAQ</a></li>
 *     <li><a href="http://www.programcreek.com/java-api-examples/index.php?api=ch.ethz.ssh2.StreamGobbler">StreamGobbler 使用示例</a></li>
 *     <li><a href="http://www.programcreek.com/java-api-examples/index.php?api=ch.ethz.ssh2.SCPClient">SCPClient 使用示例</a></li>
 * </ul>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 21:40
 * @since 1.0.0
 */
@SuppressWarnings("all")
public final class SSHAgent {

    /** 日志记录器 */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /** 远程主机名或 IP 地址 */
    private String hostName;
    /** SSH 连接对象 */
    private Connection connection;

    /**
     * 初始化 SSH 会话连接并进行身份认证
     * <p>
     * 该方法创建到指定远程主机的 SSH 连接，并使用提供的用户名和密码进行身份验证。
     * 认证成功后会打开一个测试会话并立即关闭，以验证连接的有效性。
     * 如果连接或认证失败，会抛出运行时异常。
     *
     * @param hostName 远程主机名或 IP 地址
     * @param userName SSH 登录用户名
     * @param passwd   SSH 登录密码
     * @param port     SSH 服务端口号（字符串格式）
     * @throws IOException 连接或认证失败时抛出
     * @since 1.0.0
     */
    public void initSession(String hostName, String userName, String passwd, String port) throws IOException {
        int portNumber = 0;
        try {
            portNumber = Integer.parseInt(port);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("不是有效的端口号: port=" + port);
        }
        this.hostName = hostName;
        this.connection = new Connection(hostName, portNumber);
        this.connection.connect();

        try {
            this.connection.authenticateWithPassword(userName, passwd);
            this.connection.openSession().close();
        } catch (Exception e) {
            throw new RuntimeException("服务器认证失败, 可使用 '-Dpublish.username' 和 '-Dpublish.password' 设置账号和密码");
        }
    }

    /**
     * 执行远程命令并显示执行结果
     * <p>
     * 该方法是 execCommand 的简化版本，默认显示命令执行结果。
     * 对于单个 SSH 会话只能执行一个命令的限制，可以通过以下方式解决：
     * 1. 使用 && 连接多个命令："echo Hello && echo again"
     * 2. 为每个命令开启新的会话（推荐做法）
     * 3. 使用 Session.startShell() 获取交互式 shell
     *
     * @param explain 命令说明信息，用于日志记录
     * @param command 要执行的 shell 命令
     * @throws IOException 命令执行失败时抛出
     * @since 1.0.0
     */
    public void execCommand(String explain, String command) throws IOException {
        this.execCommand(explain, command, true);
    }

    /**
     * 执行远程命令并可选是否显示结果
     *
     * @param explain    命令说明信息
     * @param command    要执行的 shell 命令
     * @param showResult 是否显示命令执行结果
     * @throws IOException 命令执行失败时抛出
     * @since 1.0.0
     */
    public void execCommand(String explain, String command, boolean showResult) throws IOException {
        this.execCommand(explain, command, 60 * 1000, showResult);
    }

    /**
     * 执行远程命令并设置超时时间和结果显示选项
     * <p>
     * 该方法提供了最完整的命令执行控制，包括超时设置和结果显示控制。
     * 使用 UTF-8 编码执行命令，确保中文字符的正确处理。
     * 当显示结果时，会等待命令执行完成并输出所有结果和退出状态。
     *
     * @param explain    命令说明信息，用于日志记录
     * @param command    要执行的 shell 命令
     * @param timeout    命令执行超时时间（毫秒）
     * @param showResult 是否输出命令执行结果和状态
     * @throws IOException 命令执行或网络通信失败时抛出
     * @since 1.0.0
     */
    public void execCommand(String explain, String command, long timeout, boolean showResult) throws IOException {
        Session session = this.connection.openSession();
        this.log.info("[{}]: [{}]", explain, command);
        session.execCommand(command, StandardCharsets.UTF_8.toString());

        if (showResult) {
            session.waitForCondition(ChannelCondition.TIMEOUT, timeout);

            InputStream streamGobbler = new StreamGobbler(session.getStdout());
            String result = IOUtils.toString(streamGobbler, StandardCharsets.UTF_8);
            this.log.info("-------------- 执行结果 -------------- \n[{}]\nstatus: [{}]",
                StringUtils.isBlank(result) ? "" : "\n" + result,
                session.getExitStatus());
            IOUtils.closeQuietly(streamGobbler, e -> this.log.error(e.getMessage()));
        }

        session.close();
    }

    /**
     * 传输单个文件到远程服务器
     * <p>
     * 该方法实现安全的文件传输功能，包括以下步骤：
     * 1. 验证源文件是否为有效的文件（非目录）
     * 2. 在远程服务器上创建目标目录
     * 3. 删除同名文件并创建新文件
     * 4. 使用 SCP 协议传输文件内容
     * 5. 记录传输耗时和结果
     *
     * @param file                  要传输的本地文件对象
     * @param remoteTargetDirectory 远程目标目录路径
     * @throws IOException 文件传输失败时抛出
     * @since 1.0.0
     */
    public void transferFile(@NotNull File file, String remoteTargetDirectory) throws IOException {
        if (file.isDirectory()) {
            throw new RuntimeException(file + "  is not a file");
        }
        String fileName = file.getName();
        this.execCommand("创建远程文件",
            "mkdir -p " + remoteTargetDirectory
                + "; cd " + remoteTargetDirectory
                + "; rm " + fileName
                + "; touch " + fileName);

        SCPClient sCPClient = this.connection.createSCPClient();
        SCPOutputStream scpOutputStream = sCPClient.put(fileName, file.length(), remoteTargetDirectory, "7777");

        StopWatch stopWatch = StopWatch.createStarted();
        FileUtils.copyFile(file, scpOutputStream);
        stopWatch.stop();

        this.log.info("[{}] 上传完成, 耗时: [{}] s", file.getName(), stopWatch.getTime(TimeUnit.SECONDS));
        scpOutputStream.close();
    }

    /**
     * 传输指定路径的文件到远程服务器
     *
     * @param localFile             本地文件路径字符串
     * @param remoteTargetDirectory 远程目标目录路径
     * @throws IOException 文件传输失败时抛出
     * @since 1.0.0
     */
    public void transferFile(String localFile, String remoteTargetDirectory) throws IOException {
        this.transferFile(new File(localFile), remoteTargetDirectory);
    }

    /**
     * 递归传输整个目录及其子目录到远程服务器
     * <p>
     * 该方法实现整个目录结构的递归传输，包括以下特性：
     * 1. 验证本地路径是否为有效目录
     * 2. 过滤以点号开头的隐藏文件
     * 3. 递归处理子目录和文件
     * 4. 在远程服务器上创建相应的目录结构
     * 5. 逐个传输所有文件
     *
     * @param localDirectory        本地目录路径
     * @param remoteTargetDirectory 远程目标目录路径
     * @throws IOException 目录传输过程中出现错误时抛出
     * @since 1.0.0
     */
    public void transferDirectory(String localDirectory, String remoteTargetDirectory) throws IOException {
        File dir = new File(localDirectory);
        if (!dir.isDirectory()) {
            throw new RuntimeException(localDirectory + " is not directory");
        }

        String[] files = dir.list();
        for (String file : files) {
            if (file.startsWith(".")) {
                continue;
            }
            String fullName = localDirectory + "/" + file;
            if (new File(fullName).isDirectory()) {
                String rdir = remoteTargetDirectory + "/" + file;
                this.execCommand("创建远程文件", "mkdir -p " + remoteTargetDirectory + "/" + file);
                this.transferDirectory(fullName, rdir);
            } else {
                this.transferFile(fullName, remoteTargetDirectory);
            }
        }

    }

    /**
     * 从远程服务器获取文件内容并输出到日志
     *
     * @param fileName 远程文件路径
     * @since 1.0.0
     */
    @SneakyThrows
    public void getFile(String fileName) {
        SCPClient scpClient = this.connection.createSCPClient();
        SCPInputStream scpInputStream = scpClient.get(fileName);

        String result = IOUtils.toString(scpInputStream, StandardCharsets.UTF_8);
        this.log.info("-------------- 执行结果 -------------- \n[{}]", result);

        IOUtils.closeQuietly(scpInputStream, e -> this.log.error(e.getMessage()));
    }

    /**
     * 关闭 SSH 连接并释放资源
     *
     * @since 1.0.0
     */
    public void close() {
        this.connection.close();
    }

    /**
     * 比较两个 SSHAgent 对象是否相等，根据主机名判断
     *
     * @param o 待比较的对象
     * @return 相等返回 true，否则返回 false
     * @since 1.0.0
     */
    @Contract(value = "null -> false", pure = true)
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        SSHAgent sshAgent = (SSHAgent) o;
        return Objects.equals(this.hostName, sshAgent.hostName);
    }

    /**
     * 获取 SSHAgent 对象的哈希码，基于主机名计算
     *
     * @return 哈希码值
     * @since 1.0.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.hostName);
    }
}
