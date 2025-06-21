package dev.dong4j.arco.maven.plugin.common.util;

import lombok.experimental.UtilityClass;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;

/**
 * Executes an arbitrary mojo using a fluent interface.  This is meant to be executed within the context of a Maven 2
 * mojo.
 * <p>
 * Here is an execution that invokes the dependency plugin:
 * <pre>
 * executeMojo(
 *              plugin(
 *                      groupId("org.apache.maven.plugins"),
 *                      artifactId("maven-dependency-plugin"),
 *                      version("2.0")
 *              ),
 *              goal("copy-dependencies"),
 *              configuration(
 *                      element(name("outputDirectory"), "${project.build.directory}/foo")
 *              ),
 *              executionEnvironment(
 *                      project,
 *                      session,
 *                      pluginManager
 *              )
 *          );
 * </pre>
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.05.01 21:49
 * @see `http://code.google.com/p/mojo-executor/`
 * @since 1.0.0
 */
@UtilityClass
public class MojoExecutor {
    /**
     * Entry point for executing a mojo
     *
     * @param plugin        The plugin to execute
     * @param goal          The goal to execute
     * @param configuration The execution configuration
     * @param env           The execution environment
     * @throws MojoExecutionException If there are any exceptions locating or executing the mojo
     * @since 1.0.0
     */
    @Contract("_, _, null, _ -> fail")
    public static void executeMojo(Plugin plugin, String goal, Xpp3Dom configuration,
                                   ExecutionEnvironment env) throws MojoExecutionException {
        if (configuration == null) {
            throw new NullPointerException("configuration may not be null");
        }
        try {
            MavenSession session = env.getMavenSession();

            PluginDescriptor pluginDescriptor = env.getPluginManager().loadPlugin(plugin,
                Collections.emptyList(),
                session.getRepositorySession());
            MojoDescriptor mojo = pluginDescriptor.getMojo(goal);
            if (mojo == null) {
                throw new MojoExecutionException("Could not find goal '" + goal + "' in plugin "
                    + plugin.getGroupId() + ":"
                    + plugin.getArtifactId() + ":"
                    + plugin.getVersion());
            }
            configuration = Xpp3DomUtils.mergeXpp3Dom(configuration,
                toXpp3Dom(mojo.getMojoConfiguration()));
            MojoExecution exec = new MojoExecution(mojo, configuration);
            env.getPluginManager().executeMojo(session, exec);
        } catch (Exception e) {
            throw new MojoExecutionException("Unable to execute mojo", e);
        }
    }

    /**
     * Converts PlexusConfiguration to a Xpp3Dom.
     *
     * @param config the PlexusConfiguration
     * @return the Xpp3Dom representation of the PlexusConfiguration
     * @since 1.0.0
     */
    private static @NotNull Xpp3Dom toXpp3Dom(@NotNull PlexusConfiguration config) {
        Xpp3Dom result = new Xpp3Dom(config.getName());
        result.setValue(config.getValue(null));
        for (String name : config.getAttributeNames()) {
            result.setAttribute(name, config.getAttribute(name));
        }
        for (PlexusConfiguration child : config.getChildren()) {
            result.addChild(toXpp3Dom(child));
        }
        return result;
    }

    /**
     * Constructs the {@link ExecutionEnvironment} instance fluently
     *
     * @param mavenProject  The current Maven project
     * @param mavenSession  The current Maven session
     * @param pluginManager The Build plugin manager
     * @return The execution environment
     * @throws NullPointerException if mavenProject, mavenSession or pluginManager                              are null
     * @since 1.0.0
     */
    public static ExecutionEnvironment executionEnvironment(MavenProject mavenProject,
                                                            MavenSession mavenSession,
                                                            BuildPluginManager pluginManager) {
        return new ExecutionEnvironment(mavenProject, mavenSession, pluginManager);
    }

    /**
     * Builds the configuration for the goal using Elements
     *
     * @param elements A list of elements for the configuration section
     * @return The elements transformed into the Maven-native XML format
     * @since 1.0.0
     */
    public static Xpp3Dom configuration(Element... elements) {
        Xpp3Dom dom = new Xpp3Dom("configuration");
        for (Element e : elements) {
            dom.addChild(e.toDom());
        }
        return dom;
    }

    /**
     * Defines the plugin without its version
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @return The plugin instance
     * @since 1.0.0
     */
    public static Plugin plugin(String groupId, String artifactId) {
        return plugin(groupId, artifactId, null);
    }

    /**
     * Defines a plugin
     *
     * @param groupId    The group id
     * @param artifactId The artifact id
     * @param version    The plugin version
     * @return The plugin instance
     * @since 1.0.0
     */
    public static Plugin plugin(String groupId, String artifactId, String version) {
        Plugin plugin = new Plugin();
        plugin.setArtifactId(artifactId);
        plugin.setGroupId(groupId);
        plugin.setVersion(version);
        return plugin;
    }

    /**
     * Wraps the group id string in a more readable format
     *
     * @param groupId The value
     * @return The value
     * @since 1.0.0
     */
    public static String groupId(String groupId) {
        return groupId;
    }

    /**
     * Wraps the artifact id string in a more readable format
     *
     * @param artifactId The value
     * @return The value
     * @since 1.0.0
     */
    public static String artifactId(String artifactId) {
        return artifactId;
    }

    /**
     * Wraps the version string in a more readable format
     *
     * @param version The value
     * @return The value
     * @since 1.0.0
     */
    public static String version(String version) {
        return version;
    }

    /**
     * Wraps the goal string in a more readable format
     *
     * @param goal The value
     * @return The value
     * @since 1.0.0
     */
    public static String goal(String goal) {
        return goal;
    }

    /**
     * Wraps the element name string in a more readable format
     *
     * @param name The value
     * @return The value
     * @since 1.0.0
     */
    public static String name(String name) {
        return name;
    }

    /**
     * Constructs the element with a textual body
     *
     * @param name  The element name
     * @param value The element text value
     * @return The element object
     * @since 1.0.0
     */
    public static Element element(String name, String value) {
        return new Element(name, value);
    }

    /**
     * Constructs the element containg child elements
     *
     * @param name     The element name
     * @param elements The child elements
     * @return The Element object
     * @since 1.0.0
     */
    public static Element element(String name, Element... elements) {
        return new Element(name, elements);
    }

    /**
     * Element wrapper class for configuration elements
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.05.01 21:49
     * @since 1.0.0
     */
    public static class Element {
        /** Children */
        private final Element[] children;
        /** Name */
        private final String name;
        /** Text */
        private final String text;

        /**
         * Element
         *
         * @param name     name
         * @param children children
         * @since 1.0.0
         */
        public Element(String name, Element... children) {
            this(name, null, children);
        }

        /**
         * Element
         *
         * @param name     name
         * @param text     text
         * @param children children
         * @since 1.0.0
         */
        public Element(String name, String text, Element... children) {
            this.name = name;
            this.text = text;
            this.children = children;
        }

        /**
         * To dom
         * xpp 3 dom
         *
         * @return the xpp 3 dom
         * @since 1.0.0
         */
        public Xpp3Dom toDom() {
            Xpp3Dom dom = new Xpp3Dom(this.name);
            if (this.text != null) {
                dom.setValue(this.text);
            }
            for (Element e : this.children) {
                dom.addChild(e.toDom());
            }
            return dom;
        }
    }

    /**
     * Collects Maven execution information
     *
     * @author dong4j
     * @version 1.0.0
     * @email "mailto:dong4j@gmail.com"
     * @date 2020.05.01 21:49
     * @since 1.0.0
     */
    public static class ExecutionEnvironment {
        /** Maven project */
        private final MavenProject mavenProject;
        /** Maven session */
        private final MavenSession mavenSession;
        /** Plugin manager */
        private final BuildPluginManager pluginManager;

        /**
         * Execution environment
         *
         * @param mavenProject  maven project
         * @param mavenSession  maven session
         * @param pluginManager plugin manager
         * @since 1.0.0
         */
        public ExecutionEnvironment(MavenProject mavenProject, MavenSession mavenSession,
                                    BuildPluginManager pluginManager) {
            if (mavenProject == null) {
                throw new NullPointerException("mavenProject may not be null");
            }
            if (mavenSession == null) {
                throw new NullPointerException("mavenSession may not be null");
            }
            if (pluginManager == null) {
                throw new NullPointerException("pluginManager may not be null");
            }
            this.mavenProject = mavenProject;
            this.mavenSession = mavenSession;
            this.pluginManager = pluginManager;
        }

        /**
         * Gets maven project *
         *
         * @return the maven project
         * @since 1.0.0
         */
        public MavenProject getMavenProject() {
            return this.mavenProject;
        }

        /**
         * Gets maven session *
         *
         * @return the maven session
         * @since 1.0.0
         */
        public MavenSession getMavenSession() {
            return this.mavenSession;
        }

        /**
         * Gets plugin manager *
         *
         * @return the plugin manager
         * @since 1.0.0
         */
        public BuildPluginManager getPluginManager() {
            return this.pluginManager;
        }
    }
}
