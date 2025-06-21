package dev.dong4j.arco.maven.plugin.helper.mojo;

import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import dev.dong4j.arco.maven.plugin.common.util.FileUtils;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Description: 删除 checkstyle pmd 等临时文件 </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dongshijie@gmail.com"
 * @date 2020.05.05 21:10
 * @since 1.0.0
 */
@Mojo(name = "delete-temp-file", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class DeleteTempFileMojo extends ArcoMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_DELETE_TEMP_FILE, defaultValue = Plugins.TURN_ON_PLUGIN)
    private boolean skip;

    /** TEMP_FILES */
    @SuppressWarnings("java:S1171")
    private static final List<String> TEMP_FILES = new ArrayList<String>() {
        private static final long serialVersionUID = -7121985380840710018L;

        {
            this.add("pmd");
            this.add("checkstyle");
            this.add("pmd.xml");
            this.add("checkstyle-checker.xml");
            this.add("checkstyle-suppressions.xml");
        }
    };

    /**
     * Execute
     *
     * @since 1.0.0
     */
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("delete-temp-file is skipped");
            return;
        }

        TEMP_FILES.forEach(tempFile -> FileUtils.deleteFiles(FileUtils.appendPath(this.getBuildDirectory(), tempFile)));

    }
}
