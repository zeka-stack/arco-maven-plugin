# Arco Container Maven Plugin

一个专为 Spring Boot 应用程序设计的 Maven 插件，用于自动化生成 Docker 容器化配置文件和优化的镜像构建策略。

## 📋 模块简介

`arco-container-maven-plugin` 是 Arco Maven Plugin 系列中的容器化插件，它能够：

- 🐳 **自动生成 Dockerfile** - 根据项目配置智能生成优化的 Dockerfile
- 📦 **创建 Assembly 配置** - 生成 Maven Assembly 打包配置，实现分层构建
- 🚀 **多策略容器化** - 提供多种容器化策略，满足不同场景需求
- ⚡ **层缓存优化** - 采用分层构建策略，最大化 Docker 层缓存效率
- 🔍 **智能端口检测** - 自动读取应用配置文件，识别需要暴露的端口
- 💊 **健康检查集成** - 自动配置容器健康检查机制

该插件遵循 Docker 最佳实践，提供开箱即用的容器化解决方案，显著简化 Spring Boot 应用的容器化部署流程。

## ✨ 主要功能

### 1. 智能 Dockerfile 生成

插件提供三种不同的 Dockerfile 模板：

- **Dockerfile-B**: 基础镜像模板
    - 基于 Eclipse Temurin JRE 17
    - 集成 busybox 工具链
    - 优化时区和软件源配置

- **Dockerfile-M**: 多层构建模板（推荐）
    - 分离依赖层和应用层
    - 最大化 Docker 层缓存效率
    - 适合频繁更新的应用

- **Dockerfile-S**: 单层构建模板
    - 简单直接的构建方式
    - 适合一次性部署场景

### 2. 自动化 Assembly 配置

- **lib.xml**: 依赖库打包配置
    - 分离第三方依赖
    - 支持依赖排除规则
    - 优化构建缓存

- **app.xml**: 应用文件打包配置
    - 配置文件处理
    - 启动脚本集成
    - 主 jar 包管理

### 3. 智能配置解析

- 自动读取 `application.yml` 配置
- 解析 `zeka-stack.docker.export` 端口配置
- 生成对应的 EXPOSE 指令和健康检查
- 支持多端口暴露

### 4. Docker Compose 集成

- 自动生成 `docker-compose.yml`
- 配置服务依赖关系
- 集成健康检查和重启策略
- 支持环境变量配置

## 🚀 使用说明

### 插件配置

在项目的 `pom.xml` 中添加插件配置：

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-container-maven-plugin</artifactId>
    <version>${arco.version}</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-dockerfile</goal>
                <goal>generate-docker-assembly-config</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 应用配置

在 `application.yml` 中配置端口信息：

```yaml
# 基础配置
server:
  port: 8080

# 容器化配置
zeka-stack:
  docker:
    export:
      - "8080"    # 主服务端口
      - "8443"    # HTTPS 端口（可选）
      - "9090"    # 管理端口（可选）
```

### 启用插件

> **注意**: 该插件默认是跳过执行的（`dockerfile.skip=true`），需要手动开启。

#### 单层构建模式（简单场景）

适用于快速构建，不会使用到 Docker 分层缓存：

```bash
# 开启插件并执行 Maven 打包（单层构建）
mvn clean package -Ddockerfile.skip=false

# 单独执行插件目标
mvn arco-container:generate-dockerfile -Ddockerfile.skip=false
mvn arco-container:generate-docker-assembly-config -Ddockerfile.skip=false
```

#### 多层构建模式（推荐）

为了充分利用 Docker 分层缓存，框架层提供了专门的 `docker` Profile 配置，该配置会：

- 自动开启容器化插件（`dockerfile.skip=false`）
- 配置分层打包策略（lib 层 + app 层）
- 生成优化的构建产物

```bash
# 开启多层构建模式
mvn clean package -Pdocker

# 等价于
mvn clean package -Ddockerfile.skip=false -Pdocker
```

#### 两种模式对比

| 特性             | 单层构建                                  | 多层构建                                        |
|----------------|---------------------------------------|---------------------------------------------|
| **命令**         | `mvn package -Ddockerfile.skip=false` | `mvn package -Pdocker`                      |
| **生成文件**       | `target/app.tar.gz`                   | `target/docker/lib/` + `target/docker/app/` |
| **Dockerfile** | Dockerfile-S                          | Dockerfile-M                                |
| **缓存效率**       | 普通                                    | 高效（分层缓存）                                    |
| **构建速度**       | 正常                                    | 更快（后续构建）                                    |
| **适用场景**       | 简单部署                                  | 频繁更新的生产环境                                   |

### 构建产物

#### 单层构建模式产物

使用 `mvn package -Ddockerfile.skip=false` 后生成：

```
target/
├── docker/
│   ├── Dockerfile-S        # 单层构建 Dockerfile
│   ├── Dockerfile-M        # 多层构建 Dockerfile
│   ├── Dockerfile-B        # 基础镜像 Dockerfile
│   └── docker-compose.yml  # Docker Compose 配置
├── arco-maven-plugin/
│   └── assembly/
│       ├── app.xml         # 应用文件打包配置
│       └── lib.xml         # 依赖库打包配置
├── your-app.tar.gz         # 单层打包产物
└── classes/
```

#### 多层构建模式产物

使用 `mvn package -Pdocker` 后生成：

```
target/
├── docker/
│   ├── Dockerfile-S        # 单层构建 Dockerfile
│   ├── Dockerfile-M        # 多层构建 Dockerfile
│   ├── Dockerfile-B        # 基础镜像 Dockerfile
│   ├── docker-compose.yml  # Docker Compose 配置
│   └── your-app/           # 应用层目录
│   		├── lib/            # 依赖层目录
│       ├── config/         # 配置文件
│       ├── patch/          # 补丁目录
│       ├── plugin/         # 插件目录
│       └── your-app.jar    # 主应用 jar
├── arco-maven-plugin/
│   └── assembly/
│       ├── app.xml         # 应用文件打包配置
│       └── lib.xml         # 依赖库打包配置
├── your-app.tar.gz         # 完整打包产物（兼容单层）
└── classes/
```

> **说明**: 多层构建模式同时生成单层和多层两种产物，提供更大的灵活性。

### Docker 镜像构建

#### 1. 构建基础镜像

> ```
> 在 target 同级目录执行
> ```

```bash
docker build -f target/docker/Dockerfile-B -t zeka-stack-java17-base:latest .
```

#### 2. 构建应用镜像

> **重要**: 根据是否存在自定义 Docker 配置，有两种不同的构建方式

##### 方式一：使用插件生成的 Dockerfile（推荐）

当项目根目录下**没有** `docker/` 目录时，插件会在 `target/docker/` 下生成 Dockerfile。

> 在 target 同级目录下(根目录)执行

```bash
# 或使用单层构建
docker build -f target/docker/Dockerfile-S -t your-app:latest target

# 使用多层构建（推荐）
docker build -f target/docker/Dockerfile-M -t your-app:latest target
```

生成的 Dockerfile 内容示例(单层)：

```dockerfile
# 使用 dockerfile-base 构建的基础镜像
FROM zeka-stack-java17-base:latest

# 添加构建元信息
LABEL maintainer="dong4j <dong4j@gmail.com>"
LABEL project="zeka.stack"
LABEL description="基于 JRE 17 的 Spring Boot 应用容器镜像"

# 定义应用启动方式
ENV START_TYPE=docker
# 创建工作目录
WORKDIR /app
# 拷贝并解压部署包（直接使用当前目录的 tar 包）
ADD your-app.tar.gz /app/
# 进入部署目录
WORKDIR /app/your-app
# 暴露端口
EXPOSE 8080
# 健康检查
HEALTHCHECK --interval=30s --timeout=5s CMD curl -f http://localhost:8080/actuator/health || exit 1
# 启动命令
ENTRYPOINT ["bin/launcher"]
```

##### 方式二：使用自定义 Dockerfile

当项目根目录下**存在** `docker/` 目录时，用户通常是拷贝了插件生成的 Dockerfile 进行自定义。

> 在 target 同级目录下(根目录)执行

```bash
# 单层构建
docker build -f docker/Dockerfile-S -t your-app:latest target

# 或使用多层构建
docker build -f docker/Dockerfile-M -t your-app:latest target
```

#### 3. 多平台构建

> 在 target 同级目录下(根目录)执行

```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t your-registry/your-app:latest \
  -f Dockerfile-M target \
  --push
```

### 🤖 一键式构建脚本（推荐）

为了简化使用流程，我们提供了交互式的构建脚本 `docker-build.sh`：

#### 快速使用

```bash
# 在项目根目录执行
./docker-build.sh
```

#### 脚本特性

- 🎯 **交互式选择**: 支持单层/多层构建模式选择
- 🎮 **多种执行方式**: 仅构建镜像 or 一键运行
- 🔍 **智能检测**: 自动检测项目配置和 Docker 目录
- 🖥️ **友好界面**: 彩色输出，步骤清晰
- 🛡️ **错误处理**: 完善的错误提示和中断处理

#### 功能演示

```
╔══════════════════════════════════════════════════════════════╗
║    🐳 Arco Container Maven Plugin - Docker Build Script    ║
║    交互式 Docker 构建和部署工具                               ║
╚══════════════════════════════════════════════════════════════╝

请选择构建模式：
  1) 单层构建模式 (简单快速)
  2) 多层构建模式 (推荐)

请选择执行模式：
  1) 仅构建镜像 (Build Only)
  2) 一键运行 (Build & Run)
```

> 📖 详细使用说明请参考：[docker_build_guide.md](./docker_build_guide.md)

### 高级配置

#### Docker Profile 配置详解

框架层提供了完整的 `docker` Profile 配置，用于支持多层构建。该配置包含：

**核心特性**：

- 自动开启容器化插件（`dockerfile.skip=false`）
- 配置 `maven-antrun-plugin` 创建必要的目录结构
- 重写 `maven-assembly-plugin` 配置实现分层打包
- 生成 lib 层和 app 层的独立构建产物

**Profile 配置示例**：

```xml
<profiles>
    <profile>
        <id>docker</id>
        <properties>
            <!-- 自动开启容器化插件 -->
            <dockerfile.skip>false</dockerfile.skip>
        </properties>
        <build>
            <pluginManagement>
                <plugins>
                    <!-- 创建必要的目录结构 -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-antrun-plugin</artifactId>
                        <executions>
                            <execution>
                                <id>generate-patch-and-plugin-dir</id>
                                <phase>generate-resources</phase>
                                <configuration>
                                    <target>
                                        <mkdir dir="${project.build.directory}/docker/${project.build.finalName}/patch"/>
                                        <mkdir dir="${project.build.directory}/docker/${project.build.finalName}/plugin"/>
                                    </target>
                                </configuration>
                                <goals>
                                    <goal>run</goal>
                                </goals>
                            </execution>
                        </executions>
                    </plugin>

                    <!-- 分层打包配置 -->
                    <plugin>
                        <groupId>org.apache.maven.plugins</groupId>
                        <artifactId>maven-assembly-plugin</artifactId>
                        <executions>
                            <!-- 完整打包（兼容单层） -->
                            <execution>
                                <id>make-assembly</id>
                                <phase>package</phase>
                                <goals>
                                    <goal>single</goal>
                                </goals>
                            </execution>

                            <!-- lib 层：第三方依赖 -->
                            <execution>
                                <id>lib-layer</id>
                                <phase>package</phase>
                                <goals>
                                    <goal>single</goal>
                                </goals>
                                <configuration>
                                    <descriptors>
                                        <descriptor>${project.build.directory}/arco-maven-plugin/assembly/lib.xml</descriptor>
                                    </descriptors>
                                    <outputDirectory>${project.build.directory}/docker</outputDirectory>
                                </configuration>
                            </execution>

                            <!-- app 层：应用文件 -->
                            <execution>
                                <id>app-layer</id>
                                <phase>package</phase>
                                <goals>
                                    <goal>single</goal>
                                </goals>
                                <configuration>
                                    <descriptors>
                                        <descriptor>${project.build.directory}/arco-maven-plugin/assembly/app.xml</descriptor>
                                    </descriptors>
                                    <outputDirectory>${project.build.directory}/docker</outputDirectory>
                                </configuration>
                            </execution>
                        </executions>
                    </plugin>
                </plugins>
            </pluginManagement>
        </build>
    </profile>
</profiles>
```

**使用场景**：

- ✅ 生产环境部署（推荐）
- ✅ 频繁迭代的开发环境
- ✅ 需要优化构建速度的场景
- ✅ 大型应用（依赖较多）

#### 自定义 Dockerfile

如果需要自定义 Dockerfile，在项目根目录创建 `docker/` 目录：

```dockerfile
# 项目根目录/docker/Dockerfile-S
FROM your-custom-base:latest

# 如果修改 ADD 指令的路径, 就是修改 build 的 context(target 同级目录执行): docker build -f docker/Dockerfile-S -t your-app:latest .
ADD target/your-app.tar.gz /app/
# 如果不修改 ADD 指令的路径, 构建命令为(target 同级目录执行): docker build -f docker/Dockerfile-S -t your-app:latest target

# 其他自定义配置...
WORKDIR /app/your-app
ENTRYPOINT ["bin/launcher"]
```

**重要注意事项**：

1. 插件检测到 `docker/` 目录存在时，会使用自定义的 Dockerfile
2. 自定义 Dockerfile 中的 `ADD` 指令使用 `target/` 路径前缀时需要修改 build 命令中的 context
3. 构建时需要在项目根目录执行，而不是 `target` 目录(不同层级执行 build 时需要修改 context)

**快速创建自定义配置**：

```bash
# 先让插件生成模板
mvn clean package -Ddockerfile.skip=false

# 拷贝生成的配置到项目根目录
cp -r target/docker ./

# 执行 build 构建镜像
 docker build -f docker/Dockerfile-S -t your-app:latest target
```

#### 自定义 Assembly 配置

在项目根目录创建 `assembly/` 目录：

```
assembly/
├── lib.xml    # 自定义依赖打包配置
└── app.xml    # 自定义应用打包配置
```

#### 控制插件执行

##### 单层构建

```bash
# 方式1: 单层构建模式
mvn clean package -Ddockerfile.skip=false

# 方式1: 通过 Maven 属性配置(单层构建)
<properties>
    <!-- 开启插件 -->
    <dockerfile.skip>false</dockerfile.skip>
</properties>
```

##### 多层构建

```bash
# 方式1: 多层构建模式（推荐，自动开启插件）
mvn clean package -Pdocker

# 方式2: 在 IDEA 中勾选启用 docker profile(多层构建)
```

**推荐用法**：

- 🚀 **生产环境**: `mvn package -Pdocker`（多层构建，充分利用缓存）
- 🔧 **开发测试**: `mvn package -Ddockerfile.skip=false`（单层构建，简单快速）
- 📦 **CI/CD**: `mvn package -Pdocker`（优化构建时间）

## 🔧 原理和实现细节

### 架构设计

插件采用 Maven Mojo 架构，包含两个核心目标：

1. **generate-dockerfile** (`GenerateDockerfileScriptMojo`)
2. **generate-docker-assembly-config** (`GenerateDockerAssemblyConfigFileMojo`)

### 核心实现原理

#### 1. Dockerfile 生成流程

```java
@Mojo(name = "generate-dockerfile", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateDockerfileScriptMojo extends ZekaMavenPluginAbstractMojo {
    // 核心逻辑
}
```

**执行流程**：

1. 检查是否存在自定义 Dockerfile (`${project.basedir}/docker/Dockerfile`)
2. 如果存在，直接复制到输出目录
3. 如果不存在，使用内置模板生成：
    - 读取 `application.yml` 解析端口配置
    - 使用 SnakeYAML 和 Jackson 解析配置文件
    - 生成 EXPOSE 指令和健康检查配置
    - 应用模板变量替换生成最终 Dockerfile

#### 2. 端口检测机制

```java
private void writePort(Map<String, String> replaceMap) {
    // 读取 application.yml
    final BufferedReader reader = FileUtil.getReader(mainConfigFile, StandardCharsets.UTF_8);
    final Object config = YamlUtil.load(reader, Object.class);

    // 解析 JSON 路径
    final JsonNode path = jsonNode.findPath("zeka-stack")
                                 .findPath("docker")
                                 .findPath("export");

    // 生成端口配置
    if (path.isArray()) {
        path.elements().forEachRemaining(node -> {
            ports.add(StrUtil.trim(node.toString()));
        });
    }
}
```

#### 3. Assembly 配置生成

```java
@Mojo(name = "generate-docker-assembly-config", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true)
public class GenerateDockerAssemblyConfigFileMojo extends ZekaMavenPluginAbstractMojo {
    // Assembly 配置生成逻辑
}
```

**生成策略**：

- **lib.xml**: 处理依赖排除，支持 Spring Boot Fat Jar 模式
- **app.xml**: 根据应用类型（Spring Boot/Cloud）选择不同的配置包含策略

#### 4. 模板变量替换

插件使用模板引擎进行变量替换：

- `${PACKAGE.NAME}`: 项目构建名称
- `${EXPORT.PORT}`: 端口暴露指令
- `${PORTS}`: Docker Compose 端口映射
- `${HEALTHCHECK}`: 健康检查配置

#### 5. 分层构建优化

**多层构建策略**：

```dockerfile
# 缓存层：第三方依赖 jar（变化最少）
COPY docker/${PACKAGE.NAME}/lib/ /app/lib/
# 应用层：脚本、配置、主 jar（变化频繁）
COPY arco-maven-plugin/bin/ /app/bin/
COPY docker/${PACKAGE.NAME}/config/ /app/config/
COPY docker/${PACKAGE.NAME}/patch/ /app/patch/
COPY docker/${PACKAGE.NAME}/plugin/ /app/plugin/
COPY docker/${PACKAGE.NAME}/*.jar /app/
```

> `arco-maven-plugin/bin/launcher` 启动脚本是由 `arco-script-maven-plugin` 插件生成的
