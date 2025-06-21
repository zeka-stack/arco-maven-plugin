package dev.dong4j.arco.maven.plugin.deploy.mojo.util;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;

/**
 * <p>Description: https://www.jianshu.com/p/513c72dfee1b </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 21:41
 * @since 1.6.0
 */
@SuppressWarnings("all")
public class RemoteShellExecutor {

    /** Conn */
    private Connection conn;
    /** 远程机器IP */
    private final String ip;
    /** 用户名 */
    private final String osUsername;
    /** 密码 */
    private final String password;
    /** Charset */
    private final String charset = Charset.defaultCharset().toString();

    /** TIME_OUT */
    private static final int TIME_OUT = 1000 * 5 * 60;

    /**
     * Remote shell executor
     *
     * @param ip      ip
     * @param usr     usr
     * @param pasword pasword
     * @since 1.6.0
     */
    public RemoteShellExecutor(String ip, String usr, String pasword) {
        this.ip = ip;
        this.osUsername = usr;
        this.password = pasword;
    }

    /**
     * 登录
     *
     * @return boolean boolean
     * @throws IOException io exception
     * @since 1.6.0
     */
    private boolean login() throws IOException {
        this.conn = new Connection(this.ip);
        this.conn.connect();
        return this.conn.authenticateWithPassword(this.osUsername, this.password);
    }

    /**
     * 执行脚本
     *
     * @param cmds cmds
     * @return int int
     * @throws Exception exception
     * @since 1.6.0
     */
    public int exec(String cmds) throws Exception {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {
            if (this.login()) {
                // Open a new {@link Session} on this connection
                Session session = this.conn.openSession();
                // Execute a command on the remote machine.
                session.execCommand(cmds);
                stdOut = new StreamGobbler(session.getStdout());
                outStr = this.processStream(stdOut, this.charset);

                stdErr = new StreamGobbler(session.getStderr());
                outErr = this.processStream(stdErr, this.charset);

                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);

                System.out.println("outStr=" + outStr);
                System.out.println("outErr=" + outErr);

                ret = session.getExitStatus();
            } else {
                throw new Exception("登录远程机器失败" + this.ip); // 自定义异常类 实现略
            }
        } finally {
            if (this.conn != null) {
                this.conn.close();
            }
            IOUtils.closeQuietly(stdOut);
            IOUtils.closeQuietly(stdErr);
        }
        return ret;
    }

    /**
     * 执行脚本
     *
     * @param cmds cmds
     * @return int int
     * @throws Exception exception
     * @since 1.6.0
     */
    public int exec2(String cmds) throws Exception {
        InputStream stdOut = null;
        InputStream stdErr = null;
        String outStr = "";
        String outErr = "";
        int ret = -1;
        try {
            if (this.login()) {
                Session session = this.conn.openSession();
                // 建立虚拟终端
                session.requestPTY("bash");
                // 打开一个Shell
                session.startShell();
                stdOut = new StreamGobbler(session.getStdout());
                stdErr = new StreamGobbler(session.getStderr());
                BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(stdOut));
                BufferedReader stderrReader = new BufferedReader(new InputStreamReader(stdErr));

                // 准备输入命令
                PrintWriter out = new PrintWriter(session.getStdin());
                // 输入待执行命令
                out.println(cmds);
                out.println("exit");
                // 6. 关闭输入流
                out.close();
                // 7. 等待, 除非1.连接关闭；2.输出数据传送完毕；3.进程状态为退出；4.超时
                session.waitForCondition(ChannelCondition.CLOSED | ChannelCondition.EOF | ChannelCondition.EXIT_STATUS, 3000);
                System.out.println("Here is the output from stdout:");
                while (true) {
                    String line = stdoutReader.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                System.out.println("Here is the output from stderr:");
                while (true) {
                    String line = stderrReader.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                /* Show exit status, if available (otherwise "null") */
                System.out.println("ExitCode: " + session.getExitStatus());
                ret = session.getExitStatus();
                session.close();/* Close this session */
                this.conn.close();/* Close the connection */

            } else {
                throw new Exception("登录远程机器失败" + this.ip); // 自定义异常类 实现略
            }
        } finally {
            if (this.conn != null) {
                this.conn.close();
            }
            IOUtils.closeQuietly(stdOut);
            IOUtils.closeQuietly(stdErr);
        }
        return ret;
    }

    /**
     * Process stream
     *
     * @param in      in
     * @param charset charset
     * @return the string
     * @throws Exception exception
     * @since 1.6.0
     */
    private String processStream(InputStream in, String charset) throws Exception {
        byte[] buf = new byte[1024];
        StringBuilder sb = new StringBuilder();
        while (in.read(buf) != -1) {
            sb.append(new String(buf, charset));
        }
        return sb.toString();
    }

    /**
     * Process std err
     *
     * @param in      in
     * @param charset charset
     * @return the string
     * @throws IOException io exception
     * @since 1.6.0
     */
    private String processStdErr(InputStream in, String charset)
        throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in, charset));
        StringBuilder sb = new StringBuilder();
        if (in.available() != 0) {
            while (true) {
                String line = br.readLine();
                if (line == null) {
                    break;
                }
                sb.append(line).append(System.getProperty("line.separator"));
            }
        }
        return sb.toString();
    }

}
