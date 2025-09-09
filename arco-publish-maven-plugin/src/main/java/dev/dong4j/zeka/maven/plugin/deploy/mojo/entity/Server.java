package dev.dong4j.zeka.maven.plugin.deploy.mojo.entity;

import java.util.List;
import java.util.Objects;
import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * 服务器配置实体类
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 14:56
 * @since 1.0.0
 */
@Data
public class Server {

    /** 服务器主机地址 */
    @Parameter
    private String host;
    /** 服务器名称列表 */
    @Parameter
    private List<String> names;

    /**
     * 判断两个服务器对象是否相等，根据主机地址比较
     *
     * @param o 待比较的对象
     * @return 相等返回 true，否则返回 false
     * @since 1.0.0
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || this.getClass() != o.getClass()) {
            return false;
        }
        Server server = (Server) o;
        return Objects.equals(this.host, server.host);
    }

    /**
     * 获取服务器对象的哈希码，基于主机地址计算
     *
     * @return 哈希码值
     * @since 1.0.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.host);
    }
}
