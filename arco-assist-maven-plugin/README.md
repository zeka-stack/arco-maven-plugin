# Arco Assist Maven Plugin

## 概述

`arco-assist-maven-plugin` 是 Arco 框架的核心辅助 Maven 插件,提供了一系列自动化构建功能,用于简化项目构建、部署和开发工作流。该插件包含多个
Mojo,每个 Mojo 负责特定的功能,帮助开发者自动处理构建过程中的各种任务。

## 主要功能

### 核心功能

#### 1. Assembly 配置动态生成 (`GenerateAssemblyConfigFileMojo`)

根据应用类型(Spring Boot 或 Spring Cloud)动态生成不同的打包配置文件,避免每个项目手动配置 assembly.xml。

#### 2. 可重现构建支持 (`AssemblyOutputTimestampMojo`)

为项目注入版本号绑定的固定时间戳,确保同一版本在任何环境下构建产物完全一致,解决可重现构建需求。

#### 3. 主类检测与模块类型判断 (`StarterMainClassPropertyMojo`)

自动判断模块是否为主模块(包含 Spring Boot starter class),主动注入 `start.class` 变量, 避免手动配置。

#### 4. 构建信息生成 (`GenerateProjectBuildInfoMojo`)

自动生成 `build-info.properties` 文件,包含项目基本信息和自定义属性,便于运行时获取构建信息。

#### 5. 时间戳属性注入 (`TimestampPropertyMojo`)

为构建过程注入格式化的时间戳属性,解决 Maven 内置时间戳的时区和兼容性问题。

### 智能控制功能

#### 6. 插件智能控制 (`SkipPluginMojo`)

根据模块类型自动启用/禁用相关插件,通过是否存在 starter class 自动判断并控制插件的生效状态。

### 辅助功能

- **Spring 环境配置管理** (`SpringProfilesActivePropertyMojo`):生成 `spring.profiles.active` 配置文件,默认值为 `local`
- **本地依赖清理** (`DeleteMavenDependenceMojo`):专门清理 zeka.stack 框架依赖,支持按包名、版本号精确清理和缓存文件清理
- **临时文件清理** (`DeleteTempFileMojo`):自动删除 checkstyle、pmd 等插件生成的临时文件(如 `pmd.xml`、`checkstyle-checker.xml` 等)
- **编译标识生成** (`CompiledProcessorMojo`):在编译阶段生成时间戳标识文件,用于构建流程状态追踪
- **文件上传** (`DeployFileMojo`):批量上传第三方依赖 JAR 包到 Maven 仓库,自动生成 POM 文件

## 插件执行阶段

### 核心功能

| Goal                                | 执行阶段               | 描述                           |
|-------------------------------------|--------------------|------------------------------|
| `generate-assembly-config`          | PACKAGE            | 动态生成 Assembly 打包配置           |
| `assembly-outputTimestamp-property` | INITIALIZE         | 注入项目版本绑定的 outputTimestamp 属性 |
| `mainclass-property`                | COMPILE            | 自动检测并配置主类属性                  |
| `generate-build-info`               | GENERATE_RESOURCES | 生成构建信息属性文件                   |
| `timestamp-property`                | VALIDATE           | 注入时间戳属性到 Maven 属性中           |

### 智能控制

| Goal          | 执行阶段     | 描述               |
|---------------|----------|------------------|
| `skip-plugin` | VALIDATE | 根据模块类型智能跳过不必要的插件 |

### 辅助功能

| Goal                      | 执行阶段             | 描述                             |
|---------------------------|------------------|--------------------------------|
| `profile-active-property` | GENERATE_SOURCES | 生成 Spring Profiles Active 配置文件 |
| `delete-temp-file`        | GENERATE_SOURCES | 删除临时文件(checkstyle、pmd 等)       |
| `generate-compiled-id`    | COMPILE          | 生成编译完成标识文件                     |
| `upload-file`             | DEPLOY           | 批量上传文件到 Maven 仓库               |
| `clear`                   | -                | 清理本地 Maven 仓库依赖(独立执行)          |

## 使用方式

### 1. 插件配置

在项目的 `pom.xml` 中添加插件配置:

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-assist-maven-plugin</artifactId>
    <version>${arco-maven-plugin.version}</version>
    <executions>
        <!-- 在 validate 阶段根据当前模块类型禁用部分插件 dev.dong4j.zeka.maven.plugin.helper.SkipPluginMojo -->
        <execution>
            <id>skip-plugin</id>
            <goals>
                <goal>skip-plugin</goal>
            </goals>
        </execution>
        <!-- 更友好的时间戳 dev.dong4j.zeka.maven.plugin.helper.TimestampPropertyMojo -->
        <execution>
            <id>timestamp-property</id>
            <goals>
                <goal>timestamp-property</goal>
            </goals>
            <configuration>
                <!-- 可注入到环境变量的时间戳 key -->
                <name>current.time</name>
                <pattern>MMddHHmm</pattern>
                <timeZone>GMT+8</timeZone>
                <locale>zh,CN</locale>
            </configuration>
        </execution>
        <!-- 部署模块配置处理(编译时扫描并找到启动类, 自动注入到 start.class 环境变量中) dev.dong4j.zeka.maven.plugin.helper.StarterMainClassPropertyMojo -->
        <execution>
            <id>mainclass-property</id>
            <goals>
                <goal>mainclass-property</goal>
            </goals>
            <configuration>
                <!-- 可注入到环境变量的时间戳 key -->
                <name>start.class</name>
            </configuration>
        </execution>
        <!-- 用于生成 build-info.properties dev.dong4j.zeka.maven.plugin.helper.GenerateProjectBuildInfoMojo -->
        <execution>
            <goals>
                <goal>generate-build-info</goal>
            </goals>
            <configuration>
                <additionalProperties>
                    <encoding.source>${project.encoding}</encoding.source>
                    <encoding.reporting>${project.encoding}</encoding.reporting>
                    <project.name>${project.build.finalName}</project.name>
                    <user.name>${user.name}</user.name>
                    <compiler.source.jdk>${java.version}</compiler.source.jdk>
                    <compiler.target.jdk>${java.version}</compiler.target.jdk>
                </additionalProperties>
            </configuration>
        </execution>
        <!-- 注入 profile.active 变量 (local) dev.dong4j.zeka.maven.plugin.helper.SpringProfilesActivePropertyMojo -->
        <execution>
            <id>profile-active-property</id>
            <goals>
                <goal>profile-active-property</goal>
            </goals>
        </execution>
        <!-- 动态生成 assembly.xml dev.dong4j.zeka.maven.plugin.helper.GenerateAssemblyConfigFileMojo -->
        <execution>
            <id>generate-assembly-config</id>
            <goals>
                <goal>generate-assembly-config</goal>
            </goals>
        </execution>
        <!-- 删除 checkstyle pmd 等临时文件 dev.dong4j.zeka.maven.plugin.helper.DeleteTempFileMojo -->
        <execution>
            <id>delete-temp-file</id>
            <goals>
                <goal>delete-temp-file</goal>
            </goals>
        </execution>
        <!-- 正常编译之后生成一个标识文件 dev.dong4j.zeka.maven.plugin.helper.CompiledProcessorMojo -->
        <execution>
            <id>generate-compiled-id</id>
            <goals>
                <goal>generate-compiled-id</goal>
            </goals>
        </execution>
        <!-- 注入 outputTimestamp.project.version, 由 dev.dong4j.zeka.maven.plugin.helper.mojo.AssemblyOutputTimestampMojo 注入 -->
        <execution>
            <id>assembly-outputTimestamp-property</id>
            <goals>
                <goal>assembly-outputTimestamp-property</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 2. 单独执行目标

#### 核心功能目标

```bash
# 1. 生成 Assembly 配置
mvn arco-assist:generate-assembly-config

# 2. 注入可重现构建时间戳
mvn arco-assist:assembly-outputTimestamp-property

# 3. 检测主类并配置属性
mvn arco-assist:mainclass-property

# 4. 生成构建信息
mvn arco-assist:generate-build-info

# 5. 注入时间戳属性
mvn arco-assist:timestamp-property -Dname=current.time -Dpattern=MMddHHmm -DtimeZone=GMT+8
```

#### 智能控制目标

```bash
# 智能跳过插件
mvn arco-assist:skip-plugin
```

#### 辅助功能目标

```bash
# 生成 Spring 环境配置
mvn arco-assist:profile-active-property

# 删除临时文件
mvn arco-assist:delete-temp-file

# 生成编译标识
mvn arco-assist:generate-compiled-id

# 上传第三方依赖
mvn arco-assist:upload-file -DrepositoryId=nexus -Durl=http://nexus.example.com/repository/maven-public/

# 清理本地依赖
mvn arco-assist:clear -Dname=all -Dversion=all
mvn arco-assist:clear -Dname=arco-meta -Dversion=2.0.0
mvn arco-assist:clear  # 清理无效目录和缓存
```

### 3. 配置参数

#### 构建信息生成配置

`generate-build-info` 目标会自动生成 `build-info.properties` 文件,包含以下默认属性:

- `build.group` - 项目的 groupId
- `build.artifact` - 项目的 artifactId
- `build.name` - 项目名称
- `build.version` - 项目版本
- `build.time` - 构建时间(格式:yyyy-MM-dd HH:mm:ss)

可以通过 `additionalProperties` 添加自定义属性:

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-assist-maven-plugin</artifactId>
    <configuration>
        <additionalProperties>
            <!-- 自定义属性会以 "build." 前缀写入到 build-info.properties -->
            <custom.property>custom-value</custom.property>
            <environment>${profile.active}</environment>
            <jdk.version>${java.version}</jdk.version>
        </additionalProperties>
    </configuration>
</plugin>
```

生成的 `build-info.properties` 文件示例:

```properties
build.group=dev.dong4j
build.artifact=demo-service
build.name=Demo Service
build.version=1.0.0
build.time=2024-01-15 14:30:25
build.custom.property=custom-value
build.environment=local
build.jdk.version=17
```

#### 跳过特定功能

```xml
<properties>
    <!-- 跳过构建信息生成 -->
    <build.info.skip>true</build.info.skip>
    <!-- 跳过临时文件删除 -->
    <delete.temp.file.skip>true</delete.temp.file.skip>
    <!-- 跳过 Assembly 配置生成 -->
    <assembly.config.skip>true</assembly.config.skip>
</properties>
```

## 实现细节

### 1. Assembly 配置动态生成 (`GenerateAssemblyConfigFileMojo`)

#### 开发背景

Spring Boot 的部署包可以分为 **瘦 JAR** 和 **胖 JAR** 两种方式:

- **传统方式**:使用 `spring-boot-maven-plugin` 直接打包,简单但定制性有限
- **框架选择**:使用 `maven-assembly-plugin` 实现更多定制化需求

**为什么选择 maven-assembly-plugin?**

使用 `maven-assembly-plugin` 可以实现更灵活的打包策略:

- 打包启动脚本到部署包中
- 利用 Docker 多层缓存特性的目录结构
- 依赖包排除等自定义优化逻辑
- 支持多种打包格式(tar.gz、zip 等)

**面临的问题**

如果每个微服务都需要配置 `assembly.xml`,会出现:

- 大量重复的配置文件
- 维护成本高,容易出错

**解决方案**

基于"约定大于配置"的设计理念,`GenerateAssemblyConfigFileMojo` 提供:

1. **自动生成**:框架内置通用的 assembly 配置,业务项目无需手动编写
2. **智能适配**:根据应用类型(Spring Boot vs Spring Cloud)生成不同配置
3. **自定义支持**:支持业务项目覆盖默认配置

#### 框架内置的 maven-assembly-plugin 配置

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>${maven-assembly-plugin.version}</version>
    <configuration>
        <!-- 避免将 assembly 构建产物附加到构建生命周期,防止错误的 deploy/install 行为 -->
        <attach>false</attach>
        <appendAssemblyId>false</appendAssemblyId>
        <!-- 由 arco-assist-maven-plugin 自动生成,避免每个项目手动配置 -->
        <descriptors>
            <descriptor>${project.build.directory}/arco-maven-plugin/assembly/assembly.xml</descriptor>
        </descriptors>
        <!-- 统一使用模块名,结合时间戳生成唯一的构建产物名称 -->
        <finalName>${project.build.finalName}_${current.time}</finalName>
        <!-- 可重现构建配置,确保同版本构建结果一致 -->
        <outputTimestamp>${outputTimestamp.project.version}</outputTimestamp>
        <!-- 没有打包配置时忽略打包 -->
        <ignoreMissingDescriptor>true</ignoreMissingDescriptor>
    </configuration>
    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

#### 动态配置生成逻辑

插件会根据应用类型自动生成不同的打包配置:

```java
ApplicationType applicationType = this.deduceFromDependencies();

String include = BOOT_PROPERTIES_INCLUDE;  // application*.yml
if (applicationType == ApplicationType.CLOUD) {
    include = CLOUD_PROPERTIES_INCLUDE;     // bootstrap.yml
}

// 根据 jar.repackage.skip 配置决定依赖排除策略
String dependencesExclude = repackageSkip ?
    DEFAULT_DEPENDENCES_EXCLUDES :
    FileUtils.readToString(ASSEMBLY_EXCLUDES_FILE_NAME);
```

#### 自定义配置支持

**优先级机制**:

1. **业务自定义配置**:如果 `/assembly` 存在 `assembly.xml`,优先使用
2. **框架默认配置**:否则使用框架生成的通用配置

**自定义配置路径**:

```
${project.basedir}/assembly/assembly.xml  → 使用业务自定义配置
不存在                          						→ 使用框架生成配置
```

> 如果存在自定义的 assembly.xml 配置, 插件会将这个配置写入到 `${project.build.directory}/arco-maven-plugin/assembly/assembly.xml`,
> 这样就不需要更改框架内默认的配置: `<descriptor>${project.build.directory}/arco-maven-plugin/assembly/assembly.xml</descriptor>`

#### 生成的配置文件位置

自动生成的 Assembly 配置文件位于:

```
${project.build.directory}/arco-maven-plugin/assembly/assembly.xml
```

#### 设计理念

说白了,就是想让开发者少写点配置文件。在没有这个插件之前,每个微服务模块都得自己写一份 `assembly.xml`,还要配置主类,几十个微服务就得维护几十份几乎一样的配置,实在是太痛苦了。

现在有了这个插件,开发者什么都不用管,插件会自动判断你的项目类型,然后生成对应的打包配置。如果你觉得默认的配置不够用,也可以放一个自己的
`assembly.xml` 进去,插件会优先用你的。

这样做的好处是:业务开发者不用关心这些基础设施的配置细节,专心写业务代码就行了。而且万一哪天要调整打包策略,只要在框架层面改一次,所有项目都能受益。

这种思路其实就是 Spring Boot 那套"约定大于配置",但我们把它扩展到了整个构建流程, 乃至整个框架。

---

### 2. 可重现构建支持 (`AssemblyOutputTimestampMojo`)

#### 开发背景

在传统构建过程中,打包产物(如 JAR、ZIP、TAR)会包含文件时间戳等可能导致差异的因素。即使源代码完全相同,在不同时间、不同机器上执行构建,得到的产物二进制结果可能不同,难以保证一致性与可追溯性。

#### 可重现构建的重要性

- **安全性**:防止供应链攻击,确保二进制产物与源代码一致
- **合规性**:开源项目越来越强调 reproducible build(如 Debian、Apache)
- **一致性**:避免因为时间戳或环境差异导致的 CI/CD 构建产物不一致
- **可追溯性**:同一版本的产物在任何地方重构都完全一致,方便问题排查和验证

#### 解决方案

`AssemblyOutputTimestampMojo` 采用 **版本号绑定固定时间戳** 的方案:

```xml
<properties>
    <!-- 每个版本固定一个 outputTimestamp, 在发布新版本时新增一个配置即可 -->
    <outputTimestamp.1.0.0>2025-05-01T00:00:00Z</outputTimestamp.1.0.0>
    <outputTimestamp.2.0.0>2025-08-15T00:00:00Z</outputTimestamp.2.0.0>
</properties>

<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <outputTimestamp>${outputTimestamp.project.version}</outputTimestamp>
    </configuration>
</plugin>
```

插件会自动注入 `outputTimestamp.project.version` 属性,确保:

- 同一版本号在任何环境下构建产物完全一致
- 不同版本号有不同的时间戳,便于区分

关于 `maven-assembly-plugin` 的 `outputTimestamp` 配置的详细说明可参考 [可重现构建说明文档](assembly.outputTimestamp.md).

---

### 3. 主类检测与模块类型判断 (`StarterMainClassPropertyMojo`)

#### 开发背景

在微服务项目中,每个可执行的服务模块都需要配置主类(mainClass),传统的做法是在每个模块的 `maven-jar-plugin` 或 `spring-boot-maven-plugin`
中手动指定启动类:

```xml
<!-- 传统做法:每个服务都要手动配置 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <configuration>
        <archive>
            <manifest>
                <mainClass>com.example.service.Application</mainClass>  <!-- 手动配置 -->
            </manifest>
        </archive>
    </configuration>
</plugin>
```

**问题在于**:

- 每个微服务都需要重复配置主类
- 主类名变更时需要同步更新配置
- 容易遗漏或配置错误

**解决思路**

其实通过 Maven 插件很容易就能检测到启动类。既然能自动检测,为什么还要每个服务都手动配置?这完全可以自动化!

现在最新的 `spring-boot-maven-plugin` 也实现了类似功能,不再需要手动配置启动类了。看来"为每个服务配置启动类的时代应该翻篇了"。

#### 实现机制

**两阶段检测流程:**

**第一阶段(SkipPluginMojo - VALIDATE 阶段):**

```java
// 检测启动类并存储到系统属性
if (moduleType.equals(ModuleType.DELOPY)) {
    JavaFile javaFile = this.mainClass(this.project);  // 调用 JavaFileScanner
    // 存储格式:{artifactId}_START_CLASS
    System.setProperty(this.getProject().getModel().getArtifactId() + "_START_CLASS",
                      javaFile.getClassName());
}
```

**第二阶段(StarterMainClassPropertyMojo - COMPILE 阶段):**

```java
// 从系统属性中获取启动类名并注入到 Maven 属性
String startClassName = System.getProperty(this.getProject().getModel().getArtifactId() + "_START_CLASS");
this.defineProperty("start.class", startClassName);
```

**JavaFileScanner 检测逻辑:**

插件使用 JavaParser 库扫描源码,查找被 `@SpringBootApplication` 或 `@EnableAutoConfiguration` 标识的类:

```java
// 解析类上的注解
@Override
public void visit(ClassOrInterfaceDeclaration declaration, JavaFile javaFile) {
    List<AnnotationExpr> annotationList = declaration.getAnnotations();
    for (AnnotationExpr annotation : annotationList) {
        if ("SpringBootApplication".equals(annotation.getNameAsString()) ||
            "EnableAutoConfiguration".equals(annotation.getNameAsString())) {
            javaFile.setMainClass(true);
            declaration.getFullyQualifiedName().ifPresent(javaFile::setClassName);
            break;
        }
    }
}
```

#### 框架集成应用

**自动化的 maven-jar-plugin 配置:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-jar-plugin</artifactId>
    <version>${maven-jar-plugin.version}</version>
    <configuration>
        <archive>
            <manifest>
                <!-- 不再需要手动配置,自动注入 -->
                <mainClass>${start.class}</mainClass>
            </manifest>
        </archive>
    </configuration>
</plugin>
```

**模块类型判断:**

基于启动类检测结果,插件将模块分为四种类型:

- **POM 模块**:聚合模块,不包含业务代码
- **空模块**:无 Java 文件的模块
- **部署模块**:包含 Spring Boot 主类,启用构建信息生成、打包等功能
- **依赖模块**:普通的依赖库模块

**额外的自动化配置:**

当检测到启动类时,插件还会自动设置:

- `maven.install.skip=true` - 部署模块不需要安装到本地仓库
- `maven.deploy.skip=true` - 部署模块不需要发布到远程仓库

**为什么要跳过 install 和 deploy?**

虽然这看起来是个"鸡毛蒜皮"的小事,但体现了对技术的严谨态度:

**技术逻辑**:

- **启动模块**:包含 `main` 方法的可执行程序,是最终的运行产物,不应该被其他模块依赖
- **依赖模块**:提供公共功能的库,应该发布到仓库供其他开发者使用

**实际问题**:

- 启动模块的 JAR 包被 install 到本地仓库纯属浪费空间
- 启动模块被 deploy 到 Maven 仓库更是毫无意义,谁会去依赖一个启动类?
- 在大型项目中,几十个微服务的启动包都发布到仓库,徒增管理成本

---

虽然对于大多数开发者来说,可能并不关心哪些应该被 install,哪些应该 deploy。但作为框架设计者,应该替开发者做正确的事情。这就像垃圾分类一样,看似无关紧要,但体现了对技术的尊重和严谨。

正确的做法应该是:

- **Common 模块、Utils 模块**:install + deploy ✅
- **Service 启动模块**:skip install + skip deploy ✅

这样的自动化不仅减少了配置工作,更重要的是让项目结构更加清晰合理。

#### 设计理念

这个功能的核心思想是:**能自动检测的就不要手动配置**。

以前开发者需要:

1. 创建启动类
2. 在 `maven-jar-plugin` 中配置主类
3. 设置跳过 install/deploy

现在只需要:

1. 创建启动类(带上 `@SpringBootApplication` 注解)

其他的一切都由插件自动处理。

---

### 4. 构建信息生成 (`GenerateProjectBuildInfoMojo`)

#### 开发背景

Spring Boot 的 `spring-boot-maven-plugin` 会生成一个非常有用的 `build-info.properties` 文件:

```properties
build.artifact=myapp
build.group=com.example
build.name=myapp
build.version=1.0.0
build.time=2025-08-31T10:15:30Z
```

**这个文件的价值:**

1. **运行时内省**:应用可以通过 `BuildProperties` 类获取自身构建信息
2. **Actuator 集成**:自动暴露在 `/actuator/info` 端点,方便监控
3. **版本追踪**:运维人员可以直接看到"到底部署的是哪个版本"
4. **故障排查**:通过构建时间快速定位问题版本

**问题所在**

由于框架没有使用 `spring-boot-maven-plugin`(选择了更灵活的 `maven-assembly-plugin`),就失去了这个有用的功能。

但我个人认为 `build-info.properties` 是一个很有价值的内省文件,特别是在微服务架构中,每个服务都能报告自己的版本信息是非常重要的运维能力。

所以决定复刻这个功能,也就有了 `GenerateProjectBuildInfoMojo`。

#### 实现功能

**默认生成的属性:**

- `build.group` - 项目的 groupId
- `build.artifact` - 项目的 artifactId
- `build.name` - 项目名称
- `build.version` - 项目版本
- `build.time` - 构建时间(格式:yyyy-MM-dd HH:mm:ss)

**自定义属性支持:**

```xml
<configuration>
    <additionalProperties>
        <encoding.source>${project.encoding}</encoding.source>
        <environment>local</environment>
        <jdk.version>${java.version}</jdk.version>
    </additionalProperties>
</configuration>
```

生成的文件内容:

```properties
build.group=com.example
build.artifact=user-service
build.name=user-service
build.version=1.0.0
build.time=2025-08-31 18:30:45
build.encoding.source=UTF-8
build.environment=local
build.jdk.version=17
```

**文件位置:**

```
${project.build.outputDirectory}/META-INF/build-info.properties
```

#### 集成应用

**与 Spring Boot Actuator 配合:**

```java
@RestController
public class InfoController {

    @Autowired(required = false)
    private BuildProperties buildProperties;

    @GetMapping("/version")
    public Map<String, String> version() {
        if (buildProperties != null) {
            return Map.of(
                "name", buildProperties.getName(),
                "version", buildProperties.getVersion(),
                "time", buildProperties.getTime().toString()
            );
        }
        return Map.of("error", "build info not available");
    }
}
```

**运维监控:**

通过 `/actuator/info` 端点可以直接看到:

```json
{
  "build": {
    "artifact": "user-service",
    "name": "user-service",
    "time": "2025-08-31T18:30:45Z",
    "version": "1.0.0",
    "group": "com.example"
  }
}
```

#### 设计理念

虽然框架为了更大的灵活性选择了 `maven-assembly-plugin` 而不是 `spring-boot-maven-plugin`,但不应该因此失去构建信息追踪这个重要能力。

在微服务时代,能够清楚知道每个服务的版本信息,对于:

- **故障排查**:快速定位问题版本
- **灰度发布**:确认部署的版本正确性
- **监控告警**:版本一致性检查

都是非常重要的。所以这个"复刻"是很有必要的。

---

### 5. 时间戳属性注入机制 (`TimestampPropertyMojo`)

#### 开发背景

在项目构建过程中,我们经常需要在打包时添加时间戳后缀来标识构建版本。Maven 提供了内置的时间戳功能:

```xml
<properties>
    <maven.build.timestamp.format>yyyyMMddHHmm</maven.build.timestamp.format>
</properties>
```

然后可以通过 `${maven.build.timestamp}` 变量使用这个时间戳。

#### 遇到的问题

然而,在实际使用中发现了几个严重问题:

1. **时区问题**:`maven.build.timestamp`
   存在8小时时差问题,这在中国时区尤其明显 [参考链接](https://blog.csdn.net/zzzgd_666/article/details/118756366)
2. **插件兼容性问题**:当使用其他插件(如 docker-maven-plugin)时,会出现 "The template variable 'timestamp' has no value" 错误
3. **执行时机问题**:需要特定的 Maven 命令组合才能正确获取时间戳变量

#### 解决方案

为了解决这些问题,我们开发了 `TimestampPropertyMojo`,它提供了:

- **正确的时区支持**:支持自定义时区配置(如 GMT+8)
- **灵活的格式配置**:支持任意的日期时间格式
- **稳定的变量注入**:在 VALIDATE 阶段就注入属性,确保后续插件都能正确使用
- **国际化支持**:支持不同地区的 locale 配置

#### 实际应用

目前这个功能主要用于 `maven-assembly-plugin` 的配置中:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <configuration>
        <!-- 使用 TimestampPropertyMojo 注入的时间戳 -->
        <finalName>${project.build.finalName}_${current.time}</finalName>
        <!-- 使用 AssemblyOutputTimestampMojo 注入的可重现构建时间戳 -->
        <outputTimestamp>${outputTimestamp.${project.version}}</outputTimestamp>
    </configuration>
</plugin>
```

通过 `TimestampPropertyMojo` 的配置:

```xml
<execution>
    <id>timestamp-property</id>
    <goals>
        <goal>timestamp-property</goal>
    </goals>
    <configuration>
        <name>current.time</name>
        <pattern>MMddHHmm</pattern>
        <timeZone>GMT+8</timeZone>
        <locale>zh,CN</locale>
    </configuration>
</execution>
```

这样就能生成格式为 `project-name_01151430.tar.gz` 的打包文件,时间戳准确反映了中国时区的构建时间。

---

### 6. 智能插件控制 (`SkipPluginMojo`)

#### 开发背景

同样是践行"**约定大于配置**"的设计哲学。框架内置了大量 Maven 插件,每个插件都有默认配置,但实际使用中会遇到几个问题:

**框架插件管理的挑战:**

1. **默认配置不适用**:内置默认配置无法满足所有业务要求
2. **插件无法排除**:Maven 无法像依赖一样排除不需要的插件
3. **手动配置繁琐**:每个模块都要单独配置插件的启用/禁用

**实际应用场景:**

框架默认启用 checkstyle 和 pmd 插件进行代码质量检查,代码格式不规范会在编译阶段报错。但遇到紧急情况时:

```bash
# 线上出现严重 bug,修复后需要立即上线
# 谁还管代码规不规范,先解决线上问题再说!
mvn clean package -Dcheckstyle.skip=true -Dpmd.skip=true
```

这时 skip 功能就非常有用了。

#### 设计方案

基于模块类型提供**智能化的插件控制**:

1. **自动化判断**:根据模块类型自动启用/禁用相应插件
2. **手动覆盖**:用户可以通过 skip 参数手动控制
3. **灵活配置**:支持项目级和命令行级的配置覆盖

#### 模块类型检测机制

```java
private ModuleType setModuleType() {
    String packaging = this.getProject().getPackaging();

    ModuleType moduleType;
    if (PackageType.POM.name().equalsIgnoreCase(packaging)) {
        moduleType = ModuleType.POM;           // POM 聚合模块
    } else if (this.noJavaFile(this.getProject())) {
        moduleType = ModuleType.EMPTY;         // 空模块(无 Java 文件)
    } else if (this.isDeployModel(this.project)) {
        moduleType = ModuleType.DELOPY;        // 部署模块(包含主类)
    } else {
        moduleType = ModuleType.DEPEND;        // 依赖模块
    }

    return moduleType;
}
```

#### 智能插件控制策略

**POM/空模块**:

```java
// 这些模块不包含业务代码,跳过代码检查
this.skipPlugin(Plugins.SKIP_CHECKSTYLE);    // 跳过 checkstyle
this.skipPlugin(Plugins.SKIP_PMD);           // 跳过 pmd
this.skipPlugin(Plugins.SKIP_ASSEMBLY);      // 跳过 assembly
this.skipPlugin(Plugins.SKIP_GITCOMMITID);   // 跳过 git-commit-id
```

**部署模块**:

```java
// 启动类模块,启用所有相关功能
this.enablePlugin(Plugins.SKIP_BUILD_INFO);          // 启用构建信息生成
this.enablePlugin(Plugins.SKIP_ASSEMBLY_CONFIG);     // 启用 assembly 配置
this.enablePlugin(Plugins.SKIP_LAUNCH_SCRIPT);       // 启用启动脚本
this.enablePlugin(Plugins.SKIP_DOCKERFILE_SCRIPT);   // 启用 dockerfile 生成
```

**依赖模块**:

```java
// 普通依赖库,跳过部署相关功能
this.skipPlugin(Plugins.SKIP_GITCOMMITID);   // 跳过 git 信息
// 保持代码检查:checkstyle、pmd 等
```

#### 实际应用场景

**日常开发:** 一切自动化,开发者无需关心

```bash
mvn clean package  # 插件自动判断模块类型,启用对应功能
```

**紧急发布:** 快速跳过耗时检查

```bash
mvn clean package -Dcheckstyle.skip=true -Dpmd.skip=true
```

**特殊需求:** 精确控制插件行为

```bash
# 强制为依赖模块生成构建信息
mvn clean package -Dbuild.info.skip=false

# 跳过部署模块的 assembly 打包
mvn clean package -Dassembly.config.skip=true
```

#### 设计理念

说白了,就是想让插件更聪明一点。

平时你什么都不用管,插件会根据你的项目类型自动开启或关闭对应的功能。POM 模块就不跑代码检查了,启动模块就自动生成构建信息,依赖模块就跳过打包配置,该干啥干啥。

但万一你有特殊需求,或者遇到紧急情况(比如线上出 bug 急着修复上线),你随时可以用 `-D` 参数覆盖这些自动判断。框架帮你做决定,但决定权还是在你手里。

## 辅助功能

### 1. Spring 环境配置管理 (`SpringProfilesActivePropertyMojo`)

#### 开发背景

这个功能的设计初衷比较特别,不是为了技术便利,而是为了 **增加环境切换的操作成本**。

**问题场景**:

在 zeka.stack 脚手架中,yml 配置文件中的 `spring.profiles.active` 在非生产环境不会生效,默认使用 `application-local.yml`
。如果开发者想切换到其他环境(如
`prev`),需要手动修改 `target/arco-maven-plugin/profile` 目录下的 `spring.profiles.active` 文件。

**设计目的**:

在业务开发过程中,原则上不允许连接非本地开发环境,但很难控制开发人员随意连接其他环境的行为。所以这个插件故意增加了切换环境的操作成本——如果不了解这个机制,大概率无法正常切换到非
`local` 环境。

**实际效果**:

- **默认安全**:开发者默认只能连接本地环境
- **故意增加摩擦**:切换环境需要额外的操作步骤
- **提高门槛**:只有了解框架机制的人才能切换环境

#### 实现机制

生成 `spring.profiles.active` 配置文件到 `${project.build.directory}/arco-maven-plugin/profile/` 目录:

- **默认行为**:如果文件不存在,自动创建并写入 `local`
- **避免覆盖**:如果文件已存在,不会重写(保护用户自定义配置)
- **Maven 属性注入**:同时将 `profile.active=local` 注入到 Maven 属性中

**环境切换步骤**:

1. 找到 `${project.build.directory}/arco-maven-plugin/profile/spring.profiles.active` 文件
2. 将内容从 `local` 改为目标环境(如 `prev`)
3. 重新启动应用

#### 后续考虑

通过使用者反馈了解到,并不是每个公司都会这么严格限制环境访问。所以后续考虑删除这个功能,将主动权交给使用者。至于是否允许随意切换环境,让管理者通过其他方式控制,比如通过绩效
😂。

毕竟技术手段强制限制不如管理制度约束来得直接有效。

### 2. 本地依赖清理 (`DeleteMavenDependenceMojo`)

专门用于清理 `~/.m2/repository/dev/dong4j` 下的 zeka.stack 框架依赖:

```bash
# 清理所有 zeka.stack 框架依赖
mvn arco-assist:clear -Dname=all -Dversion=all

# 清理指定包的所有版本
mvn arco-assist:clear -Dname=arco-meta -Dversion=all

# 清理指定包的指定版本
mvn arco-assist:clear -Dname=arco-meta -Dversion=2.0.0

# 清理无效目录和 lastUpdated 缓存文件
mvn arco-assist:clear
```

### 3. 临时文件清理 (`DeleteTempFileMojo`)

自动删除构建目录下的插件临时文件:

- `pmd` 目录及相关文件
- `checkstyle` 目录及相关文件
- `pmd.xml`、`checkstyle-checker.xml`、`checkstyle-suppressions.xml` 等配置文件

### 4. 编译标识生成 (`CompiledProcessorMojo`)

在 `COMPILE` 阶段生成编译完成标识:

- **文件位置**:`${project.build.directory}/maven-status/maven-compiler-plugin/compile/identify/checked`
- **文件内容**:当前时间戳 (`System.currentTimeMillis()`)
- **用途**:供其他构建工具或脚本判断编译状态

### 5. 文件上传 (`DeployFileMojo`)

支持批量上传第三方依赖到 Maven 仓库:

```java
// 从项目依赖中创建 Maven 项目
List<MavenProject> uploadProjects = new ArrayList<>();
uploadDependency.forEach(d -> uploadProjects.add(this.createMavenProject(d)));

// 为每个依赖生成 POM 文件并上传
uploadProjects.forEach(p -> {
    Artifact artifact = p.getArtifact();
    if (this.generatePom) {
        File pom = this.generatePomFile(p);
        ProjectArtifactMetadata metadata = new ProjectArtifactMetadata(artifact, pom);
        artifact.addMetadata(metadata);
    }
    this.artifactDeployer.deploy(buildingRequest, artifactRepository, artifacts);
});
```

## 核心依赖

- **JavaParser**:用于 Java 代码解析和主类检测
- **Maven Artifact Transfer**:用于构件上传和仓库操作
- **Arco Maven Plugin Common**:提供通用功能和工具类

## 配置项说明

### 跳过配置项

#### 智能控制机制

`SkipPluginMojo` 会根据模块类型自动控制插件的启用/跳过状态:

**模块类型分类:**

- **POM 模块**:`packaging = pom`
- **空模块**:无 Java 文件的模块
- **部署模块**:包含 Spring Boot 主类的可执行模块
- **依赖模块**:普通的依赖库模块

#### 核心功能跳过配置

| 配置项                             | 默认值     | 智能控制 | 描述               | 使用方式                                    |
|---------------------------------|---------|------|------------------|-----------------------------------------|
| `assembly.config.skip`          | `true`  | ✅    | 跳过 Assembly 配置生成 | `-Dassembly.config.skip=false`          |
| `assembly.outputTimestamp.skip` | `true`  | ❌    | 跳过可重现构建时间戳注入     | `-Dassembly.outputTimestamp.skip=false` |
| `build.mainclass.property.skip` | `true`  | ✅    | 跳过主类属性配置         | `-Dbuild.mainclass.property.skip=false` |
| `build.info.skip`               | `true`  | ✅    | 跳过构建信息生成         | `-Dbuild.info.skip=false`               |
| `timestamp.property.skip`       | `false` | ❌    | 跳过时间戳属性注入        | `-Dtimestamp.property.skip=true`        |

#### 智能控制跳过配置

| 配置项                | 默认值     | 描述            | 使用方式                      |
|--------------------|---------|---------------|---------------------------|
| `skip.plugin.skip` | `false` | 跳过智能插件控制(不推荐) | `-Dskip.plugin.skip=true` |

#### 辅助功能跳过配置

| 配置项                              | 默认值     | 描述         | 使用方式                                     |
|----------------------------------|---------|------------|------------------------------------------|
| `build.profile.active.file.skip` | `true`  | 跳过环境配置文件生成 | `-Dbuild.profile.active.file.skip=false` |
| `delete.temp.file.skip`          | `false` | 跳过临时文件删除   | `-Ddelete.temp.file.skip=true`           |
| `compiled.id.skip`               | `true`  | 跳过编译标识生成   | `-Dcompiled.id.skip=false`               |

#### 第三方插件跳过配置(由 SkipPluginMojo 控制)

| 配置项                      | 受影响插件                     | 模块类型控制策略                                 | 使用方式                             |
|--------------------------|---------------------------|------------------------------------------|----------------------------------|
| `checkstyle.skip`        | `maven-checkstyle-plugin` | POM/空模块:自动跳过<br/>其他模块:默认启用               | `-Dcheckstyle.skip=true`         |
| `pmd.skip`               | `maven-pmd-plugin`        | POM/空模块:自动跳过<br/>其他模块:默认启用               | `-Dpmd.skip=true`                |
| `assembly.skipAssembly`  | `maven-assembly-plugin`   | POM/空模块:自动跳过<br/>部署模块:自动启用<br/>依赖模块:默认跳过 | `-Dassembly.skipAssembly=false`  |
| `maven.gitcommitid.skip` | `git-commit-id-plugin`    | POM/空模块/依赖模块:自动跳过<br/>部署模块:默认启用          | `-Dmaven.gitcommitid.skip=false` |

#### 高级功能跳过配置(手动控制)

| 配置项                   | 默认值    | 描述              | 使用方式                          | 备注             |
|-----------------------|--------|-----------------|-------------------------------|----------------|
| `jar.repackage.skip`  | `true` | Spring Boot 重打包 | `-Djar.repackage.skip=false`  | 需手动开启,影响依赖打包策略 |
| `makeself.skip`       | `true` | 自解压部署包生成        | `-Dmakeself.skip=false`       | 需手动开启          |
| `launch.script.skip`  | `true` | 启动脚本生成          | `-Dlaunch.script.skip=false`  | 仅部署模块自动启用      |
| `dockerfile.skip`     | `true` | Dockerfile 生成   | `-Ddockerfile.skip=false`     | 仅部署模块自动启用      |
| `publish-single.skip` | `true` | 单文件发布           | `-Dpublish-single.skip=false` | 仅部署模块自动启用      |

#### 模块类型与插件控制矩阵

| 模块类型       | 代码检查 | Assembly | Git 信息 | 构建信息 | 主类检测 | 启动脚本 | Docker |
|------------|------|----------|--------|------|------|------|--------|
| **POM 模块** | ❌ 跳过 | ❌ 跳过     | ❌ 跳过   | ❌ 跳过 | ❌ 跳过 | ❌ 跳过 | ❌ 跳过   |
| **空模块**    | ❌ 跳过 | ❌ 跳过     | ❌ 跳过   | ❌ 跳过 | ❌ 跳过 | ❌ 跳过 | ❌ 跳过   |
| **部署模块**   | ✅ 启用 | ✅ 启用     | ✅ 启用   | ✅ 启用 | ✅ 启用 | ✅ 启用 | ✅ 启用   |
| **依赖模块**   | ✅ 启用 | ❌ 跳过     | ❌ 跳过   | ❌ 跳过 | ❌ 跳过 | ❌ 跳过 | ❌ 跳过   |

### 生成文件路径

| 文件类型        | 默认路径                                                                                     |
|-------------|------------------------------------------------------------------------------------------|
| Assembly 配置 | `${project.build.directory}/arco-maven-plugin/assembly/assembly.xml`                     |
| 构建信息        | `${project.build.outputDirectory}/META-INF/build-info.properties`                        |
| 环境配置        | `${project.build.directory}/arco-maven-plugin/profile/spring.profiles.active`            |
| 编译标识        | `${project.build.directory}/maven-status/maven-compiler-plugin/compile/identify/checked` |

### 时间戳相关配置

| 配置项                               | 描述                        |
|-----------------------------------|---------------------------|
| `outputTimestamp.project.version` | 可重现构建时间戳(版本号绑定)           |
| `current.time`                    | 当前时间戳(可配置格式和时区)           |
| `maven.build.timestamp`           | Maven 内置时间戳(存在时区问题,不推荐使用) |

## 最佳实践

### 1. 在父 POM 中统一配置

建议在父 POM 中配置插件,子模块自动继承:

```xml
<pluginManagement>
    <plugins>
        <plugin>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-assist-maven-plugin</artifactId>
            <version>${arco-maven-plugin.version}</version>
            <executions>
                <!-- 在 validate 阶段根据当前模块类型禁用部分插件 dev.dong4j.zeka.maven.plugin.helper.SkipPluginMojo -->
                <execution>
                    <id>skip-plugin</id>
                    <goals>
                        <goal>skip-plugin</goal>
                    </goals>
                </execution>
                <!-- 更友好的时间戳 dev.dong4j.zeka.maven.plugin.helper.TimestampPropertyMojo -->
                <execution>
                    <id>timestamp-property</id>
                    <goals>
                        <goal>timestamp-property</goal>
                    </goals>
                    <configuration>
                        <!-- 可注入到环境变量的时间戳 key -->
                        <name>current.time</name>
                        <pattern>MMddHHmm</pattern>
                        <timeZone>GMT+8</timeZone>
                        <locale>zh,CN</locale>
                    </configuration>
                </execution>
                <!-- 部署模块配置处理(编译时扫描并找到启动类, 自动注入到 start.class 环境变量中) dev.dong4j.zeka.maven.plugin.helper.StarterMainClassPropertyMojo -->
                <execution>
                    <id>mainclass-property</id>
                    <goals>
                        <goal>mainclass-property</goal>
                    </goals>
                    <configuration>
                        <!-- 可注入到环境变量的时间戳 key -->
                        <name>start.class</name>
                    </configuration>
                </execution>
                <!-- 用于生成 build-info.properties dev.dong4j.zeka.maven.plugin.helper.GenerateProjectBuildInfoMojo -->
                <execution>
                    <goals>
                        <goal>generate-build-info</goal>
                    </goals>
                    <configuration>
                        <additionalProperties>
                            <encoding.source>${project.encoding}</encoding.source>
                            <encoding.reporting>${project.encoding}</encoding.reporting>
                            <project.name>${project.build.finalName}</project.name>
                            <user.name>${user.name}</user.name>
                            <compiler.source.jdk>${java.version}</compiler.source.jdk>
                            <compiler.target.jdk>${java.version}</compiler.target.jdk>
                        </additionalProperties>
                    </configuration>
                </execution>
                <!-- 注入 profile.active 变量 (local) dev.dong4j.zeka.maven.plugin.helper.SpringProfilesActivePropertyMojo -->
                <execution>
                    <id>profile-active-property</id>
                    <goals>
                        <goal>profile-active-property</goal>
                    </goals>
                </execution>
                <!-- 动态生成 assembly.xml dev.dong4j.zeka.maven.plugin.helper.GenerateAssemblyConfigFileMojo -->
                <execution>
                    <id>generate-assembly-config</id>
                    <goals>
                        <goal>generate-assembly-config</goal>
                    </goals>
                </execution>
                <!-- 删除 checkstyle pmd 等临时文件 dev.dong4j.zeka.maven.plugin.helper.DeleteTempFileMojo -->
                <execution>
                    <id>delete-temp-file</id>
                    <goals>
                        <goal>delete-temp-file</goal>
                    </goals>
                </execution>
                <!-- 正常编译之后生成一个标识文件 dev.dong4j.zeka.maven.plugin.helper.CompiledProcessorMojo -->
                <execution>
                    <id>generate-compiled-id</id>
                    <goals>
                        <goal>generate-compiled-id</goal>
                    </goals>
                </execution>
                <!-- 注入 outputTimestamp.project.version, 由 dev.dong4j.zeka.maven.plugin.helper.mojo.AssemblyOutputTimestampMojo 注入 -->
                <execution>
                    <id>assembly-outputTimestamp-property</id>
                    <goals>
                        <goal>assembly-outputTimestamp-property</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</pluginManagement>
```

### 2. 按模块类型定制配置

对于不同类型的模块,可以通过 profile 或属性配置不同的插件行为:

```xml
<profiles>
    <profile>
        <id>deployment-module</id>
        <properties>
            <!-- 启用部署模块的核心功能(如果是包含启动类的模块的话, 插件其实会自动判断来启动对应的插件) -->
            <assembly.config.skip>false</assembly.config.skip>
            <build.info.skip>false</build.info.skip>
            <build.mainclass.property.skip>false</build.mainclass.property.skip>
        </properties>
    </profile>
</profiles>
```

### 3. 智能跳过配置的最佳实践

#### 推荐的跳过配置策略

**对于多模块项目:**

```xml
<!-- 在父 POM 中统一配置,依赖智能控制 -->
<properties>
    <!-- 让 SkipPluginMojo 自动判断模块类型 -->
    <!-- 不要手动设置这些配置,除非有特殊需求 -->
    <!-- <assembly.config.skip>false</assembly.config.skip> -->
    <!-- <build.info.skip>false</build.info.skip> -->
</properties>
```

**对于单个模块的临时跳过:**

```bash
# 临时跳过某个功能
mvn clean package -Dassembly.config.skip=true

# 强制启用某个功能(覆盖智能判断)
mvn clean package -Dbuild.info.skip=false
```

**开发环境 vs 生产环境:**

```xml
<profiles>
    <profile>
        <id>dev</id>
        <properties>
            <!-- 开发环境启用所有检查 -->
            <checkstyle.skip>fasle</checkstyle.skip>
            <pmd.skip>fasle</pmd.skip>
        </properties>
    </profile>
    <profile>
        <id>prod</id>
        <properties>
            <!-- 生产环境跳过一些耗时功能 -->
            <checkstyle.skip>true</checkstyle.skip>
            <pmd.skip>true</pmd.skip>
        </properties>
    </profile>
</profiles>
```

## 故障排除

### 常见问题

1. **Assembly 配置生成失败**
    - 检查 `jar.repackage.skip` 属性配置
    - 确保项目依赖配置正确
    - 检查是否正确检测到应用类型(Spring Boot vs Spring Cloud)

2. **可重现构建时间戳问题**
    - 检查 `outputTimestamp.project.version` 属性是否正确配置
    - 确认版本号格式是否符合预期

3. **插件无法检测到主类**
    - 确保主类使用了 `@SpringBootApplication` 或 `@EnableAutoConfiguration` 注解
    - 检查 Java 源码路径是否正确

4. **构建信息生成失败**
    - 检查 `build-info.properties` 文件输出路径权限
    - 确认 `additionalProperties` 配置格式正确

5. **时间戳格式问题**
    - 检查时区配置是否正确(timeZone=GMT+8)
    - 确认日期格式 pattern 是否有效

6. **文件上传失败**
    - 检查仓库 URL 和认证配置
    - 确保依赖列表格式正确

### 调试模式

启用 Maven 调试模式查看详细日志:

```bash
# 调试核心功能
mvn arco-assist:generate-assembly-config -X
mvn arco-assist:assembly-outputTimestamp-property -X
mvn arco-assist:mainclass-property -X
mvn arco-assist:generate-build-info -X
mvn arco-assist:timestamp-property -X

# 调试智能控制
mvn arco-assist:skip-plugin -X
```
