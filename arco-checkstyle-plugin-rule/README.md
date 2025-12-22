---
published: 2022.03.15
---

# 代码风格检查

## 概述

`arco-checkstyle-plugin-rule` 是一个为 Maven Checkstyle Plugin 提供自定义检查规则的插件。该插件内置了多套经过优化的代码检查规则配置，旨在简化项目中的代码质量检查配置，提供开箱即用的代码规范检查功能。

**重要说明**: 在 Zeka Stack 框架中，Checkstyle 插件默认开启，如果编译时检查到不符合规范要求的代码，会停止编译。如需紧急修复 bug，可临时关闭代码检查。

## 使用方式

### 1. 在 Zeka Stack 项目中使用

由于插件已内置到框架中，无需额外配置，直接运行 Maven 命令即可：

```bash
# 运行代码检查
mvn checkstyle:check

# 生成检查报告
mvn checkstyle:checkstyle

# 在验证阶段自动运行
mvn validate

# 临时关闭代码检查（紧急修复时使用）
mvn clean compile -Dcheckstyle.skip=true
```

### 2. 在独立项目中使用

如果需要在非 Zeka Stack 项目中使用，可以添加依赖：

```xml
<dependency>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-checkstyle-plugin-rule</artifactId>
    <version>${arco-maven-plugin.version}</version>
</dependency>
```

## 功能特性

### 🎯 内置多套检查规则

- **Zeka Stack 规则** (`zeka-stack-checks.xml`) - 推荐使用，专为 Zeka Stack 框架优化
- **Google 代码规范** (`google-checks.xml`) - 遵循 Google Java Style Guide
- **阿里巴巴规范** (`alibaba-strict-checks.xml`) - 基于阿里巴巴 Java 开发手册
- **自定义规范** (`customer-checks.xml`) - 可定制的检查规则
- **示例规范** (`sample-checks.xml`) - 基础检查规则示例

### 🔧 自定义检查规则

插件提供了以下自定义检查器：

#### 1. MethodLimitCheck - 方法数量限制检查

- **功能**: 限制类或接口中的方法数量
- **默认限制**: 每个类最多 30 个方法
- **用途**: 防止类过于庞大，提高代码可维护性

#### 2. IllegalNewCheck - 非法实例化检查

- **功能**: 检查是否使用了禁止实例化的类
- **特性**:
    - 支持配置禁止实例化的类列表
    - 支持排除特定包不进行检查
    - 支持反射调用的检测（待实现）
- **用途**: 防止直接实例化某些工具类或单例类

#### 3. JavadocCheck - Javadoc 注释检查

- **功能**: 检查 Javadoc 注释的完整性和格式
- **特性**: 支持自定义检查范围和忽略模式

## 框架集成配置

### Zeka Stack 框架内置配置

为了简化使用者的配置工作，本插件已内置到 Zeka Stack 框架中，提供开箱即用的代码检查功能。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>${maven-checkstyle-plugin.version}</version>
    <executions>
        <execution>
            <id>checkstyle-validation</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <logViolationsToConsole>true</logViolationsToConsole>
        <configLocation>zeka-stack-checks.xml</configLocation>
        <suppressionsLocation>suppressions.xml</suppressionsLocation>
        <suppressionsFileExpression>checkstyle.suppressions.file</suppressionsFileExpression>
        <encoding>${project.build.sourceEncoding}</encoding>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
        <cacheFile>${project.build.directory}/checkstyle/checkstyle-cachefile</cacheFile>
        <outputFile>${project.build.directory}/checkstyle/checkstyle-result.xml</outputFile>
        <rulesFiles>${project.build.directory}/checkstyle/checkstyle-rules.xml</rulesFiles>
        <linkXRef>false</linkXRef>
        <includes>dev/dong4j/**/*.java</includes>
        <includeResources>false</includeResources>
        <includeTestResources>false</includeTestResources>
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
    </configuration>
    <dependencies>
        <dependency>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-checkstyle-plugin-rule</artifactId>
            <version>${arco-maven-plugin.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### 配置说明

| 配置项                      | 说明           | 默认值                     |
|--------------------------|--------------|-------------------------|
| `configLocation`         | 检查规则配置文件     | `zeka-stack-checks.xml` |
| `suppressionsLocation`   | 抑制规则配置文件     | `suppressions.xml`      |
| `includes`               | 检查的文件模式      | `dev/dong4j/**/*.java`  |
| `failOnViolation`        | 发现违规时是否失败    | `true`                  |
| `logViolationsToConsole` | 是否在控制台输出违规信息 | `true`                  |

### 标准 Maven Checkstyle Plugin 配置对比

在介绍本插件的简化配置之前，先了解标准的 Maven Checkstyle Plugin 配置方式：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.3.0</version>
    <executions>
        <execution>
            <id>checkstyle-validation</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <logViolationsToConsole>true</logViolationsToConsole>
        <configLocation>checkstyle.xml</configLocation>
        <suppressionsLocation>checkstyle-suppressions.xml</suppressionsLocation>
        <encoding>${project.build.sourceEncoding}</encoding>
        <consoleOutput>true</consoleOutput>
        <failOnViolation>true</failOnViolation>
        <cacheFile>${project.build.directory}/checkstyle/checkstyle-cachefile</cacheFile>
        <outputFile>${project.build.directory}/checkstyle/checkstyle-result.xml</outputFile>
        <linkXRef>false</linkXRef>
        <includes>**/*.java</includes>
        <includeResources>false</includeResources>
        <includeTestResources>false</includeTestResources>
        <includeTestSourceDirectory>false</includeTestSourceDirectory>
    </configuration>
</plugin>
```

## 检查规则详解

### Zeka Stack 规则特性

#### 1. 文件级别检查

- **文件长度**: 最大 1500 行
- **行长度**: 最大 140 个字符
- **文件编码**: UTF-8
- **文件结尾**: 必须以空行结束

#### 2. 代码风格检查

- **命名规范**: 遵循 Java 标准命名约定
- **包名**: 小写字母，支持点分隔
- **类名**: 大写字母开头，驼峰命名
- **方法名**: 小写字母开头，驼峰命名
- **变量名**: 小写字母开头，驼峰命名
- **常量名**: 全大写，下划线分隔

#### 3. 代码质量检查

- **方法长度**: 最大 80 行
- **参数数量**: 最大 5 个参数
- **嵌套深度**: if 语句最多 3 层，for 循环最多 2 层
- **方法数量**: 每个类最多 30 个方法（自定义规则）

#### 4. 注释检查

- **类注释**: 必须包含 Javadoc 注释
- **方法注释**: private 方法需要完整的 Javadoc 注释
- **参数注释**: 必须包含参数说明
- **返回值注释**: 非 void 方法需要返回值说明

#### 5. 导入检查

- **避免通配符导入**: 禁止使用 `import java.util.*;`
- **移除未使用导入**: 自动检测并报告未使用的导入
- **导入顺序**: 按照标准顺序排列导入语句

#### 6. 代码结构检查

- **大括号位置**: 遵循标准 Java 风格
- **空格规范**: 运算符周围必须有空格
- **缩进规范**: 使用 4 个空格缩进
- **空行规范**: 类、方法之间需要空行分隔

### 自定义检查规则配置

#### MethodLimitCheck 配置示例

```xml
<module name="dev.dong4j.zeka.maven.plugin.checkstyle.checks.MethodLimitCheck">
    <property name="severity" value="warning"/>
</module>
```

#### IllegalNewCheck 配置示例

```xml
<module name="dev.dong4j.zeka.maven.plugin.checkstyle.checks.IllegalNewCheck">
    <property name="classes" value="java.lang.Boolean,java.lang.String"/>
    <property name="unpackage" value="com.example.test"/>
</module>
```

## 抑制规则配置

通过 `suppressions.xml` 文件可以配置忽略特定文件或目录的检查：

```xml
<?xml version="1.0"?>
<!DOCTYPE suppressions PUBLIC
    "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN"
    "https://checkstyle.org/dtds/suppressions_1_2.dtd">

<suppressions>
    <!-- 忽略测试目录 -->
    <suppress checks="[a-zA-Z0-9]*" files="[/\\]src[/\\]test[/\\]"/>

    <!-- 忽略自动生成的代码 -->
    <suppress checks="[a-zA-Z0-9]*" files="[\\/]build[\\/]generated-sources[\\/]"/>

    <!-- 忽略特定文件 -->
    <suppress checks="FileLength" files="dev.dong4j.*.StringUtils.java"/>
</suppressions>
```

## 检查报告

### 控制台输出

检查结果会在控制台显示，包括：

- 违规文件路径
- 违规行号
- 违规类型
- 违规描述

### XML 报告

详细的检查报告会生成到：

```
${project.build.directory}/checkstyle/checkstyle-result.xml
```

### 缓存文件

为了提高检查效率，会生成缓存文件：

```
${project.build.directory}/checkstyle/checkstyle-cachefile
```

## 最佳实践

### 1. 团队协作

- 在项目根目录配置统一的检查规则
- 使用版本控制管理检查规则文件
- 在 CI/CD 流程中集成代码检查

### 2. 规则定制

- 根据项目需求调整检查规则
- 使用抑制规则忽略特定情况
- 定期更新检查规则版本

### 3. 性能优化

- 启用缓存功能提高检查速度
- 合理配置检查范围
- 使用并行检查（如果支持）

## 故障排除

### 常见问题

1. **检查失败但代码看起来正确**
    - 检查是否有抑制规则配置
    - 确认检查规则版本是否匹配

2. **检查速度慢**
    - 启用缓存功能
    - 减少检查的文件范围
    - 排除不必要的目录

3. **自定义规则不生效**
    - 确认规则类路径正确
    - 检查规则配置语法
    - 查看 Maven 依赖是否正确

### 调试技巧

1. **启用详细日志**
   ```bash
   mvn checkstyle:check -X
   ```

2. **检查规则文件**
   ```bash
   mvn checkstyle:checkstyle -Dcheckstyle.config.location=your-config.xml
   ```

3. **查看缓存状态**
   ```bash
   mvn checkstyle:check -Dcheckstyle.cache.file=your-cache-file
   ```
