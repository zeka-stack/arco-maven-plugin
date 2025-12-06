# Arco Maven Plugin

## 📖 项目简介

`arco-maven-plugin` 是 Zeka Stack 框架的核心支撑项目，提供了一套完整的 Maven 插件体系，旨在通过自动化简化复杂的重复性操作，让开发者能够将更多精力投入到业务开发中。

> **设计理念**: 践行 "If something – anything – requires more than 90 seconds of his time, he writes a script to automate that" 的自动化思想，通过 Maven 插件的方式将任何超过 90 秒的重复性任务自动化。

> **经典案例**: 这个理念的完美体现可以参考 [hacker-scripts](https://github.com/NARKOZ/hacker-scripts) 项目，其中一位构建工程师将日常生活中的各种重复性任务都自动化了，包括自动发短信给妻子、自动处理邮件、甚至自动煮咖啡等。正如项目描述所说："If something – anything – requires more than 90 seconds of his time, he writes a script to automate that."

## 🎯 核心价值

- **🚀 开箱即用**: 框架内置完整配置，无需额外配置即可使用
- **⚡ 自动化优先**: 将复杂的手动操作自动化，提升开发效率
- **🔧 约定大于配置**: 提供合理的默认配置，减少配置负担
- **🛡️ 质量保障**: 内置代码质量检查，确保代码规范
- **📦 一键部署**: 支持多种部署方式，简化部署流程
- **🐳 容器化支持**: 提供完整的容器化解决方案

## 🏗️ 项目架构

```
arco-maven-plugin/
├── arco-assist-maven-plugin/          # 核心辅助插件 - 构建自动化
├── arco-boot-maven-plugin/            # 启动优化插件 - 动态类加载
├── arco-checkstyle-plugin-rule/       # 代码风格检查 - 静态分析
├── arco-pmd-plugin-rule/              # 代码质量检查 - 逻辑分析
├── arco-enforcer-plugin-rule/         # 依赖管理检查 - 冲突检测
├── arco-publish-maven-plugin/         # 一键部署插件 - 远程部署
├── arco-script-maven-plugin/          # 启动脚本生成 - 生命周期管理
├── arco-container-maven-plugin/       # 容器化插件 - Docker 支持
├── arco-makeself-maven-plugin/        # 自解压部署 - 一键安装
├── arco-maven-plugin-common/          # 公共模块 - 共享组件
└── pom.xml                            # 父 POM - 统一管理
```

## 🔧 插件详解

### 1. arco-assist-maven-plugin - 核心辅助插件

**功能**: 提供构建自动化的核心功能，简化 Maven 构建流程

**核心特性**:
- 🎯 **动态 assembly.xml 生成**: 自动生成 Maven Assembly 配置，支持可重现构建
- 📊 **构建信息生成**: 自动生成 `build-info.properties`，包含版本、时间戳等信息
- 🔍 **主类自动检测**: 智能识别 Spring Boot 应用的主类
- ⚙️ **智能插件控制**: 提供 `SkipPluginMojo` 统一管理插件启用/禁用
- 🧹 **辅助功能**: Spring 环境管理、依赖清理、临时文件清理等

**使用场景**: 所有使用 Zeka Stack 框架的项目都会自动集成此插件

### 2. arco-boot-maven-plugin - 启动优化插件

**功能**: 解决传统服务器环境中的存储浪费、部署时间长、内存占用高等问题

**核心特性**:
- 📦 **JAR 重打包**: 将应用 JAR 分离为业务代码和框架代码
- 🔄 **动态类加载**: 支持 `patch/` 和 `plugin/` 目录的动态类加载
- 🔥 **热补丁支持**: 支持运行时热更新，无需重启应用
- 🏗️ **插件架构**: 支持插件式扩展，灵活配置类加载路径
- 📋 **类加载优先级**: `patch/` > `plugin/` > `BOOT-INF/lib/` > `BOOT-INF/classes/`

**使用场景**: 需要动态更新、插件扩展或优化部署效率的场景

### 3. arco-checkstyle-plugin-rule - 代码风格检查

**功能**: 提供基于 Checkstyle 的代码风格检查规则，确保代码规范统一

**核心特性**:
- 📏 **内置规则集**: Zeka Stack、Google、Alibaba、自定义等多种规则集
- 🎯 **自定义检查**: `MethodLimitCheck`、`IllegalNewCheck`、`JavadocCheck` 等
- ⚡ **编译时检查**: 默认启用，违反规则时停止编译
- 🔧 **灵活配置**: 支持规则自定义和排除配置

**规则示例**:
- 文件长度限制: 最大 1500 行
- 行长度限制: 最大 140 字符
- 方法长度限制: 最大 80 行
- 参数数量限制: 最多 5 个参数
- 强制 Javadoc: 所有公共方法必须有文档

### 4. arco-pmd-plugin-rule - 代码质量检查

**功能**: 基于 Alibaba P3C 规范的 PMD 规则，专注于代码质量和逻辑检查

**核心特性**:
- 🏢 **P3C 规范**: 基于阿里巴巴 Java 开发手册的规则集
- 🔍 **多维度检查**: 注释、并发、常量、异常、流程控制、命名、OOP、ORM 等
- ⚡ **编译时检查**: 与 Checkstyle 互补，专注于代码逻辑质量
- 📊 **详细报告**: 提供详细的违规信息和修复建议

**规则示例**:
- 线程池创建规范
- 避免使用 `Timer` 类
- `ThreadLocal` 正确清理
- `finally` 块中的锁释放
- 魔数检查
- `switch` 语句必须有 `default`
- `equals` 和 `hashCode` 最佳实践

### 5. arco-enforcer-plugin-rule - 依赖管理检查

**功能**: 提供 Maven Enforcer 规则，早期检测和处理依赖冲突

**核心特性**:
- 🔍 **依赖收敛检查**: 检测同一依赖的不同版本冲突
- ⚡ **早期发现问题**: 在构建阶段就发现潜在的运行时问题
- 🛠️ **自定义规则**: `ZekaEcforcerRule` 提供项目、会话、运行时信息验证
- 🔧 **冲突解决**: 提供 `dependencyManagement` 和 `exclusions` 解决建议

**使用场景**: 多模块项目、复杂依赖关系的项目

### 6. arco-publish-maven-plugin - 一键部署插件

**功能**: 提供一键部署功能，支持后端、前端、文档项目的自动化部署

**核心特性**:
- 🚀 **一键部署**: 单个模块和多模块批量部署
- 🌐 **前端支持**: 集成 Node.js、NPM、Yarn、Webpack、Gulp、Grunt 等
- 📚 **文档部署**: 支持项目文档的版本化部署
- 🔧 **多环境支持**: dev、test、prev、prod 环境配置
- 🔒 **安全部署**: 基于 SSH 的安全文件传输和远程执行
- ⚡ **并行部署**: 支持多线程并行部署提升效率

**部署流程**:
1. 本地打包 → 2. 安全上传 → 3. 远程解压 → 4. 服务重启 → 5. 状态验证

### 7. arco-script-maven-plugin - 启动脚本生成

**功能**: 自动生成通用的 Java 应用启动脚本，提供完整的生命周期管理

**核心特性**:
- 🎯 **生命周期管理**: 启动、停止、重启、状态查看等核心操作
- 🔧 **多环境支持**: dev、test、prod 环境配置
- 📊 **监控集成**: 支持 JMX、APM（SkyWalking）、Debug 模式
- 📝 **日志管理**: 自动日志目录创建、日志查看、日志轮转
- ⚙️ **JVM 优化**: 内置生产级 JVM 参数，支持自定义配置
- 🛡️ **错误处理**: OOM 处理、进程管理、优雅停止

**脚本参数**:
- `-s env`: 启动应用（指定环境）
- `-r env`: 重启应用
- `-S env`: 停止应用
- `-c env`: 查看状态
- `-l`: 查看日志
- `-d port`: Debug 模式
- `-m port`: JMX 监控
- `-w`: APM 监控

### 8. arco-container-maven-plugin - 容器化插件

**功能**: 自动化 Spring Boot 应用的 Docker 容器化，提供优化的镜像构建策略

**核心特性**:
- 🐳 **智能 Dockerfile 生成**: 基础镜像、单层构建、多层构建三种模板
- 📦 **分层构建优化**: 最大化 Docker 层缓存效率
- 🔍 **智能端口检测**: 自动读取 `application.yml` 配置
- 💊 **健康检查集成**: 自动配置容器健康检查
- 🚀 **Docker Compose 支持**: 自动生成编排配置
- ⚡ **一键构建脚本**: 交互式构建和部署工具

**构建模式**:
- **单层构建**: `mvn package -Ddockerfile.skip=false`（简单快速）
- **多层构建**: `mvn package -Pdocker`（推荐，充分利用缓存）

### 9. arco-makeself-maven-plugin - 自解压部署

**功能**: 创建自解压的部署包（.run 文件），实现真正的一键部署

**核心特性**:
- 📦 **自解压部署包**: 将 tar.gz 转换为可执行的 .run 文件
- 🚀 **一键部署**: 上传后直接执行 `./app.run` 即可完成部署
- 🔧 **环境配置**: 支持不同环境的启动脚本
- 🛡️ **权限管理**: 自动处理文件权限和用户切换
- 🔍 **完整性校验**: 支持 MD5、CRC、SHA256 校验
- 📊 **多压缩格式**: 支持 gzip、bzip2、xz、lzo、lz4 等

**使用方式**:
```bash
# 构建自解压包
mvn clean package -Dmakeself.skip=false

# 部署使用
./your-app.run          # 生产环境
./your-app.run test     # 测试环境
./your-app.run local    # 本地环境
```

## 🚀 快速开始

### 1. 框架集成

使用 Zeka Stack 框架的项目会自动集成所有插件，无需额外配置：

```xml
<parent>
    <groupId>dev.dong4j</groupId>
    <artifactId>zeka-stack-parent</artifactId>
    <version>2.0.0-SNAPSHOT</version>
</parent>
```

### 2. 基础使用

```bash
# 代码质量检查（自动执行）
mvn clean compile

# 构建部署包
mvn clean package

# 生成启动脚本
mvn clean package

# 容器化构建
mvn clean package -Pdocker

# 自解压部署包
mvn clean package -Dmakeself.skip=false

# 一键部署
mvn clean deploy -Dpublish.switch=true
```

### 3. 高级配置

```xml
<properties>
    <!-- JVM 参数配置 -->
    <jvm.options>-Xms256M -Xmx512M</jvm.options>
    <prod.jvm.options>-Xms1G -Xmx2G -XX:+UseG1GC</prod.jvm.options>
    
    <!-- 部署配置 -->
    <publish.enable>true</publish.enable>
    <publish.hosts.test>192.168.1.100</publish.hosts.test>
    
    <!-- 容器化配置 -->
    <dockerfile.skip>false</dockerfile.skip>
</properties>
```

## 🔧 配置指南

### 代码质量配置

```xml
<!-- 自定义 Checkstyle 规则 -->
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-checkstyle-plugin-rule</artifactId>
    <configuration>
        <configLocation>custom-checkstyle.xml</configLocation>
    </configuration>
</plugin>

<!-- 自定义 PMD 规则 -->
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-pmd-plugin-rule</artifactId>
    <configuration>
        <rulesets>
            <ruleset>custom-pmd-rules.xml</ruleset>
        </rulesets>
    </configuration>
</plugin>
```

### 部署配置

```xml
<!-- 单模块部署 -->
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
</plugin>
```

### 容器化配置

```yaml
# application.yml
zeka-stack:
  docker:
    export:
      - "8080"    # 主服务端口
      - "8443"    # HTTPS 端口
      - "9090"    # 管理端口
```

## 📊 最佳实践

### 1. 开发环境

```bash
# 开发时使用较小的 JVM 参数
bin/launcher -s dev -o '-Xms128M -Xmx256M' -t
```

### 2. 测试环境

```bash
# 测试环境启用调试信息
bin/launcher -s test -o '-Xms256M -Xmx512M' -i
```

### 3. 生产环境

```bash
# 生产环境使用优化的 JVM 参数
bin/launcher -s prod -o '-Xms2G -Xmx4G -XX:MaxGCPauseMillis=200'
```

### 4. 容器化部署

```bash
# 使用多层构建优化缓存
mvn clean package -Pdocker

# 构建和运行
docker build -f target/docker/Dockerfile-M -t your-app:latest target
docker run -p 8080:8080 your-app:latest
```

## 🛠️ 故障排除

### 常见问题

1. **代码质量检查失败**
   - 检查 Checkstyle 和 PMD 规则配置
   - 使用 `-Dcheckstyle.skip=true` 临时跳过

2. **启动脚本权限问题**
   - 确保脚本有执行权限：`chmod +x bin/launcher`
   - 检查文件系统权限

3. **容器构建失败**
   - 检查 Dockerfile 路径和构建上下文
   - 确保基础镜像存在

4. **部署连接失败**
   - 检查 SSH 连接和服务器配置
   - 验证用户权限和目录权限

### 调试技巧

```bash
# 查看详细构建信息
mvn clean package -X

# 查看启动参数
bin/launcher -s dev -i

# 查看容器构建日志
docker build -f target/docker/Dockerfile-M -t your-app:latest target --no-cache
```

## 📈 性能优化

### 构建优化

- 使用 `-T` 参数进行并行构建
- 合理配置 Maven 内存参数
- 使用本地 Maven 仓库镜像

### 运行时优化

- 根据应用特点调整 JVM 参数
- 使用 G1GC 进行垃圾回收优化
- 启用 JVM 性能监控

### 部署优化

- 使用多层 Docker 构建充分利用缓存
- 并行部署多个服务
- 使用自解压包减少部署步骤

## 🤝 贡献指南

欢迎贡献代码、报告问题或提出改进建议！

1. Fork 项目
2. 创建特性分支
3. 提交更改
4. 推送到分支
5. 创建 Pull Request

## 📄 许可证

本项目采用 [MIT 许可证](https://github.com/zeka-stack/zeka-stack/blob/main/LICENSE)

## 🙏 致谢

感谢所有为这个项目做出贡献的开发者和开源社区！

特别感谢 [hacker-scripts](https://github.com/NARKOZ/hacker-scripts) 项目，它完美诠释了自动化理念，启发了我们对"90秒自动化"原则的坚持和实践。

---

> **记住**: 如果任何任务超过 90 秒，就写脚本自动化它！这就是 Zeka Stack 的自动化哲学。