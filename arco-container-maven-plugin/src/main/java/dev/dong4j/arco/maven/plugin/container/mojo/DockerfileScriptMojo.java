package dev.dong4j.arco.maven.plugin.container.mojo;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.yaml.YamlUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.FileWriter;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * dockerfile 生成器
 * 1. docker/Dockerfile 是模板文件, 如果不存在自定义 dockerfile, 则会使用此模板;
 * 2. 插件打包时需要将 docker/Dockerfile 写入到 {@link DockerfileScriptMojo#DOCKERFILE};
 * 3. 插件打包时使用 assembly/assembly.xml, 需要配置 Dockerfile 写入 jar 包的规则;
 * 4. 业务模块存在启动类时才会打包, {@link dev.dong4j.arco.maven.plugin.helper.mojo.SkipPluginMojo#execute()};
 * 5. 业务模块如果不存在自定义 dockerfile, 将会把插件包 docker/Dockerfile 复制到业务模块的 target/Dockerfile;
 * 6. Jenkins 使用 target/Dockerfile 创建镜像
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2024.02.04 18:26
 * @since x.x.x
 */
@Mojo(name = "generate-dockerfile", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class DockerfileScriptMojo extends ArcoMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_DOCKERFILE_SCRIPT, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;
    /** 最终文件输出到 target 目录下, 和 tar.gz 同级 */
    @Parameter(defaultValue = "${project.build.directory}/Dockerfile")
    private File outputFile;
    /** 自定义的脚本文件 */
    @Parameter(defaultValue = "${project.basedir}/Dockerfile")
    private File scriptFile;

    /** 包名 */
    @Parameter(defaultValue = "${project.build.finalName}")
    private String packageName;

    /** 如果没有自定义配置, 则从插件包中获取模板文件 */
    private static final String DOCKERFILE = "META-INF/docker/Dockerfile";

    /** PACKAGE_NAME */
    private static final String PACKAGE_NAME = "${PACKAGE.NAME}";
    /** EXPORT_PORT */
    private static final String EXPORT_PORT = "${EXPORT.PORT}";
    private static final String HEALTHCHECK = "${HEALTHCHECK}";

    /** 主配置文件 */
    @Parameter(defaultValue = "${project.basedir}/src/main/resources/application.yml")
    private File mainConfigFile;

    /** objectMapper */
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Execute *
     *
     * @since 2024.1.1
     */
    @SneakyThrows
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("build dockerfile is skipped");
            return;
        }

        // 存在自定义脚本则将自定义脚本写入到 outputFile
        if (this.scriptFile.exists()) {
            new FileWriter(this.outputFile).write(this.scriptFile);
            this.getLog().info("使用自定义 Dockerfile: " + this.scriptFile.getPath());
        } else {
            Map<String, String> replaceMap = Maps.newHashMapWithExpectedSize(2);

            writePort(replaceMap);

            replaceMap.put(PACKAGE_NAME, packageName);
            new FileWriter(this.outputFile, replaceMap).write(DOCKERFILE);
        }
        this.buildContext.refresh(this.outputFile);

        this.getLog().info("生成 Dockerfile: " + this.outputFile.getPath());
    }

    /**
     * 从项目的 application.yml 文件中读取 zeka-stack.docker.export 配置并写入到 map
     *
     * @param replaceMap replace map
     * @since 2024.2.0
     */
    private void writePort(Map<String, String> replaceMap) {
        if (mainConfigFile == null) {
            this.getLog().error("application.yml 文件不存在, 无法确认 export port");
        } else {
            final BufferedReader reader = FileUtil.getReader(mainConfigFile, StandardCharsets.UTF_8);
            final Object config = YamlUtil.load(reader, Object.class);
            if (config == null) {
                this.getLog().info("application.yml 未配置 zeka-stack.docker.export, 无法确认 export port");
                replaceMap.put(EXPORT_PORT, "");
            } else {
                try {
                    final String json = objectMapper.writeValueAsString(config);
                    final JsonNode jsonNode = objectMapper.readTree(json);
                    final JsonNode path = jsonNode.findPath("zeka-stack").findPath("docker").findPath("export");

                    List<String> ports = new ArrayList<>();
                    if (path.isArray()) {
                        path.elements().forEachRemaining(node -> {
                            ports.add(StrUtil.trim(node.toString()));
                        });
                    }

                    if (CollectionUtil.isNotEmpty(ports)) {
                        replaceMap.put(EXPORT_PORT, "EXPOSE " + String.join(" ", ports));
                        // 使用第一个端口作为检查检查端口
                        final String firstPort = ports.get(0);
                        replaceMap.put(HEALTHCHECK,
                            StrFormatter.format("HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost:{}/actuator/health || exit 1", firstPort));
                    } else {
                        // 默认暴露 8080 端口
                        replaceMap.put(EXPORT_PORT, "EXPOSE 8080");
                        replaceMap.put(HEALTHCHECK, "");
                    }

                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
