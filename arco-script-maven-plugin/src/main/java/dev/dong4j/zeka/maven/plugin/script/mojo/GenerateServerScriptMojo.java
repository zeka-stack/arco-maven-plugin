package dev.dong4j.zeka.maven.plugin.script.mojo;

import dev.dong4j.zeka.maven.plugin.common.FileWriter;
import dev.dong4j.zeka.maven.plugin.common.Plugins;
import dev.dong4j.zeka.maven.plugin.common.ZekaMavenPluginAbstractMojo;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import lombok.SneakyThrows;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * <p>Description: package 打包时动态生成启动脚本并替换自定义参数 </p>
 *
 * @author dong4j
 * @version 1.5.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.07.02 17:29
 * @since 1.5.0
 */
@Mojo(name = "generate-server-script", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateServerScriptMojo extends ZekaMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_LAUNCH_SCRIPT, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;
    /** Output file */
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/bin/server.sh")
    private File outputFile;
    /** 自定义的脚本文件 */
    @Parameter(defaultValue = "${project.basedir}/bin/server.sh")
    private File scriptFile;
    /** jvm 参数 */
    @Parameter(property = "jvmOptions", defaultValue = "-Xms128M -Xmx256M ")
    private String jvmOptions;
    /** JVM_SYMBOL */
    private static final String JVM_SYMBOL = "#{jvmOptions}";
    /** SERVER_FILE */
    private static final String SERVER_FILE = "META-INF/bin/server.sh";

    /**
     * Execute *
     *
     * @since 1.0.0
     */
    @SneakyThrows
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("arco-script-maven-plugin is skipped");
            return;
        }

        // 存在自定义脚本则将自定义脚本写入到 outputFile
        if (this.scriptFile.exists()) {
            new FileWriter(this.outputFile).write(this.scriptFile);
            this.getLog().info("使用自定义 server.sh: " + this.scriptFile.getPath());
        } else {
            boolean isProd = Boolean.parseBoolean(System.getProperty("package.env.prod", "false"));
            String jvmProperties = this.project.getProperties().getProperty("jvm.options", this.jvmOptions);
            if (isProd) {
                jvmProperties = this.project.getProperties().getProperty("prod.jvm.options", this.jvmOptions);
                this.getLog().warn("生成生产环境启动脚本: jvmProperties: [" + jvmProperties + "]");
            } else {
                this.getLog().warn("生成非生产环境启动脚本, jvmProperties: ["
                    + jvmProperties
                    + "]. 生产环境打包请使用 [-Dpackage.env.prod=true] 并配置 [prod.jvm.options], 否则将使用 [jvm.options]");
            }
            Map<String, String> replaceMap = new HashMap<>(2);
            replaceMap.put(JVM_SYMBOL, jvmProperties);
            new FileWriter(this.outputFile, replaceMap).write(SERVER_FILE);
        }
        this.buildContext.refresh(this.outputFile);
    }
}
