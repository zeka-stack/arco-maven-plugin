package dev.dong4j.arco.maven.plugin.helper.deploy;

import lombok.SneakyThrows;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.ModelBuildingException;
import org.apache.maven.model.building.ModelSource;
import org.apache.maven.model.building.StringModelSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.artifact.ProjectArtifactMetadata;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.deploy.ArtifactDeployer;
import org.apache.maven.shared.transfer.artifact.deploy.ArtifactDeployerException;
import org.apache.maven.shared.transfer.repository.RepositoryManager;
import org.apache.maven.shared.utils.Os;
import org.codehaus.plexus.util.IOUtil;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.WriterFactory;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs the artifact in the remote repository.
 * <p>
 * mvn deploy:deploy-file -Durl=file://C:\m2-repo \
 * -DrepositoryId=some.id \
 * -Dfile=your-artifact-1.0.jar \
 * [-DpomFile=your-pom.xml] \
 * [-DgroupId=org.some.group] \
 * [-DartifactId=your-artifact] \
 * [-Dversion=1.0] \
 * [-Dpackaging=jar] \
 * [-Dclassifier=test] \
 * [-DgeneratePom=true] \
 * [-DgeneratePom.description="My Project Description"] \
 * [-DrepositoryLayout=legacy]
 *
 * @author dong4j
 * @version 1.3.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.04.02 11:29
 * @since 1.0.0
 */
@SuppressWarnings("all")
@Mojo(name = "upload-file", threadSafe = true, defaultPhase = LifecyclePhase.DEPLOY)
public class DeployFileMojo extends AbstractDeployMojo {
    /** Artifact deployer */
    @Component
    private ArtifactDeployer artifactDeployer;

    /** Used for attaching the artifacts to deploy to the project. */
    @Component
    private MavenProjectHelper projectHelper;
    /** Used for creating the project to which the artifacts to deploy will be attached. */
    @Component
    private ProjectBuilder projectBuilder;

    /** Repo manager */
    @Component
    private RepositoryManager repoManager;

    /**
     * Server Id to map on the &lt;id&gt; under &lt;server&gt; section of settings.xml In most cases, this parameter
     * will be required for authentication.
     */
    @Parameter(property = "repositoryId")
    private String repositoryId;

    /**
     * URL where the artifact will be deployed. <br/>
     * ie ( file:///C:/m2-repo or scp://host.com/path/to/repo )
     */
    @Parameter(property = "url")
    private String url;

    /**
     * Upload a POM for this artifact. Will generate a default POM if none is supplied with the pomFile argument.
     */
    @Parameter(property = "generatePom", defaultValue = "true")
    private boolean generatePom;

    /**
     * Description passed to a generated POM file (in case of generatePom=true)
     */
    @Parameter(property = "generatePom.description")
    private String description;

    /**
     * 要部署的工件的类型.
     * 如果指定了POM文件, 则从POM文件的元素检索.
     * 如果不是通过命令行或POM指定, 则默认为文件扩展名.
     * Maven使用两个术语来引用这个数据: 整个POM的元素和依赖规范中的元素.
     */
    @Parameter(property = "packaging", defaultValue = "jar")
    private String packaging;

    /**
     * Execute *
     *
     * @throws MojoExecutionException mojo execution exception
     * @throws MojoFailureException   mojo failure exception
     * @since 1.0.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        // 离线模式不上传
        this.failIfOffline();

        // 获取所有待上传的组件
        List<Dependency> uploadDependency = this.project.getDependencies();

        if (uploadDependency.isEmpty()) {
            throw new MojoExecutionException("没有待上传的文件");
        }

        // 创建待上传的 maven 项目模块 (用每个 Dependency 创建一个子模块, 然后循环上传)
        List<MavenProject> uploadProjects = new ArrayList<>(uploadDependency.size());

        uploadDependency.forEach(d -> uploadProjects.add(this.createMavenProject(d)));

        List<Artifact> deployableArtifacts = new ArrayList<>();

        uploadProjects.forEach(p -> {
            Artifact artifact = p.getArtifact();

            artifact.setFile(p.getFile());
            deployableArtifacts.add(artifact);

            // 为上传的 jar 生成 pom
            File pom = null;
            if (this.generatePom) {
                pom = this.generatePomFile(p);
            }
            if (pom != null) {
                ProjectArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pom);
                artifact.addMetadata(metadata);
            }
            ArtifactRepository artifactRepository = this.buildRepository(p);
            artifact.setRepository(artifactRepository);
            deployableArtifacts.addAll(p.getAttachedArtifacts());

            try {
                this.artifactDeployer.deploy(this.getSession().getProjectBuildingRequest(),
                    artifactRepository,
                    deployableArtifacts);
            } catch (ArtifactDeployerException e) {
                this.getLog().error("upload error: " + e.getMessage());
            }
        });

    }

    /**
     * Build repository deployment repository
     *
     * @param project project
     * @return the deployment repository
     * @since 1.0.0
     */
    @SneakyThrows
    private @NotNull ArtifactRepository buildRepository(@NotNull MavenProject project) {

        ArtifactRepository deploymentRepository = this.createDeploymentArtifactRepository(this.repositoryId, this.url);
        // 协议检查
        String protocol = deploymentRepository.getProtocol();
        if (StringUtils.isEmpty(protocol)) {
            throw new MojoExecutionException("No transfer protocol found.");
        }

        return deploymentRepository;
    }

    /**
     * 从用户提供的groupId artifactId 和 version 在内存中创建 Maven 项目.提供分类器时, 包装必须是POM, 因为只有附件的项目.此项目用作附加要部署的工件的基础
     *
     * @param dependency dependency
     * @return the maven project
     * @since 1.0.0
     */
    @SneakyThrows
    private @NotNull MavenProject createMavenProject(@NotNull Dependency dependency) {
        if (dependency.getGroupId() == null
            || dependency.getArtifactId() == null
            || dependency.getVersion() == null
            || dependency.getType() == null) {
            throw new MojoExecutionException("The artifact information is incomplete: 'groupId', 'artifactId', "
                + "'version' and 'type' are required.");
        }

        // 创建一个最小的 maven 项目
        ModelSource modelSource = new StringModelSource(MessageFormat.format("<project>" +
                "<modelVersion>4.0.0</modelVersion>" +
                "<groupId>{0}</groupId>" +
                "<artifactId>{1}</artifactId>" +
                "<version>{2}</version>" +
                "<packaging>{3}</packaging>" +
                "</project>",
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getVersion(),
            dependency.getType()));

        DefaultProjectBuildingRequest buildingRequest =
            new DefaultProjectBuildingRequest(this.getSession().getProjectBuildingRequest());
        buildingRequest.setProcessPlugins(false);
        try {
            MavenProject project = this.projectBuilder.build(modelSource, buildingRequest).getProject();
            // 分为 jar (type=jar) 和 pom (type=pom) 2 种文件
            project.setFile(new File(dependency.getSystemPath()));
            return project;
        } catch (ProjectBuildingException e) {
            if (e.getCause() instanceof ModelBuildingException) {
                throw new MojoExecutionException("The artifact information is not valid:" + Os.LINE_SEP
                    + e.getCause().getMessage());
            }
            throw new MojoFailureException("Unable to create the project.", e);
        }
    }

    /**
     * 获取从本地存储库中提供的groupId、artifactId、version、classifier和packaging构造的项目的路径.注意, 返回的路径不必存在
     *
     * @param project project
     * @return The absolute path to the artifact when installed, never <code>null</code>.
     * @since 1.0.0
     */
    @Contract("_ -> new")
    @NotNull
    private File getLocalRepoFile(@NotNull MavenProject project) {
        DefaultArtifactCoordinate coordinate = new DefaultArtifactCoordinate();
        coordinate.setGroupId(project.getGroupId());
        coordinate.setArtifactId(project.getArtifactId());
        coordinate.setVersion(project.getVersion());
        coordinate.setExtension(this.packaging);
        String path = this.repoManager.getPathForLocalArtifact(this.getSession().getProjectBuildingRequest(), coordinate);
        return new File(this.repoManager.getLocalRepositoryBasedir(this.getSession().getProjectBuildingRequest()), path);
    }

    /**
     * 从用户提供的工件信息生成最小的POM
     *
     * @param project project
     * @return The path to the generated POM file, never <code>null</code>.
     * @throws MojoExecutionException If the generation failed.
     * @since 1.0.0
     */
    @SneakyThrows
    private @NotNull File generatePomFile(MavenProject project) {
        Model model = this.generateModel(project);

        Writer fw = null;
        try {
            File tempFile = File.createTempFile("mvndeploy", ".pom");
            tempFile.deleteOnExit();

            fw = WriterFactory.newXmlWriter(tempFile);

            new MavenXpp3Writer().write(fw, model);

            fw.close();
            fw = null;

            return tempFile;
        } catch (IOException e) {
            throw new MojoExecutionException("Error writing temporary pom file: " + e.getMessage(), e);
        } finally {
            IOUtil.close(fw);
        }
    }

    /**
     * Generates a minimal model from the user-supplied artifact information.
     *
     * @param project project
     * @return The generated model, never <code>null</code>.
     * @since 1.0.0
     */
    private @NotNull Model generateModel(@NotNull MavenProject project) {
        Model model = new Model();

        model.setModelVersion("4.0.0");

        model.setGroupId(project.getGroupId());
        model.setArtifactId(project.getArtifactId());
        model.setVersion(project.getVersion());
        model.setPackaging(this.packaging);

        model.setDescription(this.description);

        return model;
    }
}
