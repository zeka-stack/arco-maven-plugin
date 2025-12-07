---
published: 2022.04.05
---

# 一键部署插件

## 📖 简介

`arco-publish-maven-plugin` 是一个专为简化前后端部署流程而设计的 Maven 插件，提供了一键部署功能，大幅提升部署效率。该插件支持多种部署模式，包括单模块部署、批量部署、前端项目部署以及文档部署。

### 现代自动化部署对比

如今，主流的自动化部署方案已经非常成熟和多样化：

- **CI/CD 平台**: Jenkins、GitLab CI、GitHub Actions、Azure DevOps 等
- **容器化部署**: Docker + Kubernetes、Docker Compose 等
- **配置管理工具**: Ansible、Chef、Puppet、SaltStack 等
- **云原生方案**: Helm Charts、ArgoCD、Flux 等
- **Serverless 部署**: AWS Lambda、Azure Functions、Vercel 等

这些现代方案具有更好的可扩展性、可维护性和生态支持。相比之下，本插件更像是一个轻量级的、针对特定场景的解决方案。

> **⚠️ 注意**: 该插件主要为特定环境设计，具有一定的历史背景和环境依赖性。虽然可能已经过时，但其设计思想和实现方式仍具有一定的参考价值。

## 🏢 背景

公司的基础设施建设比较落后，部署还采用非常原始的人肉部署方式：

1. **本地打包**: 开发人员需要在本地进行 Maven 打包，生成部署包
2. **手动上传**: 通过 FTP 或 SCP 工具手动上传部署包到目标服务器
3. **手动部署**: 登录服务器，手动解压、配置、启动服务
4. **环境配置**: 手动修改配置文件，设置环境变量
5. **服务启动**: 手动执行启动脚本，检查服务状态
6. **日志检查**: 手动查看日志，确认部署是否成功

这种效率是非常低下的，一个服务部署至少需要 10 分钟，而且容易出错。作为效率工作者和自动化爱好者，我是忍受不了这种低效的部署方式，所以也就有了这个插件。

**现在只需要执行 `mvn clean deploy`，就能根据 pom.xml 中的配置自动部署到远程服务器**，整个过程完全自动化，大大提升了开发效率和部署的可靠性。

## ✨ 主要特性

- 🚀 **一键部署**: 支持单个模块和多模块批量部署
- 🌐 **前端支持**: 集成前端构建工具（Node.js、Yarn、NPM、Webpack 等）
- 📚 **文档部署**: 支持项目文档的自动部署和版本管理
- 🔧 **灵活配置**: 支持多环境配置（dev、test、prev、prod）
- 🔒 **安全部署**: 基于 SSH 的安全文件传输和远程执行
- 🎯 **并行部署**: 支持多线程并行部署以提升效率
- 🔄 **自动化**: 集成 Maven 生命周期，无缝融入构建流程

## 🎯 插件目标 (Goals)

### 后端服务部署

| Goal             | 描述      | 默认阶段    |
|------------------|---------|---------|
| `publish-single` | 单个模块部署  | deploy  |
| `publish-batch`  | 多模块批量部署 | package |

### 前端项目部署

| Goal                    | 描述                | 默认阶段               |
|-------------------------|-------------------|--------------------|
| `publish-frontend`      | 前端项目部署            | deploy             |
| `install-node-and-npm`  | 安装 Node.js 和 NPM  | generate-resources |
| `install-node-and-yarn` | 安装 Node.js 和 Yarn | generate-resources |
| `npm`                   | 执行 NPM 命令         | generate-resources |
| `yarn`                  | 执行 Yarn 命令        | generate-resources |
| `webpack`               | 执行 Webpack 构建     | generate-resources |
| `gulp`                  | 执行 Gulp 任务        | generate-resources |
| `grunt`                 | 执行 Grunt 任务       | generate-resources |

### 文档部署

| Goal          | 描述     | 默认阶段   |
|---------------|--------|--------|
| `publish-doc` | 项目文档部署 | deploy |

## 🚀 快速开始

### 前提条件

1. **环境要求**:
    - Java 8+
    - Maven 3.6+
    - 目标服务器支持 SSH 连接

2. **服务器环境配置**:
    - 配置 sudo 免密码执行
    - 解决 sudo 环境变量丢失问题
    - 确保目标服务器可通过 SSH 访问

### 服务器用户配置

#### 创建部署用户 (zekastack)

在目标服务器上需要创建专门的部署用户 `zekastack`，该用户将用于：

- 接收上传的部署包
- 执行部署脚本
- 启动和管理服务进程

##### 1. 创建用户和用户组

```bash
# 创建用户组
sudo groupadd zekastack

# 创建用户并指定主目录
sudo useradd -m -g zekastack -s /bin/bash zekastack

# 设置密码（可选，建议使用密钥认证）
sudo passwd zekastack
```

##### 2. 配置 sudo 权限

创建 sudo 配置文件：

```bash
# 创建 sudo 配置文件
sudo visudo -f /etc/sudoers.d/zekastack
```

添加以下内容：

```bash
# zekastack 用户可以免密码执行特定命令
zekastack ALL=(ALL) NOPASSWD: /bin/mkdir, /bin/rm, /bin/mv, /bin/cp, /bin/tar, /bin/unzip, /bin/chown, /bin/chmod, /bin/kill, /bin/ps, /usr/bin/jps, /bin/nohup
zekastack ALL=(ALL) NOPASSWD: /opt/jdk*/bin/*
```

##### 3. 创建必要的目录结构

```bash
# 创建部署目录
sudo mkdir -p /opt/apps
sudo chown -R zekastack:zekastack /opt/apps

# 创建日志目录
sudo mkdir -p /mnt/syslogs/zeka.stack
sudo chown -R zekastack:zekastack /mnt/syslogs/zeka.stack

# 创建前端部署目录
sudo mkdir -p /opt/frontend
sudo chown -R zekastack:zekastack /opt/frontend

# 创建文档部署目录
sudo mkdir -p /mnt/zeka-stack/wiki
sudo chown -R zekastack:zekastack /mnt/zeka-stack/wiki
```

##### 4. 配置 SSH 密钥认证（推荐）

```bash
# 在本地生成密钥对
ssh-keygen -t rsa -b 4096 -C "deploy@yourcompany.com"

# 将公钥复制到服务器
ssh-copy-id zekastack@your-server-ip

# 或者手动添加公钥
mkdir -p /home/zekastack/.ssh
echo "your-public-key" >> /home/zekastack/.ssh/authorized_keys
chmod 700 /home/zekastack/.ssh
chmod 600 /home/zekastack/.ssh/authorized_keys
chown -R zekastack:zekastack /home/zekastack/.ssh
```

##### 5. 安装 Java 环境

```bash
# 安装 OpenJDK 8
sudo yum install -y java-1.8.0-openjdk java-1.8.0-openjdk-devel

# 或者安装 Oracle JDK
sudo tar -xzf jdk-8uXXX-linux-x64.tar.gz -C /opt/
sudo ln -s /opt/jdk1.8.0_XXX /opt/jdk1.8.0_211

# 设置环境变量
echo 'export JAVA_HOME=/opt/jdk1.8.0_211' >> /home/zekastack/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> /home/zekastack/.bashrc
```

##### 6. 验证配置

```bash
# 切换到 zekastack 用户
su - zekastack

# 测试 sudo 权限
sudo -n mkdir /tmp/test-sudo
sudo -n rm -rf /tmp/test-sudo

# 测试 Java 环境
java -version
jps -l

# 测试目录权限
ls -la /opt/apps
ls -la /mnt/syslogs/zeka.stack
```

#### 权限说明

`zekastack` 用户需要以下权限：

- **文件操作**: 创建、删除、移动、复制文件和目录
- **权限管理**: 修改文件所有者和权限
- **进程管理**: 启动、停止、查看 Java 进程
- **目录访问**: 访问部署目录、日志目录等
- **sudo 执行**: 免密码执行特定的系统命令

#### 安全建议

1. **使用密钥认证**: 避免使用密码，使用 SSH 密钥对
2. **限制 sudo 权限**: 只授予必要的命令执行权限
3. **定期轮换密钥**: 定期更新 SSH 密钥
4. **监控日志**: 定期检查部署和系统日志
5. **网络隔离**: 限制部署服务器的网络访问范围

### 基本使用

#### 1. 添加插件依赖

在项目的 `pom.xml` 中添加插件配置：

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-publish-maven-plugin</artifactId>
    <version>${arco.version}</version>
</plugin>
```

#### 2. 单模块部署配置

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-publish-maven-plugin</artifactId>
    <configuration>
        <groups>
            <group>
                <!-- 是否启用部署 -->
                <enable>true</enable>
                <!-- 部署环境 -->
                <env>test</env>
                <servers>
                    <server>
                        <!-- 目标服务器 IP -->
                        <host>192.168.1.100</host>
                    </server>
                </servers>
            </group>
        </groups>
    </configuration>
</plugin>
```

#### 3. 执行部署

```bash
# 单模块部署
mvn clean deploy -Dpublish.switch=true

# 指定环境部署
mvn clean deploy -Dpublish.switch=true -Dpublish.env=test

# 启用 APM 监控
mvn clean deploy -Dpublish.switch=true -Dapm.enable=true
```

## 📋 详细配置

插件参数分为两种配置方式：

1. **POM Properties 参数**: 直接配置在 `pom.xml` 的 `<properties>` 节点中
2. **JVM 参数**: 通过 Maven 命令行参数 `-D` 传递

### POM Properties 参数

#### 基础配置

| 参数                 | 描述              | 默认值  | 示例                                             |
|--------------------|-----------------|------|------------------------------------------------|
| `publish.enable`   | 是否启用部署          | true | `<publish.enable>true</publish.enable>`        |
| `publish.group.id` | 服务分组ID，用于区分不同服务 | 无    | `<publish.group.id>payment</publish.group.id>` |

#### 服务器配置

| 参数                   | 描述        | 默认值 | 说明                 |
|----------------------|-----------|-----|--------------------|
| `publish.hosts.dev`  | 开发环境服务器列表 | 无   | 多个IP用逗号分隔，未配置将忽略部署 |
| `publish.hosts.test` | 测试环境服务器列表 | 无   | 多个IP用逗号分隔，未配置将忽略部署 |
| `publish.hosts.prev` | 预演环境服务器列表 | 无   | 多个IP用逗号分隔，未配置将忽略部署 |

#### 路径配置

| 参数                    | 描述       | 默认值                | 说明                |
|-----------------------|----------|--------------------|-------------------|
| `publish.upload.path` | 部署包上传路径  | `/home/{username}` | 由于权限限制，必须先上传到用户目录 |
| `publish.target.path` | 服务部署目标路径 | `/opt/apps`        | 最终服务部署的根目录        |

#### 运行配置

| 参数                     | 描述            | 默认值                     | 可选值                        |
|------------------------|---------------|-------------------------|----------------------------|
| `publish.running.type` | 部署执行方式        | single                  | single(单线程), parallel(多线程) |
| `publish.java.home`    | 服务器 JAVA_HOME | `/opt/jdk1.8.0_211/bin` | Java 安装路径                  |

#### JVM 配置

| 参数                 | 描述           | 默认值                                                                | 说明        |
|--------------------|--------------|--------------------------------------------------------------------|-----------|
| `jvm.options`      | 非生产环境 JVM 配置 | `-Xms128M -Xmx256M -XX:MetaspaceSize=64m -XX:MaxMetaspaceSize=64m` | 开发/测试环境使用 |
| `prod.jvm.options` | 生产环境 JVM 配置  | 无                                                                  | 生产环境专用配置  |

**注意**: 通用启动脚本已包含以下 JVM 参数，自定义配置中不要重复添加：

```shell
-Xloggc
-XX:ErrorFile
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath
-XX:OnOutOfMemoryError
```

#### 命名空间配置

| 参数                  | 描述       | 默认值        | 说明          |
|---------------------|----------|------------|-------------|
| `publish.namespace` | 应用命名空间前缀 | zeka-stack | 用于区分不同系统的配置 |

#### 完整配置示例

```xml
<properties>
    <!-- 基础配置 -->
    <publish.enable>true</publish.enable>
    <publish.group.id>payment</publish.group.id>

    <!-- 服务器配置 -->
    <publish.hosts.dev>192.168.1.100</publish.hosts.dev>
    <publish.hosts.test>192.168.1.101</publish.hosts.test>
    <publish.hosts.prev>192.168.1.102,192.168.1.103</publish.hosts.prev>

    <!-- 路径配置 -->
    <publish.upload.path>/home/zekastack</publish.upload.path>
    <publish.target.path>/opt/apps</publish.target.path>

    <!-- 运行配置 -->
    <publish.running.type>single</publish.running.type>
    <publish.java.home>/opt/jdk1.8.0_211/bin</publish.java.home>

    <!-- JVM 配置 -->
    <jvm.options>-Xms256M -Xmx512M</jvm.options>
    <prod.jvm.options>-Xms512M -Xmx1024M -XX:MetaspaceSize=128m -XX:MaxMetaspaceSize=128m</prod.jvm.options>

    <!-- 命名空间配置 -->
    <publish.namespace>payment-system</publish.namespace>
</properties>
```

### JVM 参数

运行时可通过 JVM 参数覆盖 POM 配置，用于直接控制部署行为：

#### 核心控制参数

| 参数               | 描述         | 默认值   | 说明                        |
|------------------|------------|-------|---------------------------|
| `publish.switch` | 是否启用一键部署插件 | false | 安全开关，防止意外触发部署             |
| `publish.env`    | 指定部署环境     | 无     | 单模块部署时指定环境，未指定将部署到所有配置的环境 |

#### 认证参数

| 参数                 | 描述     | 默认值       | 说明              |
|--------------------|--------|-----------|-----------------|
| `publish.username` | 服务器用户名 | zekastack | 部署服务器的用户名       |
| `publish.password` | 服务器密码  | 无         | 建议使用密钥认证，避免明文密码 |

#### 功能参数

| 参数                  | 描述        | 默认值                     | 说明                      |
|---------------------|-----------|-------------------------|-------------------------|
| `apm.enable`        | 启用 APM 监控 | false                   | 启用后会注册到 SkyWalking      |
| `package.env.prod`  | 生产环境打包    | false                   | 使用 prod.jvm.options 配置  |
| `PUBLISH.JAVA_HOME` | Java 安装路径 | `/opt/jdk1.8.0_211/bin` | 覆盖 publish.java.home 配置 |

#### 使用示例

```bash
# 基础部署
mvn clean deploy -Dpublish.switch=true

# 指定环境部署
mvn clean deploy -Dpublish.switch=true -Dpublish.env=test

# 启用 APM 监控
mvn clean deploy -Dpublish.switch=true -Dapm.enable=true

# 生产环境打包
mvn clean package -Dpackage.env.prod=true

# 指定用户名和密码
mvn clean deploy -Dpublish.switch=true -Dpublish.username=deployer -Dpublish.password=secret
```

### 多模块批量部署

对于多模块项目，需要创建一个 distribution 模块：

```xml
<!-- distribution/pom.xml -->
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-publish-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>publish-batch</id>
            <phase>package</phase>
            <goals>
                <goal>publish-batch</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <groups>
            <group>
                <enable>true</enable>
                <env>test</env>
                <servers>
                    <server>
                        <host>192.168.1.100</host>
                        <names>
                            <name>service-a</name>
                            <name>service-b</name>
                        </names>
                    </server>
                    <server>
                        <host>192.168.1.101</host>
                        <names>
                            <name>service-c</name>
                            <name>service-d</name>
                        </names>
                    </server>
                </servers>
            </group>
        </groups>
    </configuration>
</plugin>
```

执行批量部署：

```bash
mvn clean package -Dpublish.switch=true
```

## 🌐 前端项目部署

### 基本配置

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-publish-maven-plugin</artifactId>
    <configuration>
        <groups>
            <group>
                <enable>true</enable>
                <env>test</env>
                <servers>
                    <server>
                        <host>192.168.1.100</host>
                    </server>
                </servers>
            </group>
        </groups>
    </configuration>
    <executions>
        <!-- 安装 Node.js 和 Yarn -->
        <execution>
            <id>install node and yarn</id>
            <goals>
                <goal>install-node-and-yarn</goal>
            </goals>
        </execution>

        <!-- 安装依赖 -->
        <execution>
            <id>yarn install</id>
            <goals>
                <goal>yarn</goal>
            </goals>
            <configuration>
                <arguments>install --registry=https://registry.npmmirror.com</arguments>
            </configuration>
        </execution>

        <!-- 构建项目 -->
        <execution>
            <id>yarn build</id>
            <goals>
                <goal>yarn</goal>
            </goals>
            <configuration>
                <arguments>run build</arguments>
            </configuration>
        </execution>

        <!-- 部署前端项目 -->
        <execution>
            <id>publish frontend</id>
            <goals>
                <goal>publish-frontend</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 前端项目属性配置

```xml
<properties>
    <package.name>your-frontend-app</package.name>
    <publish.group.id>${package.name}</publish.group.id>

    <!-- Node.js 版本配置 -->
    <nodeVersion>v16.20.0</nodeVersion>
    <yarnVersion>v1.22.19</yarnVersion>
</properties>
```

## 📚 文档部署

支持项目文档的版本化部署：

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-publish-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>publish-doc</id>
            <goals>
                <goal>publish-doc</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

文档项目配置：

```xml
<properties>
    <maven.install.skip>true</maven.install.skip>
    <maven.deploy.skip>true</maven.deploy.skip>

    <!-- 必填：部门名称 -->
    <publish.department.name>技术部</publish.department.name>
    <!-- 必填：项目名称 -->
    <publish.project.name>Arco Stack</publish.project.name>
</properties>
```

## 🔧 实现细节

### 核心组件

1. **ServicePublishMojo**: 抽象基类，提供通用的部署逻辑
2. **SSHAgent**: SSH 连接管理和远程命令执行
3. **PublishConfigAnalyse**: 部署配置解析接口
4. **前端构建支持**: 集成多种前端构建工具

### 部署流程详解

#### 1. 初始化阶段

- 解析 Maven 配置参数
- 验证必要的配置项
- 建立与目标服务器的 SSH 连接
- 检查目标服务器环境

#### 2. 文件准备阶段

- 扫描 `target` 目录中的部署包（.tar.gz 文件）
- 根据配置确定要部署的服务列表
- 准备上传路径和部署路径

#### 3. 文件传输阶段

- 将部署包上传到服务器的临时目录（`/home/zekastack/{env}/{group}`）
- 验证文件传输完整性
- 创建必要的目录结构

#### 4. 远程部署阶段

- 停止旧版本服务（如果存在）
- 备份当前部署（带时间戳）
- 解压新的部署包到目标目录
- 修改文件权限和所有者
- 启动新版本服务

#### 5. 验证阶段

- 检查服务启动状态
- 验证进程是否正常运行
- 输出部署结果和日志信息

### 安全特性

- **SSH 协议**: 基于 SSH 的安全连接，支持密码和密钥认证
- **权限控制**: 通过 sudo 执行系统命令，最小权限原则
- **用户隔离**: 使用专门的部署用户，避免使用 root 权限
- **日志记录**: 完整的部署过程日志，便于问题排查
- **超时控制**: 防止部署过程无限等待

### 错误处理

- **连接失败**: 自动重试机制，详细的错误信息
- **权限不足**: 明确的权限检查提示
- **服务启动失败**: 自动回滚到备份版本
- **超时处理**: 强制终止并清理资源

## 📝 使用示例

### 示例 1: 简单的 Spring Boot 应用部署

```xml
<properties>
    <publish.enable>true</publish.enable>
    <publish.group.id>demo-service</publish.group.id>
    <publish.hosts.test>192.168.1.100</publish.hosts.test>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-publish-maven-plugin</artifactId>
        </plugin>
    </plugins>
</build>
```

部署命令：

```bash
mvn clean deploy -Dpublish.switch=true -Dpublish.env=test
```

### 示例 2: Vue.js 前端项目部署

```xml
<properties>
    <package.name>admin-frontend</package.name>
    <publish.group.id>${package.name}</publish.group.id>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-publish-maven-plugin</artifactId>
            <executions>
                <execution>
                    <id>install-node-and-yarn</id>
                    <goals>
                        <goal>install-node-and-yarn</goal>
                    </goals>
                </execution>
                <execution>
                    <id>yarn-install</id>
                    <goals>
                        <goal>yarn</goal>
                    </goals>
                    <configuration>
                        <arguments>install</arguments>
                    </configuration>
                </execution>
                <execution>
                    <id>yarn-build</id>
                    <goals>
                        <goal>yarn</goal>
                    </goals>
                    <configuration>
                        <arguments>run build</arguments>
                    </configuration>
                </execution>
                <execution>
                    <id>publish-frontend</id>
                    <goals>
                        <goal>publish-frontend</goal>
                    </goals>
                </execution>
            </executions>
            <configuration>
                <groups>
                    <group>
                        <enable>true</enable>
                        <env>test</env>
                        <servers>
                            <server>
                                <host>192.168.1.100</host>
                            </server>
                        </servers>
                    </group>
                </groups>
            </configuration>
        </plugin>
    </plugins>
</build>
```

## ⚠️ 注意事项

1. **环境依赖**: 该插件为特定环境设计，可能需要根据实际情况调整配置
2. **安全考虑**: 请妥善保管服务器凭证，建议使用密钥认证
3. **网络要求**: 确保构建环境能够访问目标部署服务器
4. **权限配置**: 部署用户需要有足够的权限执行部署操作
5. **备份策略**: 建议在生产环境部署前进行备份

## 🔍 故障排除

### 常见问题

1. **SSH 连接失败**
    - 检查网络连通性
    - 验证用户名和密码
    - 确认 SSH 服务状态

2. **权限不足**
    - 配置 sudo 免密码执行
    - 检查目标目录权限
    - 验证用户组配置

3. **部署超时**
    - 检查网络稳定性
    - 调整超时配置
    - 查看服务器资源使用情况
