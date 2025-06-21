package dev.dong4j.zeka.maven.plugin.deploy.mojo.entity;

import lombok.Data;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.List;
import java.util.Objects;

/**
 * <p>Description:  </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.10.20 14:56
 * @since 1.6.0
 */
@Data
public class Server {

    /** Host */
    @Parameter
    private String host;
    /** Name */
    @Parameter
    private List<String> names;

    /**
     * Equals
     *
     * @param o o
     * @return the boolean
     * @since 1.6.0
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
     * Hash code
     *
     * @return the int
     * @since 1.6.0
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.host);
    }
}
