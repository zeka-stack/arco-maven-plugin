package dev.dong4j.zeka.maven.plugin.deploy.mojo.util;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;


/**
 * SSH 连接工具类，基于 Ganymed SSH-2 库实现远程服务器连接和命令执行
 * <p>
 * 该工具类封装了 SSH 连接的常用操作，包括用户名密码认证、远程命令执行、
 * 输出流解析等功能。提供了简化的 API 接口，便于在 Maven 插件中进行
 * 远程服务器操作和应用部署。
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 21:41
 * @since 1.0.0
 */
@Slf4j
@UtilityClass
public class GanymedUtil {

    /**
     * 建立 SSH 连接并进行用户名密码认证
     * <p>
     * 该方法创建到指定服务器的 SSH 连接，使用提供的用户名和密码进行身份认证。
     * 如果连接或认证失败，会记录错误日志并终止程序执行。认证成功后返回
     * 可用的连接对象供后续操作使用。
     *
     * @param ip       服务器 IP 地址
     * @param port     SSH 端口号，通常为 22
     * @param username 登录用户名
     * @param password 登录密码
     * @return SSH 连接对象，认证成功时返回有效连接
     * @since 1.0.0
     */
    private static Connection login(String ip, int port, String username, String password) {
        boolean flag;
        Connection connection = null;
        try {
            connection = new Connection(ip, port);
            connection.connect();
            flag = connection.authenticateWithPassword(username, password);
            if (flag) {
                log.info("================登录成功==================");
                return connection;
            }
        } catch (IOException e) {
            log.error("登录失败,请检查IP或端口是否有误: " + e);
            connection.close();
            System.exit(-1);
        }
        return connection;
    }

    /**
     * 在远程服务器上执行 shell 命令并获取执行结果
     * <p>
     * 该方法通过已建立的 SSH 连接在远程服务器上执行指定的 shell 命令，
     * 并读取命令的标准输出。如果执行过程中出现错误或返回结果为空，
     * 会记录相应的错误信息并终止程序。执行完成后自动关闭连接和会话。
     *
     * @param connection SSH 连接对象
     * @param command    要执行的 shell 命令
     * @return 命令执行的标准输出结果
     * @since 1.0.0
     */
    private static String execCommand(Connection connection, String command) {
        String result = "";
        if (connection != null) {
            Session session = null;
            try {
                session = connection.openSession();
            } catch (IOException ise) {
                log.error("请检查用户名或密码是否有误");
                System.exit(-1);
            }
            try {
                session.execCommand(command);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
            result = processStdout(session.getStdout());
            if ("".equals(result)) {
                log.error("请检查脚本内容是否有误");
                System.exit(1);
            }
            connection.close();
            session.close();
        }
        return result;
    }

    /**
     * 解析命令执行输出流并转换为文本结果
     * <p>
     * 该方法读取命令执行的标准输出流，使用 UTF-8 编码将二进制数据转换为
     * 可读的文本格式。使用 StreamGobbler 包装输入流以确保正确处理输出，
     * 逐行读取并拼接成完整的结果字符串。
     *
     * @param in 命令执行的标准输出流
     * @return 格式化后的文本结果，每行以换行符分隔
     * @since 1.0.0
     */
    private static String processStdout(InputStream in) {
        InputStream stdout = new StreamGobbler(in);
        StringBuilder buffer = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(stdout, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                buffer.append(line).append("\n");
            }
            br.close();
        } catch (IOException e) {
            log.error("解析脚本出错: " + e.getMessage());
        }
        return buffer.toString();
    }

    /**
     * 执行远程 SSH 命令的完整流程，包含连接建立、命令执行和结果判断
     * <p>
     * 该方法是工具类的主要入口方法，封装了完整的 SSH 操作流程：
     * 1. 建立到目标服务器的 SSH 连接并进行身份认证
     * 2. 执行指定的 shell 命令并获取输出结果
     * 3. 根据输出内容判断命令执行状态（成功/失败）
     * <p>
     * 该方法主要用于应用部署场景，通过检查输出中是否包含 "successfully"
     * 关键字来判断部署操作的执行状态。
     *
     * @param host     目标服务器主机地址
     * @param port     SSH 连接端口
     * @param username SSH 登录用户名
     * @param password SSH 登录密码
     * @param command  要执行的 shell 命令
     * @return 执行状态描述，"安装成功" 或 "安装失败"
     * @since 1.0.0
     */
    public static String ganymedExecCommand(String host, int port, String username, String password, String command) {

        Connection connection = login(host, port, username, password);
        String execCommand = execCommand(connection, command);
        if (execCommand.contains("successfully")) {
            return "安装成功";
        } else {
            return "安装失败";
        }
    }

}
