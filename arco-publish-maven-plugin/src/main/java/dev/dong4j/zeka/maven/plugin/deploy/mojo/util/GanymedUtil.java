package dev.dong4j.zeka.maven.plugin.deploy.mojo.util;

import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;


/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 21:41
 * @since 1.6.0
 */
@Slf4j
@UtilityClass
public class GanymedUtil {

    /**
     * Login
     *
     * @param ip       ip
     * @param port     port
     * @param username username
     * @param password password
     * @return the connection
     * @since 1.6.0
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
     * 远程执行shell脚本或者命令
     *
     * @param connection connection
     * @param command    即将执行的命令
     * @return 命令执行完后返回的结果值 string
     * @since 1.6.0
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
     * 解析脚本执行返回的结果集
     *
     * @param in 输入流对象
     * @return 以纯文本的格式返回 string
     * @since 1.6.0
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
     * Ganymed exec command
     *
     * @param host     host
     * @param port     port
     * @param username username
     * @param password password
     * @param command  command
     * @return the string
     * @since 1.6.0
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
