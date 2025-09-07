# Arco Enforcer Plugin Rule

## 概述

`arco-enforcer-plugin-rule` 是一个为 Maven Enforcer Plugin 提供自定义检查规则的插件。该插件专注于 Maven 依赖管理，旨在尽可能早地发现和处理依赖冲突，避免生产环境因依赖冲突导致的问题。

### 依赖冲突带来的问题

在复杂的 Maven 项目中，依赖冲突是一个常见且严重的问题，可能导致：

#### 1. 运行时异常
- **ClassNotFoundException**: 找不到预期的类，因为加载了错误版本的依赖
- **NoSuchMethodError**: 方法签名不匹配，不同版本的 API 差异导致
- **LinkageError**: 类加载器无法正确链接类文件
- **运行时崩溃**: 应用在生产环境突然崩溃

#### 2. 功能异常
- **行为不一致**: 不同版本的依赖库行为差异导致功能异常
- **性能问题**: 某些版本的依赖可能存在性能缺陷
- **安全漏洞**: 旧版本依赖可能包含已知的安全漏洞
- **兼容性问题**: 不同版本间的 API 不兼容

#### 3. 开发和维护问题
- **调试困难**: 问题难以复现，调试过程复杂
- **测试不充分**: 开发环境可能使用了不同的依赖版本
- **部署风险**: 生产环境与开发环境不一致
- **维护成本高**: 问题排查和修复成本高

### 典型依赖冲突场景

#### 场景1: Spring Framework 版本冲突
```xml
<!-- 项目A依赖Spring 5.3.x -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-core</artifactId>
    <version>5.3.21</version>
</dependency>

<!-- 项目B通过传递依赖引入Spring 5.2.x -->
<dependency>
    <groupId>com.example</groupId>
    <artifactId>some-library</artifactId>
    <version>1.0.0</version>
    <!-- 内部依赖Spring 5.2.20 -->
</dependency>
```
**问题**: 可能导致 `NoSuchMethodError`，因为不同版本的 Spring API 有差异。

#### 场景2: 日志框架冲突
```xml
<!-- 同时存在多个日志实现 -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-log4j12</artifactId>
    <version>1.7.30</version>
</dependency>
<dependency>
    <groupId>ch.qos.logback</groupId>
    <artifactId>logback-classic</artifactId>
    <version>1.2.11</version>
</dependency>
```
**问题**: 可能导致日志输出异常或重复日志。

#### 场景3: Jackson 版本冲突
```xml
<!-- 不同模块使用不同版本的Jackson -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-databind</artifactId>
    <version>2.13.3</version>
</dependency>
<!-- 传递依赖引入2.12.6版本 -->
```
**问题**: JSON 序列化/反序列化可能出现异常。

### 解决方案

为了解决这些问题，我们采用了以下策略：

1. **提前检查**: 在编译阶段就进行依赖冲突检查，而不是等到运行时才发现问题
2. **自动化验证**: 通过 Maven Enforcer Plugin 自动验证依赖的一致性
3. **强制检查**: 后续版本将强制进行依赖冲突检查，发现冲突时编译失败
4. **持续监控**: 在 CI/CD 流程中集成依赖检查，确保代码质量

### 强制检查策略

#### 当前状态
- 依赖检查已集成到构建流程中
- 检查失败时不会中断构建（`fail=false`）
- 主要用于发现和报告依赖冲突问题

#### 未来规划
- **强制检查模式**: 将 `fail` 设置为 `true`，发现依赖冲突时编译失败
- **零容忍政策**: 不允许任何依赖冲突通过构建
- **团队规范**: 所有团队成员必须解决依赖冲突后才能提交代码
- **CI/CD 集成**: 在持续集成中强制执行依赖检查

#### 实施时间表
- **Phase 1** (当前): 监控和报告依赖冲突
- **Phase 2** (下个版本): 警告模式，显示冲突但不中断构建
- **Phase 3** (后续版本): 强制模式，发现冲突立即失败

**设计理念**: 个人非常注重 Maven 的依赖管理，尽可能早地处理依赖冲突，所以会使用这个插件来检查依赖冲突。目前虽然还没有做过多的处理，但后续会强制检查依赖冲突，如果有的话就会在编译期报错，避免生产环境因为依赖冲突出现问题。

## 使用方式

### 1. 在 Zeka Stack 项目中使用

由于插件已内置到框架中，无需额外配置，直接运行 Maven 命令即可：

```bash
# 运行依赖检查
mvn enforcer:enforce

# 在验证阶段自动运行
mvn validate

# 临时跳过依赖检查（紧急修复时使用）
mvn clean compile -Denforcer.skip=true
```

### 2. 在独立项目中使用

如果需要在非 Zeka Stack 项目中使用，可以添加依赖：

```xml
<dependency>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-enforcer-plugin-rule</artifactId>
    <version>${arco-maven-plugin.version}</version>
</dependency>
```

## 功能特性

### 🎯 依赖冲突检查
- **依赖收敛检查**: 确保所有传递依赖的版本一致
- **版本冲突检测**: 发现不同模块使用不同版本的同一依赖
- **编译期检查**: 在编译阶段就发现依赖问题，避免运行时错误

### 🔧 自定义检查规则
插件提供了以下自定义检查器：

#### ZekaEcforcerRule - Zeka 自定义强制规则
- **功能**: 自定义的依赖和版本检查规则
- **特性**: 
  - 支持项目信息获取和验证
  - 支持 Maven 会话和运行时信息检查
  - 可配置是否在检查失败时中断构建
- **用途**: 为 Zeka Stack 框架提供特定的依赖管理策略

## 框架集成配置

### Zeka Stack 框架内置配置

为了简化使用者的配置工作，本插件已内置到 Zeka Stack 框架中，提供开箱即用的依赖检查功能。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>${maven-enforcer-plugin.version}</version>
    <dependencies>
        <dependency>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-enforcer-plugin-rule</artifactId>
            <version>${arco-maven-plugin.version}</version>
        </dependency>
    </dependencies>
    <executions>
        <execution>
            <configuration>
                <fail>false</fail>
                <rules>
                    <dependencyConvergence/>
                    <zekaEcforcerRule implementation="dev.dong4j.zeka.maven.plugin.enforcer.rule.ZekaEcforcerRule">
                        <shouldIfail>true</shouldIfail>
                    </zekaEcforcerRule>
                </rules>
            </configuration>
            <goals>
                <goal>enforce</goal>
            </goals>
            <id>enforce-versions</id>
        </execution>
    </executions>
</plugin>
```

### 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `fail` | 检查失败时是否中断构建 | `false` |
| `dependencyConvergence` | 依赖收敛检查规则 | 启用 |
| `zekaEcforcerRule` | 自定义 Zeka 规则 | 启用 |
| `shouldIfail` | 自定义规则失败时是否中断 | `true` |

### 标准 Maven Enforcer Plugin 配置对比

在介绍本插件的简化配置之前，先了解标准的 Maven Enforcer Plugin 配置方式：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-enforcer-plugin</artifactId>
    <version>3.0.0</version>
    <executions>
        <execution>
            <id>enforce-versions</id>
            <goals>
                <goal>enforce</goal>
            </goals>
            <configuration>
                <rules>
                    <dependencyConvergence/>
                    <requireMavenVersion>
                        <version>3.6.0</version>
                    </requireMavenVersion>
                    <requireJavaVersion>
                        <version>11</version>
                    </requireJavaVersion>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

## 检查规则详解

### 内置检查规则

#### 1. dependencyConvergence - 依赖收敛检查
- **功能**: 检查所有传递依赖的版本是否一致
- **检查内容**: 
  - 同一依赖的不同版本冲突
  - 传递依赖的版本收敛
  - 依赖树中的版本不一致
- **错误示例**: 
  ```
  Dependency convergence error for org.slf4j:slf4j-api:1.7.25 paths to dependency are:
  +-com.example:my-project:1.0.0
    +-org.slf4j:slf4j-api:1.7.25
  +-com.example:my-project:1.0.0
    +-com.another:library:2.0.0
      +-org.slf4j:slf4j-api:1.7.30
  ```

#### 2. ZekaEcforcerRule - 自定义规则
- **功能**: 提供 Zeka Stack 框架特定的依赖检查逻辑
- **检查内容**:
  - 项目信息验证
  - Maven 会话状态检查
  - 运行时信息验证
  - 构建目录和构件信息检查

### 自定义检查规则配置

#### ZekaEcforcerRule 配置示例

```xml
<zekaEcforcerRule implementation="dev.dong4j.zeka.maven.plugin.enforcer.rule.ZekaEcforcerRule">
    <shouldIfail>true</shouldIfail>
</zekaEcforcerRule>
```

#### 其他常用规则配置

```xml
<rules>
    <!-- 依赖收敛检查 -->
    <dependencyConvergence/>
    
    <!-- Maven 版本要求 -->
    <requireMavenVersion>
        <version>3.6.0</version>
    </requireMavenVersion>
    
    <!-- Java 版本要求 -->
    <requireJavaVersion>
        <version>11</version>
    </requireJavaVersion>
    
    <!-- 禁止快照版本 -->
    <bannedDependencies>
        <excludes>
            <exclude>*:*:*SNAPSHOT</exclude>
        </excludes>
    </bannedDependencies>
    
    <!-- 要求插件版本 -->
    <requirePluginVersions>
        <message>Best Practice is to always define plugin versions!</message>
    </requirePluginVersions>
</rules>
```

## 依赖冲突处理

### 常见依赖冲突类型

1. **直接依赖冲突**
   - 同一个依赖的不同版本被直接声明
   - 解决方案：统一版本号

2. **传递依赖冲突**
   - 不同依赖引入了同一库的不同版本
   - 解决方案：使用 `dependencyManagement` 统一版本

3. **范围冲突**
   - 同一依赖的不同作用域声明
   - 解决方案：明确依赖作用域

### 冲突解决策略

#### 1. 使用 dependencyManagement
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
            <version>1.7.30</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

#### 2. 使用 exclusions 排除冲突依赖
```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>library</artifactId>
    <version>1.0.0</version>
    <exclusions>
        <exclusion>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### 3. 使用 dependency:tree 分析依赖
```bash
# 查看依赖树
mvn dependency:tree

# 查看特定依赖的路径
mvn dependency:tree -Dincludes=org.slf4j:slf4j-api

# 分析依赖冲突
mvn dependency:analyze
```

## 检查报告

### 控制台输出

检查结果会在控制台显示，包括：
- 依赖冲突详情
- 版本不一致信息
- 建议的解决方案
- 构建失败原因

### 详细报告

可以通过以下命令生成详细的依赖分析报告：

```bash
# 生成依赖树报告
mvn dependency:tree -Doutput=dependency-tree.txt

# 生成依赖分析报告
mvn dependency:analyze -Doutput=dependency-analysis.txt

# 生成依赖冲突报告
mvn enforcer:enforce -Doutput=enforcer-report.txt
```

## 最佳实践

### 1. 依赖管理策略
- 在父 POM 中使用 `dependencyManagement` 统一版本
- 定期检查和更新依赖版本
- 使用 `mvn dependency:tree` 分析依赖关系

### 2. 冲突预防
- 在项目初期就配置 Enforcer 插件
- 在 CI/CD 流程中集成依赖检查
- 建立依赖版本管理规范

### 3. 团队协作
- 统一团队的依赖管理策略
- 定期审查和更新依赖版本
- 建立依赖冲突处理流程

## 故障排除

### 常见问题

1. **依赖收敛检查失败**
   - 检查是否有多个版本的同一依赖
   - 使用 `dependency:tree` 分析依赖路径
   - 在 `dependencyManagement` 中统一版本

2. **自定义规则执行失败**
   - 检查规则配置是否正确
   - 确认插件版本是否兼容
   - 查看详细的错误日志

3. **构建被意外中断**
   - 检查 `fail` 配置是否为 `true`
   - 确认是否有依赖冲突需要解决
   - 使用 `-Denforcer.skip=true` 临时跳过

### 调试技巧

1. **启用详细日志**
   ```bash
   mvn enforcer:enforce -X
   ```

2. **单独运行规则**
   ```bash
   mvn enforcer:enforce -Drules=dependencyConvergence
   ```

3. **查看依赖树**
   ```bash
   mvn dependency:tree -Dverbose
   ```
