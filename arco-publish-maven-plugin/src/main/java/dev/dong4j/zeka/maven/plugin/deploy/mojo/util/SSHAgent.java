package dev.dong4j.zeka.maven.plugin.deploy.mojo.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.SCPClient;
import ch.ethz.ssh2.SCPInputStream;
import ch.ethz.ssh2.SCPOutputStream;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeUnit;


/**
 * <p>Description: </p>
 * 1.确保所连接linux机器安装ssh, 并且服务打开;
 * 2.密码登陆, 需配置文件:
 * ssh配置文件:  /ect/ssh/sshd_config
 * 配置项: PasswordAuthentication yes
 * <p>
 * 验证登陆成功否: ssh 127.0.0.1 (/other）
 * 'http://www.ganymed.ethz.ch/ssh2/FAQ.html'
 * 'http://www.programcreek.com/java-api-examples/index.php?api=ch.ethz.ssh2.StreamGobbler'
 * 'http://www.javawebdevelop.com/3240343/'
 * 'http://www.programcreek.com/java-api-examples/index.php?api=ch.ethz.ssh2.SCPClient'
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 21:40
 * @since 1.6.0
 */
@SuppressWarnings("all")
public final class SSHAgent {

    /** Log */
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    /** Host name */
    private String hostName;
    /** Connection */
    private Connection connection;

    /**
     * Init session
     *
     * @param hostName host name
     * @param userName user name
     * @param passwd   passwd
     * @param port     port
     * @throws IOException io exception
     * @since 1.6.0
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
     * Why can't I execute several commands in one single session?
     * <p>
     * If you use Session.execCommand(), then you indeed can only execute only one command per session. This is not a restriction of the
     * library, but rather an enforcement by the underlying SSH-2 protocol (a Session object models the underlying SSH-2 session).
     * <p>
     * There are several solutions:
     * <p>
     * Simple: Execute several commands in one batch, e.g., something like Session.execCommand("echo Hello && echo again").
     * Simple: The intended way: simply open a new session for each command - once you have opened a connection, you can ask for as many
     * sessions as you want, they are only a "virtual" construct.
     * Advanced: Don't use Session.execCommand(), but rather aquire a shell with Session.startShell().
     *
     * @param explain explain
     * @param command command
     * @return string
     * @throws IOException io exception
     * @since 1.6.0
     */
    public void execCommand(String explain, String command) throws IOException {
        this.execCommand(explain, command, true);
    }

    /**
     * Exec command
     *
     * @param explain    explain
     * @param command    command
     * @param showResult show result
     * @throws IOException io exception
     * @since 1.7.3
     */
    public void execCommand(String explain, String command, boolean showResult) throws IOException {
        this.execCommand(explain, command, 60 * 1000, showResult);
    }

    /**
     * Exec command
     *
     * @param explain    explain
     * @param command    command
     * @param timeout    timeout
     * @param showResult show result
     * @throws IOException io exception
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
     * 远程传输单个文件
     *
     * @param file                  file
     * @param remoteTargetDirectory remote target directory
     * @throws IOException io exception
     * @since 1.6.0
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
     * 远程传输单个文件
     *
     * @param localFile             local file
     * @param remoteTargetDirectory remote target directory
     * @throws IOException io exception
     * @since 1.6.0
     */
    public void transferFile(String localFile, String remoteTargetDirectory) throws IOException {
        this.transferFile(new File(localFile), remoteTargetDirectory);
    }

    /**
     * 传输整个目录
     *
     * @param localDirectory        local directory
     * @param remoteTargetDirectory remote target directory
     * @throws IOException io exception
     * @since 1.6.0
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
     * Gets file *
     *
     * @param fileName file name
     * @since 1.7.1
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
     * Close
     *
     * @since 1.6.0
     */
    public void close() {
        this.connection.close();
    }

    /**
     * Equals
     *
     * @param o o
     * @return the boolean
     * @since 1.6.0
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
     * Hash code
     *
     * @return the int
     * @since 1.6.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.hostName);
    }
}
