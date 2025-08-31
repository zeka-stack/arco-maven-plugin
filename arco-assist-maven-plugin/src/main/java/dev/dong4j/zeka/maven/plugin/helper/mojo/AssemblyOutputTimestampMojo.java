package dev.dong4j.zeka.maven.plugin.helper.mojo;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;


/**
 * Maven 插件 Mojo：为项目注入 outputTimestamp.project.version 属性
 *
 * <p>功能说明：
 * 1. 根据当前 Maven 项目的版本号 (project.version)，生成属性名 outputTimestamp.<版本号>
 * 2. 注入到 MavenProject 的 properties 中，使其在后续插件（如 maven-assembly-plugin）中可直接引用
 * 3. 默认时间戳为 1980-01-01T00:00:00Z，可通过 plugin 配置覆盖
 *
 * <p>使用场景：
 * - 可重现构建（Reproducible Build）：保证同一版本号的构建产物时间戳一致
 * - 与 maven-assembly-plugin 配合，设置 outputTimestamp 属性避免每次构建产物变化
 */
@Mojo(name = "assembly-outputTimestamp-property", defaultPhase = LifecyclePhase.INITIALIZE)
public class AssemblyOutputTimestampMojo extends AbstractMojo {

    /** 注入 MavenProject 对象，获取版本号等信息 */
    @SuppressWarnings("deprecation")
    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        String version = project.getVersion();
        // 属性名固定为 outputTimestamp.project.version
        String propertyName = "outputTimestamp.project.version";
        // 属性值为 outputTimestamp.<project.version>
        String propertyValue = "outputTimestamp." + version;

        // 注入到 MavenProject properties
        project.getProperties().setProperty(propertyName, propertyValue);
        getLog().info("Injected property: " + propertyName + "=" + propertyValue);
    }
}
