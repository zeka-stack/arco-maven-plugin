package dev.dong4j.arco.maven.plugin.helper.mojo;

import dev.dong4j.arco.maven.plugin.common.util.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.StringUtils;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalRepository;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

/**
 * <p>Description: 删除指定的 maven 依赖 </p>
 * 第一次使用:
 * 1. mvn dependency:get -Dartifact=dev.dong4j:arco-assist-maven-plugin:0.0.1-SNAPSHOT
 * 2. mvn dev.dong4j:arco-assist-maven-plugin:0.0.1-SNAPSHOT:clear -Dname=指定包名(前缀匹配) -Dversion=指定版本号(前缀匹配)
 * <p>
 * 之后可简化: mvn arco-assist:clear -Dname= -Dversion=
 * 全部功能:
 * 1. 删除 dev/dong4j 下所有的 zeka.stack 依赖
 * mvn arco-assist:clear -Dname=all -Dversion=all
 * 2. 删除 dev/dong4j 下所有包中指定的版本
 * mvn arco-assist:clear -Dname=all -Dversion=x.x.x
 * 3. 删除 dev/dong4j 下指定的包中指定的版本
 * mvn arco-assist:clear -Dname=xxx -Dversion=x.x.x
 * 4. 删除 dev/dong4j 下指定的包中所有的版本
 * mvn arco-assist:clear -Dname=xxx -Dversion=all
 * 5. 删除无效的目录和缓存
 * mvn arco-assist:clear
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2021.01.20 18:53
 * @since 1.7.1
 */
@Mojo(name = "clear", requiresProject = false)
public class DeleteMavenDependenceMojo extends AbstractMojo {
    /** ALL_FLAG */
    private static final String ALL_FLAG = "all";
    /** Repo session */
    @Parameter(defaultValue = "${repositorySystemSession}")
    private RepositorySystemSession repoSession;

    /**
     * STACK_DEPENDENCE
     * todo-dong4j : (2025.06.21 18:16) [使用脚本生成: 记录目录下有 pom.xml 文件的目录名]
     */
    private static final String[] STACK_DEPENDENCE = new String[]{
        "arco-meta",
        "arco-supreme",
        "arco-builder",
        "arco-business-parent",
        "arco-component-parent",
        "arco-dependencies-parent",
        "arco-distribution-parent",
        "arco-business-distribution",
        "arco-doc-distribution",
        "arco-project-builder",
        "arco-project-dependencies",

        "blen-toolkit",
        "cubo-starter",
        "cubo-starter-examples",
        "cubo-starter-examples",
        "domi-suite",
        "eiko-orch",
        "felo-space",
    };

    /** ERROR_DIR */
    private static final String[] ERROR_DIR = new String[]{
        "unknown",
        "${revision}"
    };

    /** 更新错误的缓存文件 */
    private static final String[] ERROR_FILE = new String[]{
        "lastUpdated"
    };

    /**
     * Execute
     *
     * @throws MojoExecutionException mojo execution exception
     * @throws MojoFailureException   mojo failure exception
     * @since 1.7.1
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        LocalRepository localRepo = this.repoSession.getLocalRepository();
        File basedir = localRepo.getBasedir();

        String rootPath = basedir.getPath() + File.separator + "dev" + File.separator + "dong4j";
        this.getLog().info(basedir.getPath());

        File rootFile = new File(rootPath);
        this.clear(rootFile);
    }

    /**
     * Clear
     *
     * @param rootFile root file
     * @since 1.7.1
     */
    private void clear(File rootFile) {
        String artifactId = System.getProperty("name", "");
        String version = System.getProperty("version", "");

        // 删除无效的目录和缓存 (mvn arco-assist:clean)
        if (StringUtils.isBlank(artifactId) && StringUtils.isBlank(version)) {
            this.deleteAllFile(rootFile, new String[]{}, ERROR_DIR);
            this.deleteErrorFile(rootFile);
        } else if (ALL_FLAG.equals(artifactId) && ALL_FLAG.equals(version)) {
            // 删除 dev/dong4j 下所有的 zeka.stack 依赖 (mvn arco-assist:clear -Dname=all -Dversion=all)
            this.deleteAllFile(rootFile, STACK_DEPENDENCE, new String[]{});
        } else if (ALL_FLAG.equals(artifactId) && StringUtils.isNotBlank(version)) {
            // 删除 dev/dong4j 下所有包中指定的版本 (mvn arco-assist:clear -Dname=all -Dversion=x.x.x)
            this.deleteByVersion(rootFile, STACK_DEPENDENCE, version);
        } else if (StringUtils.isNotBlank(version)
            && !ALL_FLAG.equals(version)
            && StringUtils.isNotBlank(artifactId)
            && !ALL_FLAG.equals(artifactId)) {
            // 删除 dev/dong4j 下指定的包中指定的版本 (mvn arco-assist:clear -Dname=xxx -Dversion=x.x.x)
            this.deleteByNameAndVersion(rootFile, artifactId, version);
        } else if (ALL_FLAG.equals(version) && StringUtils.isNotBlank(artifactId)) {
            // 删除 dev/dong4j 下指定的包中所有的版本 (mvn arco-assist:clear -Dname=xxx -Dversion=all)
            this.deleteByName(rootFile, artifactId);
        } else {
            throw new IllegalArgumentException("命名错误: \n" +
                "1. 删除 dev/dong4j 下所有的 zeka.stack 依赖\n" +
                "mvn arco-assist:clear -Dname=all -Dversion=all\n" +
                "2. 删除 dev/dong4j 下所有包中指定的版本\n" +
                "mvn arco-assist:clear -Dname=all -Dversion=x.x.x\n" +
                "3. 删除 dev/dong4j 下指定的包中指定的版本\n" +
                "mvn arco-assist:clear -Dname=xxx -Dversion=x.x.x\n" +
                "4. 删除 dev/dong4j 下指定的包中所有的版本\n" +
                "mvn arco-assist:clear -Dname=xxx -Dversion=all\n" +
                "5. 删除无效的目录和缓存\n" +
                "mvn arco-assist:clean");
        }
    }

    /**
     * 删除更新的缓存文件
     *
     * @param rootFile root file
     * @since 1.8.0
     */
    @Contract(pure = true)
    private void deleteErrorFile(File rootFile) {
        Collection<File> listFiles = org.apache.commons.io.FileUtils.listFiles(rootFile, ERROR_FILE, true);
        listFiles.forEach(f -> {
            try {
                Files.delete(f.toPath());
            } catch (IOException e) {
                this.getLog().info("delete: " + f);
            }
        });
    }

    /**
     * Delete by name and version
     *
     * @param rootFile   root file
     * @param artifactId artifact id
     * @param version    version
     * @since 1.7.1
     */
    private void deleteByNameAndVersion(File rootFile, String artifactId, String version) {
        Arrays.stream(Objects.requireNonNull(rootFile.listFiles())).filter(File::isDirectory).forEach(nameFile -> {
            if (match(artifactId, nameFile.getName())) {
                Arrays.stream(Objects.requireNonNull(nameFile.listFiles())).filter(File::isDirectory).forEach(versionFile -> {
                    if (match(version, versionFile.getName())) {
                        this.deleteFile(versionFile);
                    }
                });
            }
        });
    }

    /**
     * 删除指定的 version
     *
     * @param rootFile root file
     * @param names    names
     * @param version  version
     * @since 1.7.1
     */
    private void deleteByVersion(File rootFile, String[] names, String version) {
        Arrays.stream(Objects.requireNonNull(rootFile.listFiles()))
            .filter(File::isDirectory)
            .forEach(file -> Arrays.stream(names).forEach(nameFile -> {
                if (match(nameFile, file.getName())) {
                    File[] files = file.listFiles();
                    if (files != null && file.length() > 0) {
                        Arrays.stream(files).forEach(versionFile -> {
                            if (match(version, versionFile.getName())) {
                                this.deleteFile(versionFile);
                            }
                        });
                    }
                }
            }));

    }

    /**
     * 删除指定的模块
     *
     * @param rootFile   root file
     * @param artifactId 支持正则
     * @since 1.7.1
     */
    private void deleteByName(File rootFile, String artifactId) {
        Arrays.stream(Objects.requireNonNull(rootFile.listFiles())).filter(File::isDirectory).forEach(file -> {
            if (match(artifactId, file.getName())) {
                this.deleteFile(file);
            }
        });
    }

    /**
     * 删除 zeka.stack 的所有依赖
     *
     * @param rootFile root file
     * @param names    names
     * @param versions versions
     * @since 1.7.1
     */
    @SuppressWarnings("java:S3776")
    private void deleteAllFile(@NotNull File rootFile, String[] names, String[] versions) {
        if (names.length == 0 && versions.length != 0) {
            Arrays.stream(Objects.requireNonNull(rootFile.listFiles())).filter(File::isDirectory).forEach(nameFile -> {
                File[] files = nameFile.listFiles();
                if (files != null && files.length > 0) {
                    Arrays.stream(files).filter(File::isDirectory)
                        .forEach(versionFile -> Arrays.stream(versions)
                            .forEach(version -> {
                                if (match(version, versionFile.getName())) {
                                    this.deleteFile(versionFile);
                                }
                            }));
                }
            });
        } else if (versions.length == 0 && names.length != 0) {
            Arrays.stream(Objects.requireNonNull(rootFile.listFiles()))
                .filter(File::isDirectory)
                .forEach(nameFile ->
                    Arrays.stream(names).forEach(name -> {
                        if (match(name,
                            nameFile.getName())) {
                            this.deleteFile(nameFile);
                        }
                    }));
        }
    }

    /**
     * Delete file
     *
     * @param file file
     * @since 1.7.1
     */
    private void deleteFile(File file) {
        try {
            boolean b = FileUtils.deleteDir(file);
            if (b) {
                this.getLog().info("delete: " + file);
            }
        } catch (Exception ignored) {
            // nothing to do
        }
    }

    /**
     * 前缀匹配
     *
     * @param regex        regex
     * @param beTestString be test string
     * @return the boolean
     * @since 1.7.1
     */
    public static boolean match(String regex, String beTestString) {
        return beTestString.startsWith(regex);
    }

}
