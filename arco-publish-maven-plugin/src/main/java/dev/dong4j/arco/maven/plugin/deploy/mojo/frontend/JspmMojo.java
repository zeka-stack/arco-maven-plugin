package dev.dong4j.arco.maven.plugin.deploy.mojo.frontend;

import com.github.eirslett.maven.plugins.frontend.lib.FrontendPluginFactory;
import com.github.eirslett.maven.plugins.frontend.lib.TaskRunnerException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>Description: </p>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.12.09 23:54
 * @since 1.7.0
 */
@Mojo(name = "jspm", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class JspmMojo extends AbstractFrontendMojo {

    /**
     * JSPM arguments. Default is "install".
     */
    @Parameter(defaultValue = "install", property = "frontend.bower.arguments", required = false)
    private String arguments;

    /**
     * Skips execution of this mojo.
     */
    @Parameter(property = "skip.jspm", defaultValue = "${skip.jspm}")
    private boolean skip;

    /**
     * Skip execution
     *
     * @return the boolean
     * @since 1.7.0
     */
    @Override
    protected boolean skipExecution() {
        return this.skip;
    }

    /**
     * Execute
     *
     * @param factory factory
     * @throws TaskRunnerException task runner exception
     * @since 1.7.0
     */
    @Override
    protected synchronized void execute(FrontendPluginFactory factory) throws TaskRunnerException {
        factory.getJspmRunner().execute(this.arguments, this.environmentVariables);
    }

}
