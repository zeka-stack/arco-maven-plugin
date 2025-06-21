package dev.dong4j.arco.maven.plugin.boot;

import dev.dong4j.arco.maven.plugin.boot.boost.BootSlotter;
import dev.dong4j.arco.maven.plugin.boot.boost.Slotter;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import dev.dong4j.arco.maven.plugin.common.ZekaStackMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.enums.ModuleType;
import dev.dong4j.arco.maven.plugin.common.util.PluginUtils;
import lombok.SneakyThrows;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;

/**
 * <p>Description: 将 jar 重新打包, 对 MANIFEST.MF 文件重写 Main-Class 和 Start-Class </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.29 23:32
 * @since 1.0.0
 */
@Mojo(name = "jar-repackage", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class LauncherJarRepackageMojo extends ZekaStackMavenPluginAbstractMojo {
    /** PATCH */
    public static final String PATCH = "patch";
    /** PLUGIN */
    public static final String PLUGIN = "plugin";
    /** 原本 JAR 所在文件夹 */
    @Parameter(property = "sourceDir", required = true, defaultValue = "${project.build.directory}")
    private File sourceDir;
    /** 原本 JAR 名称 */
    @Parameter(property = "sourceJar", required = true, defaultValue = "${project.build.finalName}.jar")
    private String sourceJar;
    /** Set this to 'true' to bypass artifact deploy */
    @Parameter(property = Plugins.SKIP_JAR_REPACKAGE, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;

    /**
     * Execute *
     *
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {

        ModuleType moduleType = PluginUtils.moduleType();

        if (this.skip || !moduleType.equals(ModuleType.DELOPY)) {
            this.getLog().info("arco-boot-maven-plugin is skipped");
            return;
        }

        this.buildPathAndPluginDir();

        this.getLog().info("repackage " + this.sourceJar);

        try {
            File src = new File(this.sourceDir, this.sourceJar);
            File originalFile = new File(this.sourceDir, this.sourceJar + ".original");
            // 重命名
            this.renameFile(src, originalFile);
            File dest = new File(this.sourceDir, this.sourceJar);
            Slotter slotter = new BootSlotter();
            slotter.slot(originalFile, dest);
        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
    }

    /**
     * Build path and plugin dir
     *
     * @since 1.0.0
     */
    private void buildPathAndPluginDir() {
        this.mkdir(PATCH);
        this.mkdir(PLUGIN);
    }

    /**
     * Rename file *
     *
     * @param file file
     * @param dest dest
     * @since 1.0.0
     */
    private void renameFile(@NotNull File file, File dest) {
        if (!file.renameTo(dest)) {
            throw new IllegalStateException("Unable to rename '" + file + "' to '" + dest + "'");
        }
    }

    /**
     * Mkdir *
     *
     * @param patch patch
     * @since 1.0.0
     */
    private void mkdir(String patch) {
        File file = new File(this.sourceDir.getAbsolutePath()
            + File.separator
            + patch);

        if (!file.exists() && file.mkdirs()) {
            this.getLog().debug("创建 " + file.getName());
        }
    }
}
