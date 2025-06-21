package dev.dong4j.arco.maven.plugin.makeself.mojo;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Joiner;
import dev.dong4j.arco.maven.plugin.common.ArcoMavenPluginAbstractMojo;
import dev.dong4j.arco.maven.plugin.common.Plugins;
import dev.dong4j.arco.maven.plugin.common.enums.ModuleType;
import dev.dong4j.arco.maven.plugin.common.util.CompressUtils;
import dev.dong4j.arco.maven.plugin.common.util.FileUtils;
import dev.dong4j.arco.maven.plugin.common.util.PluginUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * The Class MakeselfMojo.
 * https://github.com/megastep/makeself
 * package 用于普通的打包方式, 只会生成 tar.gz.
 * 此插件在 verify 阶段执行, 在解压 tar.gz 后自动写入 launcher.sh, 最终生成 xxx.run 自解压部署包, 上传服务器后直接执行 `./xxxx.run` 即可,
 * 默认的启动环境为 prod, 指定环境可通过添加变量 `./xxxx.run test`.
 * 第一次执行后会在当前路径下生成一个 xxxx 目录, 如果再次执行 `./xxxx.run` 将不会再次解压.
 *
 * @author dong4j
 * @version 1.0.0
 * @email "mailto:dong4j@gmail.com"
 * @date 2020.06.11 16:07
 * @since 1.5.0
 */
@SuppressWarnings("all")
@Mojo(name = "makeself", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = false, threadSafe = true)
public class MakeselfMojo extends ArcoMavenPluginAbstractMojo {
    /** 直接要部署包默认后缀 */
    private static final String DEFAULT_ARCHIVE_SUFFIX = ".run";
    /** assembly 默认的打包格式 */
    private static final String DEFAULT_ASSEMBLY_ARCHIVE_SUFFIX = ".tar.gz";
    /** makeself 相关脚本目录 */
    private static final String MAKESELF_LOCATION = "META-INF/makeself/";
    /** 启动脚本 相关目录 */
    private static final String LAUNCHER_LOCATION = "META-INF/launcher/";
    /** MAKESELF_SCRIPT */
    private static final String MAKESELF_SCRIPT = "makeself.sh";
    /** MAKESELF_HEADER_SCRIPT */
    private static final String MAKESELF_HEADER_SCRIPT = "makeself-header.sh";
    /** LAUNCHER_SCRIPT */
    private static final String LAUNCHER_SCRIPT = "launcher.sh";
    /** 服务器使用 zekastack 用户启动, 本地不需要切换用户, 使用此 key 标识 */
    private static final String PACKAGE_TYPE = "package.type";
    /** LOCAL_PACKAGE_TYPE */
    private static final String LOCAL_PACKAGE_TYPE = "local";
    /** LOCAL_AUNCHER_SCRIPT */
    private static final String LOCAL_AUNCHER_SCRIPT = "launcher-local.sh";
    /** BASH */
    private static final String BASH = "bash";

    /** archive_dir 存档的文件的目录的名称. */
    @Parameter(property = "archiveDir", required = true)
    private String archiveDir;
    /** file_name 创建的存档的名称. */
    @Parameter(property = "fileName")
    private String fileName;
    /** 描述包的任意文本字符串, 它将在提取文件时显示 */
    @Parameter(defaultValue = "Make self-extrabable archives", property = "label")
    private String label;
    /** 从解压文件目录中执行的命令, 如果要执行此目录中包含的程序, 则必须在命令前面加上 './', 例如 ./program.sh */
    @Parameter(defaultValue = "./launcher.sh", property = "startupScript")
    private String startupScript;
    /**
     * 传递给脚本的参数
     * {@code
     * <scriptArgs>
     * <scriptArg>arg1</scriptArg>
     * <scriptArg>arg2</scriptArg>
     * </scriptArgs>
     * }****
     */
    @Parameter(property = "scriptArgs")
    private List<String> scriptArgs;
    /** --help | -h : 打印帮助消息. */
    @Parameter(property = "help")
    private Boolean help;
    /** --gzip : 使用 gzip 进行压缩 */
    @Parameter(property = "gzip", defaultValue = "true")
    private Boolean gzip;

    /**
     * --bzip2 : 使用 bzip2 而不是 gzip 来获得更好的压缩,
     * bzip2 命令必须在命令路径中可用, 建议将存档前缀设置为 .bz2.run 之类的值, 以便潜在用户知道他们需要 bzip2 来提取它
     */
    @Parameter(property = "bzip2")
    private Boolean bzip2;

    /**
     * --pbzip2 : 使用 pbzip2 而不是gzip, 以便在具有多个 cpu 的计算机上实现更好更快的压缩,
     * pbzip2 命令必须在命令路径中可用, 建议将存档前缀设置为 .bz2.run 之类的值, 以便潜在用户知道他们需要 bzip2 来提取它,
     */
    @Parameter(property = "pbzip2")
    private Boolean pbzip2;

    /**
     * --xz : 使用xz而不是gzip来获得更好的压缩,
     * xz命令必须在命令路径中可用, 建议将存档前缀设置为类似的值
     */
    @Parameter(property = "xz")
    private Boolean xz;

    /** --lzo : 使用 lzop 而不是 gzip 来获得更好的压缩 */
    @Parameter(property = "lzo")
    private Boolean lzo;
    /** --lz4 : 使用 lz4 而不是 gzip 来获得更好的压缩 */
    @Parameter(property = "lz4")
    private Boolean lz4;
    /** --pigz : Use pigz for compression. */
    @Parameter(property = "pigz")
    private Boolean pigz;
    /** --base64 : 以 base64 格式将存档编码为 ASCII */
    @Parameter(property = "base64")
    private Boolean base64;
    /** --gpg-encrypt : 使用 gpg-ac-z$COMPRESS_ 级别加密存档, 这将提示输入要加密的密码 */
    @Parameter(property = "gpgEncrypt")
    private Boolean gpgEncrypt;
    /** --gpg-asymmetric-encrypt-sign : 非对称加密签名: 不是压缩, 而是使用gpg对数据进行非对称加密和签名, */
    @Parameter(property = "gpgAsymmetricEncryptSign")
    private Boolean gpgAsymmetricEncryptSign;
    /** --ssl-encrypt : 使用 openssl aes-256-cbc-a-salt 加密存档, 这将提示输入要加密的密码, 假设潜在用户安装了 OpenSSL 工具, */
    @Parameter(property = "sslEncrypt")
    private Boolean sslEncrypt;
    /** --ssl-passwd pass : 使用给定的密码使用OpenSSL加密数据, */
    @Parameter(property = "sslPasswd")
    private String sslPasswd;
    /** --ssl-pass-src : 使用给定的 src 作为使用 OpenSSL 加密数据的密码源 */
    @Parameter(property = "sslPassSrc")
    private String sslPassSrc;
    /** --ssl-no-md : 不要使用旧版 OpenSSL 不支持的 '-md' 选项, */
    @Parameter(property = "sslNoMd")
    private Boolean sslNoMd;
    /** --compress : 使用 UNIX compress 命令压缩数据, 这应该是所有没有可用 gzip 的平台上的默认值 */
    @Parameter(property = "compress")
    private Boolean compress;
    /** --nocomp : 不要对归档文件使用任何压缩, 它将是一个未压缩的 TAR, */
    @Parameter(property = "nocomp")
    private Boolean nocomp;
    /** --complevel : 指定 gzip、bzip2、pbzip2、xz、lzo 或 lz4 的压缩级别 */
    @Parameter(property = "complevel")
    private Integer complevel;
    /** --notemp : 生成的存档文件不会将文件提取到临时目录中, 而是在当前目录中创建的新目录中, */
    @Parameter(property = "notemp", defaultValue = "true")
    private Boolean notemp;
    /**
     * --current : 文件将被提取到当前目录, 而不是子目录中
     * --notemp above.
     */
    @Parameter(property = "current")
    private Boolean current;
    /** --follow : 遵循存档目录中的符号链接, 即存储指向的文件, 而不是链接本身, */
    @Parameter(property = "follow")
    private Boolean follow;

    /**
     * --append (new in 2.1.x): 将数据追加到现有存档, 而不是创建新的存档,
     * 在此模式下, 将重用原始存档中的设置 (压缩类型、标签、嵌入式脚本) , 因此不需要在命令行中再次指定,
     */
    @Parameter(property = "append")
    private Boolean append;

    /**
     * --header: Makeself 2.0 使用一个单独的文件来存储头存根, 称为 makeself-header.sh,
     * 默认情况下, 假定它与 makeself.sh公司, 如果将其存储在其他位置, 则此选项可用于指定其实际位置, 此插件不需要此项, 因为提供了头,
     */
    @Parameter(property = "headerFile", readonly = true)
    private Boolean headerFile;

    /**
     * --copy : 提取后存档将首先将自身提取到临时目录中,
     * 它的主要应用是允许独立安装程序存储在 CD 上的 Makeself存档中, 而安装程序稍后需要卸载 CD 并允许插入新的 CD,
     * 这可以防止跨多张 CD 的安装程序出现“文件系统忙”错误,
     */
    @Parameter(property = "copy")
    private Boolean copy;
    /** --nox11 : 禁用 X11 中新终端的自动生成, . */
    @Parameter(property = "nox11")
    private Boolean nox11;
    /** --nox11 : 禁用 X11中 新终端的自动生成. */
    @Parameter(property = "nowait")
    private Boolean nowait;
    /** --nomd5 : 禁用为存档创建 MD5 校验和, 如果不需要完整性检查, 这将加快提取过程. */
    @Parameter(property = "nomd5", defaultValue = "false")
    private Boolean nomd5;
    /** --nocrc : 禁用为存档创建 CRC 校验和, 如果不需要完整性检查, 这将加快提取过程. */
    @Parameter(property = "nocrc", defaultValue = "false")
    private Boolean nocrc;
    /** --sha256 : 为存档计算 sha256 校验和. */
    @Parameter(property = "sha256", defaultValue = "false")
    private Boolean sha256;

    /**
     * --lsm file : 将 lsm 文件提供给 makeself, 并将其嵌入生成的存档中,
     * LSM文件以易于解析的方式描述软件包,
     * 随后可以使用存档的 --LSM 参数检索 LSM 条目, Makeself 提供了 LSM 文件的一个示例,
     */
    @Parameter(property = "lsmFile")
    private String lsmFile;

    /** --gpg-extra opt : 将更多选项附加到 gpg 命令行, */
    @Parameter(property = "gpgExtraOpt")
    private String gpgExtraOpt;

    /**
     * --tar-extra opt : 向tar命令行追加更多选项,
     * 例如, 为了使用 GNU tar 从打包的归档目录中排除 .git 目录, 可以使用 makeself.sh --tar extra '--exclude=.git'
     */
    @Parameter(property = "tarExtraOpt")
    private String tarExtraOpt;

    /** --untar-extra opt : 在提取 tar 存档期间将更多选项附加到, */
    @Parameter(property = "untarExtraOpt")
    private String untarExtraOpt;
    /** --keep-umask : 将 umask 设置为 shell 默认值, 而不是在执行自解压存档时重写, */
    @Parameter(property = "keepUmask")
    private Boolean keepUmask;
    /** --export-conf : 将配置变量导出到启动脚本, */
    @Parameter(property = "exportConf")
    private Boolean exportConf;
    /** --packaging-date date : 使用提供的字符串作为包装日期, 而不是当前日期 */
    @Parameter(property = "packagingDate")
    private String packagingDate;
    /** --license : Append a license file. */
    @Parameter(property = "licenseFile")
    private String licenseFile;
    /** --nooverwrite : 如果指定的目标目录已经存在, 则不提取存档文件, 默认开启, 避免重复执行 run 导致覆盖已解压的数据 */
    @Parameter(property = "nooverwrite", defaultValue = "true")
    private Boolean nooverwrite;
    /** --help-header file : 将头添加到存档的--help输出, */
    @Parameter(property = "helpHeaderFile")
    private String helpHeaderFile;
    /** Skip run of plugin. */
    @Parameter(property = Plugins.SKIP_MAKESELF, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private Boolean skip;
    /** Delete temp files */
    @Parameter(defaultValue = "true", property = "deleteTempFiles")
    private Boolean deleteTempFiles;
    /** Auto run : When set to true, resulting shell will be run. This is useful for testing purposes. */
    @Parameter(defaultValue = "false", property = "autoRun")
    private Boolean autoRun;
    /** Delete temp files */
    @Parameter(defaultValue = "false", property = "forceInvokeOnWindows")
    private Boolean forceInvokeOnWindows;
    /** The build target. */
    @Parameter(defaultValue = "${project.build.directory}/", readonly = true)
    private String buildTarget;
    /** The target directory. */
    @Parameter(defaultValue = "${project.build.directory}/makeself-tmp/", readonly = true)
    private File targetDirectory;
    /** 自定义的脚本文件 */
    @Parameter(defaultValue = "${project.basedir}/bin/launcher.sh")
    private File scriptFile;
    /** The makeself. */
    private File makeself;
    /** Static ATTACH_ARTIFACT to maven lifecycle. */
    private static final boolean ATTACH_ARTIFACT = true;

    /**
     * maven plugin 入口
     *
     * @throws MojoExecutionException mojo execution exception
     * @throws MojoFailureException   mojo failure exception
     * @since 1.5.0
     */
    @Override
    @SneakyThrows
    public void execute() {
        ModuleType moduleType = PluginUtils.moduleType();

        if (this.skip
            || !moduleType.equals(ModuleType.DELOPY)
            || (SystemUtils.IS_OS_WINDOWS && !this.forceInvokeOnWindows)) {
            this.getLog().info("arco-makeself-maven-plugin is skipped");
            return;
        }

        // 当前模块经过 assembly 打包后去掉后缀的全路径
        String targetDir = FileUtils.appendPath(this.buildTarget, this.archiveDir);
        String archiveFile = targetDir + DEFAULT_ASSEMBLY_ARCHIVE_SUFFIX;
        // 解压 assembly 生成的 tar.gz
        this.decompress(targetDir, archiveFile);

        // Setup make self files
        this.extractMakeself(targetDir);

        // Get OS Name
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");

        try {
            // Output version of bash
            this.getLog().debug("Execute Bash Version");
            this.execute(Arrays.asList(BASH, "--version"), !ATTACH_ARTIFACT);

            // Output version of makeself.sh
            this.getLog().debug("Execute Makeself Version");
            this.execute(Arrays.asList(BASH, this.makeself.getAbsolutePath(), "--version"), !ATTACH_ARTIFACT);

            // If help arguments supplied, write output and get out of code.
            String helpArgs = this.helpArgs();
            if (!helpArgs.isEmpty()) {
                this.getLog().debug("Execute Makeself Help");
                this.execute(Arrays.asList(BASH, this.makeself.getAbsolutePath(), helpArgs), !ATTACH_ARTIFACT);
                return;
            }

            // Basic Configuration
            this.getLog().debug("Loading Makeself Basic Configuration");
            List<String> target = new ArrayList<>();
            target.addAll(Arrays.asList(BASH, this.makeself.getAbsolutePath()));
            target.addAll(this.loadArgs());
            target.add(this.buildTarget.concat(this.archiveDir));
            // 默认 xxx.run
            if (StringUtils.isBlank(this.fileName)) {
                this.fileName = this.archiveDir.concat(DEFAULT_ARCHIVE_SUFFIX);
            }
            target.add(this.buildTarget.concat(this.fileName));
            target.add(this.label);
            target.add(this.startupScript);
            if (CollUtil.isEmpty(this.scriptArgs)) {
                target.addAll(this.scriptArgs);
            }

            // Indicate makeself running
            this.getLog().info("Running makeself build");

            // Execute main run of makeself.sh
            this.getLog().debug("Execute Makeself Build");
            this.execute(target, ATTACH_ARTIFACT);

            // Output info on file makeself created
            this.getLog().debug("Execute Makeself Info on Resulting Shell Script");
            this.execute(Arrays.asList(BASH, this.buildTarget.concat(this.fileName), "--info"), !ATTACH_ARTIFACT);

            // Output list on file makeself created (non windows need)
            if (!isWindows) {
                this.getLog().debug("Execute Makeself List on Resulting Shell Script");
                this.execute(Arrays.asList(BASH, this.buildTarget.concat(this.fileName), "--list"), !ATTACH_ARTIFACT);
            }

            // auto run script
            if (this.autoRun) {
                this.getLog().info("Auto-run created shell (this may take a few minutes)");
                this.execute(Arrays.asList(BASH, this.buildTarget.concat(this.fileName)), !ATTACH_ARTIFACT);
            }

            // 删除临时文件
            if (this.deleteTempFiles) {
                FileUtils.deleteFiles(targetDir);
                FileUtils.deleteFiles(archiveFile);
                FileUtils.deleteFiles(this.targetDirectory.getAbsolutePath());
            }
        } catch (IOException e) {
            this.getLog().warn(e.getMessage());
            if (e.getMessage().contains("Cannot run program 'bash'")) {
                // Note: we printed the full stack trace already so don't do it again.
                if (isWindows) {
                    this.getLog().warn(
                        "Configure Bash with Cygwin or Git for Windows by adding '/usr/bin' to environment 'Path' variable to execute " +
                            "this plugin");
                }
                this.getLog().warn("Configure Bash to execute this plugin");
            }
        } catch (InterruptedException e) {
            this.getLog().warn(e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 解压在 package 阶段生成的 xxx.tar.gz 部署包
     *
     * @param targetDir   target dir
     * @param archiveFile archive file
     * @return the 解压后的文件目录
     * @since 1.5.0
     */
    private void decompress(String targetDir, String archiveFile) {
        File file = new File(targetDir);
        if (!file.exists()) {
            // 将经过 assembly 处理后的 tar.gz 解压到 target 目录下
            CompressUtils.decompress(archiveFile, this.buildTarget);
        }
    }

    /**
     * 执行 shell 命令
     *
     * @param target target
     * @param attach attach
     * @throws IOException          io exception
     * @throws InterruptedException interrupted exception
     * @since 1.5.0
     */
    private void execute(List<String> target, boolean attach) throws IOException, InterruptedException {
        // Create Process Builder
        ProcessBuilder processBuilder = new ProcessBuilder(target);
        processBuilder.redirectErrorStream(true);

        // Create Process
        Process process = processBuilder.start();

        // Write process output
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                this.getLog().info(line);
            }
            this.getLog().info("");
        }

        // Wait for process completion
        int status = process.waitFor();
        if (status > 0) {
            this.getLog().error(Joiner.on(" ").join("makeself failed with error status:", status));
        }

        // Attach artifact to maven build for install/deploy/release on success
        if (status == 0 && attach) {
            this.projectHelper.attachArtifact(this.project, "sh", new File(this.buildTarget, FilenameUtils.getName(this.fileName)));
        }
    }

    /**
     * 将 makeself.sh 和 makeself-header.sh 写入 makeself-tmp
     *
     * @param decompressFile decompress file
     * @since 1.5.0
     */
    private void extractMakeself(String decompressFile) {
        this.getLog().debug("Extracting Makeself");

        // Create makeself directory
        File makeselfTemp = new File(this.targetDirectory.getAbsolutePath());
        if (!makeselfTemp.exists() && !makeselfTemp.mkdir()) {
            this.getLog().error(Joiner.on(" ").join("Unable to make directory", this.targetDirectory.getAbsolutePath()));
            return;
        } else {
            this.getLog().debug(Joiner.on(" ").join("Created directory for", this.targetDirectory.getAbsolutePath()));
        }

        ClassLoader classloader = this.getClass().getClassLoader();
        this.makeself = new File(this.targetDirectory, MAKESELF_SCRIPT);
        // 写入 makeself.sh
        this.writingFile(classloader, this.makeself, MAKESELF_LOCATION + MAKESELF_SCRIPT);

        // 写入 makeself-header.sh
        File makeselfHeader = new File(this.targetDirectory, MAKESELF_HEADER_SCRIPT);
        this.writingFile(classloader, makeselfHeader, MAKESELF_LOCATION + MAKESELF_HEADER_SCRIPT);

        // 写入 launcher.sh
        File launcher = new File(decompressFile, LAUNCHER_SCRIPT);
        String launcherScript;
        if (this.scriptFile.exists()) {
            launcherScript = this.scriptFile.getAbsolutePath();
        } else {
            launcherScript = LAUNCHER_LOCATION + LAUNCHER_SCRIPT;
            // 本地开发时使用
            if (StringUtils.isNotBlank(System.getProperty(PACKAGE_TYPE))
                && LOCAL_PACKAGE_TYPE.equalsIgnoreCase(System.getProperty(PACKAGE_TYPE))) {
                launcherScript = LAUNCHER_LOCATION + LOCAL_AUNCHER_SCRIPT;
            }
        }
        this.writingFile(classloader, launcher, launcherScript);
    }

    /**
     * Writing file
     *
     * @param classloader classloader
     * @param targetFile  target file   需要写入的文件
     * @param sourceFile  source file   原始文件路径(插件内部文件)
     * @since 1.5.0
     */
    private void writingFile(ClassLoader classloader, @NotNull File targetFile, String sourceFile) {
        if (!targetFile.exists()) {
            this.getLog().debug("Writing " + targetFile.getName());
            try (InputStream link = classloader.getResourceAsStream(sourceFile) == null
                ? new FileInputStream(new File(sourceFile))
                : classloader.getResourceAsStream(sourceFile)) {

                Path path = targetFile.getAbsoluteFile().toPath();
                Files.copy(Objects.requireNonNull(link), path);
                FileUtils.setFilePermissions(targetFile);
                FileUtils.setPosixFilePermissions(path);
            } catch (IOException e) {
                this.getLog().error("", e);
            }
        }
    }

    /**
     * Help args.
     *
     * @return the string
     * @since 1.5.0
     */
    private @NotNull String helpArgs() {
        this.getLog().debug("Loading help arguments");

        StringBuilder args = new StringBuilder();

        // --help | -h : Print out this help message
        if (this.isTrue(this.help)) {
            args.append("--help ");
        }
        return args.toString();
    }

    /**
     * Load args.
     *
     * @return the string
     * @since 1.5.0
     */
    private @NotNull List<String> loadArgs() {
        this.getLog().debug("Loading arguments");

        List<String> args = new ArrayList<>(50);

        if (this.isTrue(this.gzip)) {
            args.add("--gzip");
        }
        if (this.isTrue(this.bzip2)) {
            args.add("--bzip2");
        }
        if (this.isTrue(this.pbzip2)) {
            args.add("--pbzip2");
        }
        if (this.isTrue(this.xz)) {
            args.add("--xz");
        }
        if (this.isTrue(this.lzo)) {
            args.add("--lzo");
        }
        if (this.isTrue(this.lz4)) {
            args.add("--lz4");
        }
        if (this.isTrue(this.pigz)) {
            args.add("--pigz");
        }
        if (this.isTrue(this.base64)) {
            args.add("--base64");
        }
        if (this.isTrue(this.gpgEncrypt)) {
            args.add("--gpg-encrypt");
        }
        if (this.isTrue(this.gpgAsymmetricEncryptSign)) {
            args.add("--gpg-asymmetric-encrypt-sign");
        }
        if (this.isTrue(this.sslEncrypt)) {
            args.add("--ssl-encrypt");
        }
        if (this.sslPasswd != null) {
            args.add("--ssl-passwd");
            args.add(this.sslPasswd);
        }
        if (this.sslPasswd != null) {
            args.add("--ssl-pass-src");
            args.add(this.sslPassSrc);
        }
        if (this.isTrue(this.sslNoMd)) {
            args.add("--ssl-no-md");
        }
        if (this.isTrue(this.compress)) {
            args.add("--compress");
        }
        if (this.isTrue(this.nocomp)) {
            args.add("--nocomp");
        }
        if (this.complevel != null) {
            args.add("--complevel");
            args.add(this.complevel.toString());
        }
        if (this.isTrue(this.notemp)) {
            args.add("--notemp");
        }
        if (this.isTrue(this.current)) {
            args.add("--current");
        }
        if (this.isTrue(this.follow)) {
            args.add("--follow");
        }
        if (this.isTrue(this.append)) {
            args.add("--append");
        }
        if (this.headerFile != null) {
            args.add("--header");
            args.add(this.headerFile.toString());
        }
        if (this.isTrue(this.copy)) {
            args.add("--copy");
        }
        if (this.isTrue(this.nox11)) {
            args.add("--nox11");
        }
        if (this.isTrue(this.nowait)) {
            args.add("--nowait");
        }
        if (this.isTrue(this.nomd5)) {
            args.add("--nomd5");
        }
        if (this.isTrue(this.nocrc)) {
            args.add("--nocrc");
        }
        if (this.isTrue(this.sha256)) {
            args.add("--sha256");
        }
        if (this.lsmFile != null) {
            args.add("--lsm");
            args.add(this.lsmFile);
        }
        if (this.gpgExtraOpt != null) {
            args.add("--gpg-extra");
            args.add(this.gpgExtraOpt);
        }
        if (this.tarExtraOpt != null) {
            args.add("--tar-extra");
            args.add(this.tarExtraOpt);
        }
        if (this.untarExtraOpt != null) {
            args.add("--untar-extra");
            args.add(this.untarExtraOpt);
        }
        if (this.isTrue(this.keepUmask)) {
            args.add("--keep-umask");
        }
        if (this.isTrue(this.exportConf)) {
            args.add("--export-conf");
        }
        if (this.packagingDate != null) {
            args.add("--packaging-date");
            args.add(this.packagingDate);
        }
        if (this.licenseFile != null) {
            args.add("--license");
            args.add(this.licenseFile);
        }
        if (this.isTrue(this.nooverwrite)) {
            args.add("--nooverwrite");
        }
        if (this.helpHeaderFile != null) {
            args.add("--help-header");
            args.add(this.helpHeaderFile);
        }
        return args;
    }

    /**
     * Is true
     *
     * @param value value
     * @return the boolean
     * @since 1.5.0
     */
    @Contract(value = "!null -> param1; null -> false", pure = true)
    private boolean isTrue(Boolean value) {
        if (value != null) {
            return value;
        }
        return false;
    }

}
