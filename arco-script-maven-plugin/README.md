---
published: 2022.04.12
---

# 启动脚本生成

## 概述

`arco-script-maven-plugin` 是 Arco 框架的启动脚本生成插件，用于在项目打包时自动生成通用启动脚本 `launcher`。该脚本提供了完整的 Java
应用生命周期管理功能，支持启动、停止、重启、状态查看等操作，并集成了多环境、日志管理、JVM 参数、APM、JMX 等高级功能。

**核心功能**：自动生成标准化的 Java 应用启动脚本，替代传统的手动编写启动脚本方式。

## 背景

在日常项目交付与运维过程中，**启动脚本几乎是每个 Java 应用都会涉及的部分**。然而，在我经手的多个项目中发现，启动脚本的质量普遍参差不齐：

- **结构混乱**：有的只写了最简单的 java -jar，有的则掺杂了各种参数，缺乏统一规范。
- **缺乏通用性**：脚本往往只能在某个项目里使用，迁移到另一个项目就需要大幅修改。
- **可配置性不足**：常见的 JVM 参数、日志路径、配置文件路径等，大多是写死在脚本里的，一旦环境或需求发生变化，就必须直接改脚本。
- **维护成本高**：随着项目增多，启动脚本版本散落在各个仓库，修改一个通用选项（比如日志配置）都要在多个地方重复操作。

这些问题直接导致了：

1. **部署体验不一致** —— 不同项目的启动方式各不相同，新成员需要额外学习。
2. **可观测性不足** —— GC 日志、OOM dump、监控参数等没有统一，排查问题困难。
3. **易出错** —— 手动修改脚本容易引入错误，且在不同环境间切换不灵活。

基于此，我希望能够沉淀出一份 **通用的启动脚本**：

- **通过配置化而不是修改脚本** 来完成参数调整，减少人为改动。
- **集成常用的可选项**，例如 JVM 调优、GC 日志、调试参数、APM 探针、JMX 监控、配置路径管理等。
- **结构规范统一**，让所有项目的启动方式保持一致。
- **可扩展**，能根据不同场景（开发/测试/生产）灵活启用或关闭特性。

这样不仅能大幅降低维护成本，也能提高部署与运维的一致性和可靠性。

## 快速开始

### 基础使用

使用 zeka.stack 框架的项目，在执行 `mvn clean package` 后：

```bash
# 1. 打包项目
mvn clean package

# 2. 解压生成的部署包
tar -xzf target/项目名_时间戳.tar.gz

# 3. 进入解压后的目录
cd 项目名_时间戳/

# 4. 直接启动应用（默认生产环境）
bin/launcher

# 或指定环境启动
bin/launcher -s dev    # 开发环境
bin/launcher -s test   # 测试环境
bin/launcher -s prod   # 生产环境
```

> **注意**：框架已自动处理脚本执行权限，无需手动执行 `chmod +x`

### 常用操作

```bash
# 查看应用状态
bin/launcher -c prod

# 停止应用
bin/launcher -S prod

# 重启应用
bin/launcher -r prod

# 查看日志
bin/launcher -l

# 启动后自动显示日志
bin/launcher -s dev -t
```

## 框架集成

zeka.stack 框架已经内置了完整的插件配置，无需手动添加。框架会在 `package` 阶段自动：

1. **生成启动脚本**：从插件资源中提取 `launcher` 脚本模板
2. **替换配置参数**：根据环境和配置自动替换 JVM 参数等变量
3. **设置执行权限**：自动为脚本设置可执行权限
4. **生成 Docker 构建脚本**：同时生成 `docker-build` 交互式构建脚本

**框架内置配置：**

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-script-maven-plugin</artifactId>
    <version>${arco-maven-plugin.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-server-script</goal>
            </goals>
            <id>generate-server-script</id>
            <phase>package</phase>
        </execution>
    </executions>
</plugin>
```

当我们执行 `mvn clean package` 后, 会生成一个 xxx.tar.gz 的部署包, 解压后具有如下结构:

```
.
├── bin
│   └── launcher
├── config
│   ├── application-default.yml
│   ├── application-dev.yml
│   ├── application-local.yml
│   ├── application.yml
│   ├── build-info.properties
│   └── pom.properties
├── cubo-rest-spring-boot-sample-servlet.jar
└── lib
    ├── blen-kernel-auth-3.0.0-SNAPSHOT.jar
    ...
```

`bin/launcher` 就是此插件生成的启动脚本, 这时启动服务只需要执行:

```bash
bin/launcher -s
```

> 当然我们还可以结合比如 `acro-makeself-maven-plugin`, `arco-container-maven-plugin` 等插件做更多自动化的功能.

## 脚本参数详解

### 核心操作参数

| 参数   | 功能   | 说明                          | 示例                     |
|------|------|-----------------------------|------------------------|
| `-s` | 启动应用 | 指定环境启动应用，如不指定默认为 `prod` 环境  | `bin/launcher -s dev`  |
| `-r` | 重启应用 | 停止后重新启动应用，需要指定环境            | `bin/launcher -r prod` |
| `-S` | 停止应用 | 优雅停止应用，会先发送 TERM 信号，5秒后强制杀死 | `bin/launcher -S test` |
| `-c` | 查看状态 | 查看指定环境下应用的运行状态              | `bin/launcher -c prod` |
| `-l` | 查看日志 | 直接查看日志文件内容（tail -f 方式）      | `bin/launcher -l`      |

### 启动增强参数

| 参数   | 功能           | 说明                    | 示例                       |
|------|--------------|-----------------------|--------------------------|
| `-t` | 启动后显示日志      | 启动后自动 tail 日志，带120秒超时 | `bin/launcher -s dev -t` |
| `-q` | 启动后显示日志（无超时） | 启动后持续显示日志，直到手动中断      | `bin/launcher -s dev -q` |
| `-T` | 日志输出到临时文件    | 将日志输出到临时文件（仅测试用）      | `bin/launcher -s dev -T` |
| `-i` | 显示启动参数       | 输出所有启动参数信息，便于调试       | `bin/launcher -s dev -i` |

### 调试与监控参数

| 参数   | 功能         | 说明                              | 示例                                         |
|------|------------|---------------------------------|--------------------------------------------|
| `-d` | Debug 模式   | 启用远程调试，默认端口5005                 | `bin/launcher -s dev -d 5005`              |
| `-m` | JMX 监控     | 启用 JMX 远程监控，需指定端口               | `bin/launcher -s dev -m 10089`             |
| `-w` | APM 监控     | 启用应用性能监控（需要配置 SkyWalking Agent） | `bin/launcher -s dev -w`                   |
| `-o` | 自定义 JVM 参数 | 覆盖默认的 JVM 启动参数                  | `bin/launcher -s dev -o '-Xms512M -Xmx1G'` |

### 帮助参数

| 参数   | 功能   | 说明             | 示例                |
|------|------|----------------|-------------------|
| `-H` | 显示帮助 | 显示详细的参数说明和使用示例 | `bin/launcher -H` |

### 参数使用规则

> **重要说明：**
> 1. **环境参数必需**：`-s`、`-r`、`-S`、`-c` 后必须指定环境（如 dev/test/prod）
> 2. **组合参数规则**：`-d`、`-t`、`-T`、`-i`、`-w`、`-m`、`-o` 不能单独使用，必须与 `-s` 或 `-r` 组合
> 3. **日志参数互斥**：`-t` 和 `-q` 只能选择一个，`-t` 优先级更高
> 4. **默认行为**：直接执行 `bin/launcher` 等同于 `bin/launcher -r prod`

## 与 cubo-launcher-spring-boot 的集成

`launcher` 脚本与 `cubo-launcher-spring-boot` 模块深度集成，通过启动参数自动配置 Spring Boot 应用：

### 环境变量映射

| 脚本参数      | 对应的 JVM 参数/环境变量          | Spring Boot 配置                | 说明       |
|-----------|--------------------------|-------------------------------|----------|
| `-s dev`  | `-DZEKA_NAME_SPACE=dev`  | `spring.profiles.active=dev`  | 激活开发环境配置 |
| `-s test` | `-DZEKA_NAME_SPACE=test` | `spring.profiles.active=test` | 激活测试环境配置 |
| `-s prod` | `-DZEKA_NAME_SPACE=prod` | `spring.profiles.active=prod` | 激活生产环境配置 |

### 核心集成参数

```bash
# 脚本中自动注入的关键参数
-DAPP_NAME=${APP_NAME}                          # 应用名称
-DIDENTIFY=${APP_NAME}@${ENV}                   # 应用标识（用于进程识别）
-DZEKA_NAME_SPACE=${ENV}                        # 环境命名空间
-Ddeploy.path=${DEPLOY_DIR}                     # 部署路径
-Dconfig.path=${DEPLOY_DIR}/config/             # 配置文件路径
-Dzeka-stack.logging.file.path=${FINAL_LOG_PATH} # 日志文件路径
-Dzeka-stack.logging.file.name=${LOG_NAME}      # 日志文件名
```

### 配置文件加载顺序

脚本启动时，Spring Boot 会按以下顺序加载配置：

1. `${DEPLOY_DIR}/config/application.yml` - 基础配置
2. `${DEPLOY_DIR}/config/application-${ENV}.yml` - 环境特定配置
3. `${DEPLOY_DIR}/config/build-info.properties` - 构建信息

## 自定义配置指南

### 1. JVM 参数配置

在 JDK8 时, 脚本的默认 JVM 参数为:

```bash
${JVM_OPTIONS}
-Xloggc:${GC_LOG}
-XX:ErrorFile=${DEPLOY_DIR}/app_error_%p.log
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=${DEPLOY_DIR}/app_error.hprof
-XX:OnOutOfMemoryError='kill -9 %p'
```

> 其中的 `${JVM_OPTIONS}` 默认为: `-Xms128M -Xmx256M`, 后面可以通过多种方式修改.



目前框架已升级到 JDK17, 所以通用的 JVM 参数做了调整:

```bash
# GC 日志配置（JDK9+ 已弃用 -Xloggc，统一使用 -Xlog）
-Xlog:gc*,safepoint,heap=info:file=${GC_LOG}:time,uptime,level,tags:filecount=10,filesize=50M

# OOM 错误日志
-XX:ErrorFile=${DEPLOY_DIR}/app_error_%p.log

# OOM 时导出堆转储
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=${DEPLOY_DIR}/app_error.hprof

# OOM 时执行动作（JDK9+ 仍支持 OnOutOfMemoryError）
-XX:OnOutOfMemoryError='kill -9 %p'

# 推荐启用的优化（JDK17 默认 G1GC，但建议显式指定）
-XX:+UseG1GC
-XX:+ParallelRefProcEnabled       # 加速弱引用回收
-XX:+AlwaysPreTouch               # 预触发物理内存，避免运行时扩容开销

# 可选调优（看业务需要）
-XX:+ExitOnOutOfMemoryError       # OOM 直接退出（可替代 OnOutOfMemoryError）
-XX:+UseStringDeduplication       # 减少重复字符串内存占用
```

> 在发生 OMM 时, 我们可以通过自定义脚本来生成 hprof, 发送告警信息等操作:
>
> ```bash
> #!/bin/bash
> # OOM 处理脚本
> # 用法: oom_handler.sh <pid>
>
> PID=$1
> LOG_DIR="/opt/app/logs"
> ALERT_URL="https://your-webhook-url"   # 可选：钉钉/企微 Webhook
> HOSTNAME=$(hostname)
> NOW=$(date '+%Y-%m-%d %H:%M:%S')
>
> mkdir -p ${LOG_DIR}
>
> # 1. 记录 OOM 事件日志
> echo "[${NOW}] OOM detected in process ${PID} on ${HOSTNAME}" >> ${LOG_DIR}/oom_events.log
>
> # 2. 如果 JVM 启用了 HeapDumpOnOutOfMemoryError，会自动生成 hprof 文件
> #    这里可以再额外 copy 一份到安全位置
> if [ -f "${LOG_DIR}/app_error.hprof" ]; then
>     cp ${LOG_DIR}/app_error.hprof ${LOG_DIR}/app_error_${PID}_$(date '+%Y%m%d%H%M%S').hprof
> fi
>
> # 3. 发送告警（示例：钉钉/企业微信 Webhook）
> if [ -n "$ALERT_URL" ]; then
>     curl -s -X POST -H "Content-Type: application/json" \
>         -d "{\"msgtype\":\"text\",\"text\":{\"content\":\"[OOM] JVM process ${PID} crashed on ${HOSTNAME} at ${NOW}\"}}" \
>         ${ALERT_URL} >/dev/null 2>&1
> fi
>
> # 4. 延迟 10 秒再杀进程（留点时间让 dump/log 落盘）
> sleep 10
> kill -9 ${PID}
> ```
>
> 只需要修改 `OnOutOfMemoryError`:
>
> ```bash
> -XX:OnOutOfMemoryError="/opt/app/bin/oom_handler.sh %p"
> ```

#### 方式一：通过启动参数（推荐）

```bash
# 临时覆盖 JVM 参数
bin/launcher -s prod -o '-Xms1G -Xmx2G -XX:+UseG1GC'
```

#### 方式二：通过 Maven 配置

```xml
<!-- 在部署模块的 pom.xml 中配置不同环境的 JVM 参数 -->
<properties>
    <jvm.options>-Xms256M -Xmx512M</jvm.options>
    <prod.jvm.options>-Xms1G -Xmx2G -XX:+UseG1GC</prod.jvm.options>
</properties>

<!-- 生产环境打包时使用(不建议这种方式) -->
<build>
    <plugins>
        <plugin>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-script-maven-plugin</artifactId>
            <configuration>
                <jvmOptions>${jvm.options}</jvmOptions>
            </configuration>
        </plugin>
    </plugins>
</build>
```

#### 方式三：通过自定义脚本

```bash
# 在项目根目录创建 bin/launcher，框架会优先使用
mkdir -p bin
cp target/arco-maven-plugin/bin/launcher bin/launcher
# 编辑 bin/launcher，修改 JVM_OPTIONS 变量
```

### 2. 日志配置

#### 日志路径配置

```bash
# 方式一：通过环境变量
export FINAL_LOG_PATH="/var/logs/myapp"
bin/launcher -s prod

# 方式二：通过系统属性
bin/launcher -s prod -o '-DFINAL_LOG_PATH=/var/logs/myapp'
```

#### 日志文件名配置

```bash
# 修改日志文件名
export LOG_NAME="application.log"
bin/launcher -s prod
```

#### 集中日志目录（推荐生产环境）

```bash
# 使用 zeka.stack 推荐的日志目录结构
export LOG_PATH="/mnt/syslogs/zeka.stack"
export FINAL_LOG_PATH=""  # 清空此变量，使用 LOG_PATH
bin/launcher -s prod

# 最终日志路径：/mnt/syslogs/zeka.stack/prod/应用名/all.log
```

### 3. 监控与调试配置

#### Debug 模式配置

```bash
# 开发环境启用 Debug
bin/launcher -s dev -d 5005

# 生产环境调试（谨慎使用）
bin/launcher -s prod -d 8000
```

#### JMX 监控配置

```bash
# 启用 JMX 监控
bin/launcher -s prod -m 10089

# 使用 JConsole 连接：localhost:10089
```

#### APM 监控配置

```bash
# 1. 安装 SkyWalking Agent 到 /opt/skywalking/agent/
# 2. 启用 APM 监控
bin/launcher -s prod -w

# APM 配置会自动注入：
# -javaagent:/opt/skywalking/agent/skywalking-agent.jar
# -Dskywalking.agent.service_name=应用名@环境
```

### 4. 自定义启动脚本

#### 完全覆盖启动脚本

框架支持完全自定义启动脚本，原理是 **约定大于配置**：

**工作机制**：

1. 框架会自动检测项目根目录下是否存在 `bin/launcher` 文件
2. 如果存在，直接将此文件复制到 `target/arco-maven-plugin/bin/launcher`
3. 如果不存在，使用框架内置的默认脚本模板

**创建自定义脚本**：

```bash
# 在项目根目录创建自定义脚本
mkdir -p bin

# 方式一：从模板复制后修改
cp target/arco-maven-plugin/bin/launcher bin/launcher
# 然后编辑 bin/launcher，根据需要修改

# 方式二：完全自定义
cat > bin/launcher << 'EOF'
#!/bin/bash
# 完全自定义的启动脚本
echo "使用项目自定义启动脚本"

# 你的自定义逻辑...
# 环境检查、依赖检查、特殊配置等

# 示例：启动 Java 应用
java -jar myapp.jar
EOF

chmod +x bin/launcher
```

**注意事项**：

- 自定义脚本会完全替代框架默认脚本
- 需要自己处理所有启动逻辑（环境变量、日志、进程管理等）
- **建议基于框架模板进行修改，而非完全重写**

#### 部分定制（推荐）

如果只需要调整特定参数，建议使用以下方式：

**方式一：环境变量覆盖**

```bash
# 覆盖默认配置
export JVM_OPTIONS="-Xms512M -Xmx1G"
export FINAL_LOG_PATH="/var/logs/myapp"
bin/launcher -s prod
```

**方式二：启动参数覆盖**

```bash
# 使用 -o 参数临时覆盖 JVM 配置
bin/launcher -s prod -o '-Xms1G -Xmx2G -XX:+UseG1GC'
```

### 5. 环境特定配置

#### 开发环境配置

```bash
# 开发环境通常使用较小的内存配置
bin/launcher -s dev -o '-Xms128M -Xmx256M' -t
```

> 默认为 `-Xms128M -Xmx256M`, 所以可以简写为:
>
> ```bash
> bin/launcher -s dev -t
> ```

#### 测试环境配置

```bash
# 测试环境启用更多调试信息
bin/launcher -s test -o '-Xms256M -Xmx512M' -i
```

#### 生产环境配置

```bash
# 生产环境使用优化的 JVM 参数
bin/launcher -s prod -o '-Xms2G -Xmx4G -XX:MaxGCPauseMillis=200'
```

## 技术实现细节

### 插件实现逻辑

这个插件的实现并不复杂, 不到 100 行代码:

```java
@Mojo(name = "generate-server-script", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateServerScriptMojo extends ZekaMavenPluginAbstractMojo {

    /** Skip */
    @Parameter(property = Plugins.SKIP_LAUNCH_SCRIPT, defaultValue = Plugins.TURN_OFF_PLUGIN)
    private boolean skip;
    /** Output file */
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/bin/launcher")
    private File outputFile;
    @Parameter(defaultValue = "${project.build.directory}/arco-maven-plugin/bin/docker-build")
    private File dockerBuildFile;
    /** 自定义的脚本文件 */
    @Parameter(defaultValue = "${project.basedir}/bin/launcher")
    private File scriptFile;
    /** jvm 参数 */
    @Parameter(property = "jvmOptions", defaultValue = "-Xms128M -Xmx256M")
    private String jvmOptions;
    /** JVM_SYMBOL */
    private static final String JVM_SYMBOL = "#{jvmOptions}";
    /** SERVER_FILE */
    private static final String SERVER_FILE = "META-INF/bin/launcher";
    private static final String DOCKER_BUILD_FILE = "META-INF/bin/docker-build";

    @SneakyThrows
    @Override
    public void execute() {
        if (this.skip) {
            this.getLog().info("arco-script-maven-plugin is skipped");
            return;
        }

        // 将插件包中的 docker-build 写入到业务项目的 target/arco-maven-plugin/bin/docker-build
        new FileWriter(this.dockerBuildFile).write(DOCKER_BUILD_FILE);

        // 存在自定义脚本则将自定义脚本写入到 outputFile
        if (this.scriptFile.exists()) {
            new FileWriter(this.outputFile).write(this.scriptFile);
            this.getLog().info("使用自定义 launcher: " + this.scriptFile.getPath());
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
```

1. 判断 `launch.script.skip` 是否为 true, 默认开启插件, 如果需要手动关闭, 可以添加 mvn 参数: `-Dlaunch.script.skip=true`;
2. 判断在指定目录下(`bin/`)是否存在自定义脚本, 存在就将自定义脚本拷贝到 `target/arco-maven-plugin/bin/`, 否则使用框架提供的脚本;
3. JVM 参数处理, 会读取 pom.xml 中的配置, 然后替换脚本中的 `#{jvmOptions}` 占位符;

---

### launcher 脚本功能模块详解

为了便于用户进行自定义，以下详细说明脚本中的每个功能模块：

#### 1. 变量定义

**作用**：定义脚本运行所需的全局变量和默认配置

**关键变量**：

```bash
ENV=${ENV:-"prod"}                    # 默认环境为生产环境
FUNC="restart"                        # 默认操作为重启
DEBUG_PORD="-1"                      # Debug端口，-1表示关闭
JMX_PORD="-1"                        # JMX端口，-1表示关闭
SHOW_LOG="off"                       # 是否显示日志
TIMEOUT_SHOWLOG="off"                # 是否使用超时显示日志
SHOW_INFO="on"                       # 是否显示启动信息
ENABLE_APM="off"                     # 是否启用APM监控
LOG_PATH="/mnt/syslogs/zeka.stack"   # 集中日志目录（可选）
FINAL_LOG_PATH="./logs"              # 最终日志路径
LOG_NAME="all.log"                   # 日志文件名
JVM_OPTIONS="-Xms128M -Xmx256M "     # 默认JVM参数
```

**自定义方法**：通过环境变量覆盖默认值

#### 2. 工具函数

**作用**：提供美观的输出和日志查看功能

**核心函数**：

- `print_title()`：标题输出，带颜色格式
- `print_success/error/warn/info()`：各种状态的彩色输出
- `logview()`：直接查看日志文件内容（tail -f）

**自定义要点**：修改这些函数可以改变脚本的输出样式

#### 3. 核心功能函数

##### 3.1 Java环境检测 (`detect_java_exe`)

**作用**：自动查找可用的Java可执行文件

**查找顺序**：

1. `$JAVA_HOME/bin/java`
2. `which java`
3. `/usr/bin/java`
4. 系统路径查找：`/opt /usr/local /usr/lib/jvm`

**自定义方法**：设置 `JAVA_HOME` 环境变量

##### 3.2 Debug配置初始化 (`init_debug`)

**作用**：配置远程调试参数

**生成参数**：

```bash
-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:端口
```

**自定义方法**：通过 `-d 端口` 参数启用

##### 3.3 JMX配置初始化 (`init_jmx`)

**作用**：配置JMX远程监控参数

**生成参数**：

```bash
-Dcom.sun.management.jmxremote=true
-Dcom.sun.management.jmxremote.port=端口
-Dcom.sun.management.jmxremote.ssl=false
-Dcom.sun.management.jmxremote.authenticate=false
-Djava.rmi.server.hostname=本机IP
```

**自定义方法**：通过 `-m 端口` 参数启用

##### 3.4 应用环境准备 (`prepare`)

**作用**：解析部署路径、应用名、JAR包路径，创建日志目录

**关键逻辑**：

- 自动检测脚本所在目录作为部署路径
- 从 `config/build-info.properties` 读取应用名
- 构造JAR文件路径：`${DEPLOY_DIR}/${APP_NAME}.jar`
- 创建日志目录结构

**自定义方法**：修改 `build-info.properties` 或环境变量

##### 3.5 APM配置初始化 (`init_apm`)

**作用**：配置SkyWalking APM监控

**生成参数**：

```bash
-javaagent:/opt/skywalking/agent/skywalking-agent.jar
-Dskywalking.agent.service_name=${APP_NAME}@${ENV}
```

**自定义方法**：通过 `-w` 参数启用，需要预先安装SkyWalking Agent

##### 3.6 进程检测 (`check_pid`)

**作用**：使用进程标识查找应用进程

**标识格式**：`${APP_NAME}@${ENV}`

**自定义方法**：确保进程启动时包含此标识

##### 3.7 日志目录创建 (`mkdir_log_file`)

**作用**：智能创建日志目录

**逻辑**：

- 如果 `FINAL_LOG_PATH` 为空，使用 `${LOG_PATH}/${ENV}/${APP_NAME}`
- 创建目录并确保日志文件存在

**自定义方法**：设置 `LOG_PATH` 或 `FINAL_LOG_PATH` 环境变量

##### 3.8 应用启动 (`running`)

**作用**：构造并执行Java启动命令

**完整启动命令结构**：

```bash
nohup java -jar \
  -Djava.security.egd=file:/dev/./urandom \              # 优化随机数生成，避免阻塞（常见于 Tomcat、Spring Boot 启动慢）
  ${JVM_OPTIONS} \                                       # 通用 JVM 参数（比如内存大小、GC 配置等，可由外部统一传入）
  -Xlog:gc*,safepoint,heap=info:file=${GC_LOG}:time,uptime,level,tags:filecount=10,filesize=50M \
                                                        # GC 日志（JDK9+ 新方式），包含 GC、safepoint、堆信息
                                                        # 输出到 ${GC_LOG}，日志按 50M 滚动，最多保留 10 个
  -XX:ErrorFile=${DEPLOY_DIR}/app_error_%p.log \        # JVM 致命错误日志（%p 表示进程号）
  -XX:+HeapDumpOnOutOfMemoryError \                     # OOM 时自动生成 heap dump
  -XX:HeapDumpPath=${DEPLOY_DIR}/app_error.hprof \      # Heap dump 输出路径
  -XX:OnOutOfMemoryError='kill -9 %p' \                 # OOM 时执行命令，这里是强制 kill JVM 进程
  -XX:+UseG1GC \                                        # 使用 G1 垃圾收集器（JDK17 默认，但显式声明更稳妥）
  -XX:+ParallelRefProcEnabled \                         # 弱引用/软引用并行处理，加快回收速度
  -XX:+AlwaysPreTouch \                                 # JVM 启动时预先分配并触碰内存，避免运行时扩容卡顿
  -XX:+UseStringDeduplication \                         # 启用字符串去重，减少堆中重复字符串占用
  -Dloader.home=${DEPLOY_DIR}/ \                        # Spring Boot Loader 的 home 路径（类加载根目录）
  -Dloader.path=lib/ \                                  # Spring Boot Loader 额外依赖路径（lib 目录下的 jar 会被加载）
  -DAPP_NAME=${APP_NAME} \                              # 应用名（自定义，用于标识进程）
  -DIDENTIFY=${APP_NAME}@${ENV} \                       # 应用唯一标识（应用名 + 环境）
  -DZEKA_NAME_SPACE=${ENV} \                            # 环境命名空间（自定义，用于区分环境）
  -Ddeploy.path=${DEPLOY_DIR} \                         # 应用部署路径
  -Dstart.type=shell \                                  # 启动方式标识（自定义，shell 脚本启动）
  -Dconfig.path=${DEPLOY_DIR}/config/ \                 # 配置文件路径
  -Dzeka-stack.logging.file.path=${FINAL_LOG_PATH} \    # 日志输出目录（应用日志）
  -Dzeka-stack.logging.file.name=${LOG_NAME} \          # 日志文件名（应用日志）
  -Djar.file=${JAR_FILE} \                              # 当前运行的 JAR 包路径
  ${JMX_OPTIONS} ${DEBUG_OPTS} ${APM_OPTS} \            # 可选参数：JMX 监控、远程调试、APM 探针
  ${JAR_FILE} \                                         # 启动的目标 JAR 文件
  --spring.profiles.active=${ENV} \                     # Spring 环境（dev/test/prod 等）
  --spring.config.location=${DEPLOY_DIR}/config/ \      # Spring Boot 配置文件加载路径
  --slot.root=${DEPLOY_DIR}/ \                          # 插件框架根目录（自定义）
  --slot.path=patch/ --slot.path=plugin/ \              # 插件加载路径（支持热修复/插件扩展）
  >${FINAL_LOG_PATH}/${LOG_NAME} 2>&1 &                 # 日志输出重定向（标准输出和错误输出都写入日志文件）
```

**自定义要点**：

- 修改 `JVM_OPTIONS` 可调整内存和GC参数
- 修改路径相关变量可调整文件位置
- 修改Spring Boot参数可调整应用配置

##### 3.9 应用控制函数

- `start()`：启动应用，支持日志显示
- `stop()`：优雅停止应用（TERM信号 → 5秒等待 → KILL信号）
- `restart()`：重启应用（stop + start）
- `status()`：查看应用运行状态

#### 4. 参数解析

**作用**：解析命令行参数，设置全局变量

**参数映射**：

```bash
-s env  → ENV=env, FUNC="start"      # 启动
-r env  → ENV=env, FUNC="restart"    # 重启
-S env  → ENV=env, FUNC="stop"       # 停止
-c env  → ENV=env, FUNC="status"     # 状态
-l      → FUNC="logview"             # 查看日志
-d port → DEBUG_PORD=port            # Debug端口
-m port → JMX_PORD=port             # JMX端口
-t      → SHOW_LOG="on"             # 显示日志
-q      → TIMEOUT_SHOWLOG="on"      # 超时显示日志
-T      → SHOW_LOG="on", 临时日志目录  # 临时日志
-o opts → JVM_OPTIONS=opts          # 覆盖JVM参数
-i      → SHOW_INFO="on"            # 显示信息
-w      → ENABLE_APM="on"           # 启用APM
-H      → usage                     # 显示帮助
```

#### 5. 主流程控制

**执行顺序**：

1. 显示启动横幅
2. 检查系统依赖（awk, pgrep, mktemp）
3. 解析命令行参数
4. 检测Java环境
5. 初始化Debug/JMX参数
6. 准备应用环境（路径、日志等）
7. 初始化APM参数
8. 根据功能参数执行对应操作

### 脚本自定义建议

1. **轻量级自定义**：通过环境变量或启动参数调整
2. **中等自定义**：复制默认脚本后修改特定函数
3. **深度自定义**：完全重写脚本，保持接口兼容性

### 脚本工作原理总结

1. **环境检测**：自动检测 Java 环境和必要的系统命令
2. **配置解析**：从 `config/build-info.properties` 读取应用信息
3. **进程管理**：使用 `pgrep` 查找进程，支持优雅停止和强制杀死
4. **日志管理**：自动创建日志目录，支持多种日志输出方式
5. **参数注入**：将启动参数转换为 JVM 系统属性和 Spring Boot 配置

### 关键环境变量

| 变量名              | 作用     | 默认值                        | 示例                       |
|------------------|--------|----------------------------|--------------------------|
| `APP_NAME`       | 应用名称   | 从 build-info.properties 读取 | `user-service`           |
| `DEPLOY_DIR`     | 部署目录   | 脚本所在目录的父目录                 | `/opt/apps/user-service` |
| `FINAL_LOG_PATH` | 日志目录   | `./logs`                   | `/var/logs/user-service` |
| `LOG_NAME`       | 日志文件名  | `all.log`                  | `application.log`        |
| `JVM_OPTIONS`    | JVM 参数 | `-Xms128M -Xmx256M`        | `-Xms1G -Xmx2G`          |

### 进程识别机制

脚本使用 `${APP_NAME}@${ENV}` 作为进程标识，确保：

- 同一应用的不同环境实例可以同时运行
- 进程查找和管理的准确性
- 支持多实例部署

### 依赖命令说明

| 命令        | 用途   | macOS 替代方案                           |
|-----------|------|--------------------------------------|
| `awk`     | 文本处理 | 系统自带                                 |
| `pgrep`   | 进程查找 | 系统自带                                 |
| `mktemp`  | 临时文件 | 系统自带                                 |
| `timeout` | 命令超时 | `brew install coreutils && gtimeout` |

## 故障排除

### 常见问题

1. **启动失败**
    - 检查 Java 环境：`java -version`
    - 检查应用 JAR 文件是否存在
    - 查看日志文件：`bin/launcher -l`

2. **端口冲突**
    - 检查端口占用：`lsof -i :8080`
    - 修改应用端口配置

3. **权限问题**
    - 确保启动脚本有执行权限
    - 检查日志目录写入权限

4. **环境变量问题**
    - 使用 `-i` 参数查看所有启动参数
    - 检查环境特定的配置文件

### 调试技巧

```bash
# 查看完整的启动命令和参数
bin/launcher -s dev -i

# 启动后立即查看日志
bin/launcher -s dev -t

# 在临时目录查看日志输出
bin/launcher -s dev -T
```

## 总结

`arco-script-maven-plugin` 的核心设计理念是 **"约定大于配置"**，这一理念体现在以下几个层面：

### 1. 开箱即用的默认体验

**框架约定**：使用 zeka.stack 框架的项目，无需任何额外配置，执行 `mvn clean package` 后即可获得功能完整的启动脚本。

**智能默认值**：

- 默认 JVM 参数：`-Xms128M -Xmx256M`（适合开发和小型应用）
- 默认环境：`prod`（生产环境优先，确保安全）
- 默认日志路径：`./logs`（项目本地目录）
- 默认操作：重启应用（最常用的操作）

这种设计让使用者在 **没有特殊要求的情况下**，可以直接使用框架提供的标准化脚本，享受到完整的应用生命周期管理功能。

### 2. 渐进式自定义能力

当默认配置无法满足使用者需求时，框架提供了多层次的自定义能力：

**轻量级定制**（推荐）：

```bash
# 通过启动参数临时调整
bin/launcher -s prod -o '-Xms1G -Xmx2G' -m 10089 -w

# 通过环境变量覆盖默认值
export JVM_OPTIONS="-Xms512M -Xmx1G"
export FINAL_LOG_PATH="/var/logs/myapp"
bin/launcher -s prod
```

**中等定制**：

```xml
<!-- 通过 Maven 配置不同环境的参数 -->
<properties>
    <jvm.options>-Xms256M -Xmx512M</jvm.options>
    <prod.jvm.options>-Xms2G -Xmx4G -XX:+UseG1GC</prod.jvm.options>
</properties>
```

**完全定制**：

```bash
# 项目根目录创建自定义脚本
mkdir -p bin
cp target/arco-maven-plugin/bin/launcher bin/launcher
# 编辑 bin/launcher 进行深度定制
```

### 3. 智能检测与自动适配

框架会自动检测项目状态并做出决策：

- **自定义脚本检测**：如果项目根目录存在 `bin/launcher`，自动使用自定义脚本
- **环境自动适配**：根据启动参数自动加载对应环境的配置文件
- **Java 环境检测**：按优先级自动查找可用的 Java 可执行文件
- **应用信息解析**：从 `build-info.properties` 自动读取应用名和版本信息

### 4. 配置化而非脚本化

传统方式需要手动编写和维护启动脚本，而 `arco-script-maven-plugin` 强调 **"通过配置而不是修改脚本"** 来实现定制化：

**传统方式**（不推荐）：

```bash
# 每个项目都要手写启动脚本
echo 'java -jar -Xms1G -Xmx2G myapp.jar' > start.sh
chmod +x start.sh
```

**框架方式**（推荐）：

```bash
# 通过参数配置，脚本自动生成
bin/launcher -s prod -o '-Xms1G -Xmx2G'
```

### 5. 约定带来的价值

**统一性**：所有使用 zeka.stack 框架的项目都拥有相同的启动方式，降低学习成本。

**可靠性**：框架内置的脚本经过充分测试，包含生产环境必需的监控、日志、错误处理机制。

**可维护性**：当需要升级启动脚本（如支持新的 JDK 版本）时，只需升级插件版本，所有项目自动受益。

**可扩展性**：在遵循约定的基础上，提供完全的自定义能力，满足特殊场景需求。

