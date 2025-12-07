---
published: 2022.04.26
---

# 自解压部署

## 简介

Arco Makeself Maven Plugin 是一个基于 [makeself](https://github.com/megastep/makeself) 的 Maven 插件，用于创建自解压的部署包。该插件可以将普通的
tar.gz 归档文件转换为自解压的 `.run` 文件，实现一键部署的便捷体验。

## 为什么使用这个插件

在原 `maven-assembly-plugin` 插件的基础之上，我们使用 `arco-makeself-maven-plugin` 来生成一个自解压的部署包，目的是方便一键部署。

**原来的部署步骤：**

1. 上传 tar.gz
2. 解压 tar.gz
3. 进入 xxx 目录
4. 执行启动脚本: `bin/launcher`

**现在的步骤：**

1. 上传 xxx.run 自解压部署包
2. 启动: `./xxx.run`

虽然跟以前比没少几个逻辑，使用者如果真的嫌麻烦，完全可以自己写一个从打包、上传到部署的一键启动脚本。

可能觉得这里再添加一个 `arco-makeself-maven-plugin` 完全是多余的，但是我不这样认为.

zeka.stack 框架的一个思想就是提供尽可能多的开箱即用的功能，尽量减少开发者非必要的操作，将主要精力集中在业务开发上，所以我更愿意多花些时间为开发者提供更多的选择。

一句话就是：**这个功能我可以不用，但是不能没有** 🙉

## 使用方式

### 快速开始

对于使用 zeka.stack 框架的项目，最简单的使用方式：

```bash
mvn clean package -Dmaven.test.skip=true -Dmakeself.skip=false
```

> **重要**：必须使用 `-Dmakeself.skip=false` 开启插件（框架默认是关闭的）

执行后会在 `target` 目录下生成一个 `xxx.run` 文件，直接执行即可：

```bash
./xxx.run
```

### 框架默认配置

框架已经内置了完整的插件配置，无需手动添加：

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-makeself-maven-plugin</artifactId>
    <version>${arco-maven-plugin.version}</version>
    <configuration>
        <!--suppress UnresolvedMavenProperty 和 maven-assembly-plugin 的 finalName 保持一致-->
        <archiveDir>${project.build.finalName}_${current.time}</archiveDir>
        <!--suppress UnresolvedMavenProperty 自定义自解压包的名称-->
        <fileName>${project.build.finalName}_${current.time}.run</fileName>

        <!-- 保持所有文件权限（跨平台兼容） -->
        <tarExtraOpt>-p</tarExtraOpt>
        <untarExtraOpt>-p</untarExtraOpt>
    </configuration>
    <executions>
        <execution>
            <id>makeself</id>
            <goals>
                <goal>makeself</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

**配置说明：**

- `archiveDir` 和 `fileName` 与 `maven-assembly-plugin` 的对应配置保持一致
- 配置中使用了 `${current.time}` 时间戳，确保每次构建的产物都有唯一标识，方便版本管理和回滚

### Maven 插件执行顺序

`maven-assembly-plugin` 和 `arco-makeself-maven-plugin` 都在 `package` 阶段执行，但 `arco-makeself-maven-plugin` 依赖 `maven-assembly-plugin`
的输出结果（xxx.tar.gz 部署包）。

Maven 会根据插件在 POM 中的定义顺序执行，因此框架将 `arco-makeself-maven-plugin` 定义在 `maven-assembly-plugin` 之后，确保正确的执行顺序：

```xml
<!-- 1. 先执行 assembly 打包 -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <!-- assembly 配置 -->
</plugin>

<!-- 2. 再执行 makeself 自解压包生成 -->
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-makeself-maven-plugin</artifactId>
    <!-- makeself 配置 -->
</plugin>
```

这样确保 makeself 插件能够找到 assembly 插件生成的 tar.gz 文件。

### 核心功能

- **自解压部署包**：将传统的 tar.gz 部署包转换为自解压的 `.run` 文件
- **一键部署**：上传到服务器后直接执行 `./app.run` 即可完成部署
- **环境配置**：支持指定不同的运行环境（prod、test、local 等）
- **智能解压**：第一次执行时自动解压，后续执行时跳过解压步骤
- **权限管理**：自动处理文件权限和用户切换
- **多压缩格式**：支持 gzip、bzip2、xz、lzo、lz4 等多种压缩算法

### 高级配置

如果需要自定义配置，可以在项目的 `pom.xml` 中覆盖默认配置：

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-makeself-maven-plugin</artifactId>
    <version>${arco-maven-plugin.version}</version>
    <configuration>
        <!--suppress UnresolvedMavenProperty -->
        <archiveDir>${project.build.finalName}</archiveDir>
        <fileName>${project.build.finalName}.run</fileName>
    </configuration>
    <executions>
        <execution>
            <id>makeself</id>
            <goals>
                <goal>makeself</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### 配置参数

#### 必需参数

| 参数           | 说明       | 示例             |
|--------------|----------|----------------|
| `archiveDir` | 存档文件目录名称 | `my-app-1.0.0` |

#### 可选参数

| 参数                     | 默认值                             | 说明              |
|------------------------|---------------------------------|-----------------|
| `fileName`             | `${archiveDir}.run`             | 生成的自解压文件名       |
| `label`                | `Make self-extrabable archives` | 解压时显示的描述信息      |
| `startupScript`        | `./runner.sh`                   | 解压后执行的启动脚本      |
| `gzip`                 | `true`                          | 使用 gzip 压缩      |
| `notemp`               | `true`                          | 在当前目录创建解压目录     |
| `nooverwrite`          | `true`                          | 避免重复解压覆盖数据      |
| `nomd5`                | `false`                         | 禁用 MD5 校验       |
| `nocrc`                | `false`                         | 禁用 CRC 校验       |
| `sha256`               | `false`                         | 启用 SHA256 校验    |
| `deleteTempFiles`      | `true`                          | 构建完成后删除临时文件     |
| `autoRun`              | `false`                         | 构建完成后自动运行（测试用）  |
| `forceInvokeOnWindows` | `false`                         | 在 Windows 上强制执行 |
| `tarExtraOpt`          | -                               | tar 打包时的额外选项    |
| `untarExtraOpt`        | -                               | tar 解压时的额外选项    |

#### 压缩选项

```xml
<configuration>
    <!-- 选择压缩算法（只能选择一个） -->
    <gzip>true</gzip>           <!-- 默认 -->
    <bzip2>false</bzip2>        <!-- 更好的压缩率 -->
    <pbzip2>false</pbzip2>      <!-- 并行 bzip2 -->
    <xz>false</xz>              <!-- 最佳压缩率 -->
    <lzo>false</lzo>            <!-- 快速压缩 -->
    <lz4>false</lz4>            <!-- 超快压缩 -->
    <pigz>false</pigz>          <!-- 并行 gzip -->

    <!-- 压缩级别（1-9） -->
    <complevel>9</complevel>
</configuration>
```

#### 安全选项

```xml
<configuration>
    <!-- SSL 加密 -->
    <sslEncrypt>true</sslEncrypt>
    <sslPasswd>your-password</sslPasswd>

    <!-- GPG 加密 -->
    <gpgEncrypt>true</gpgEncrypt>
    <gpgAsymmetricEncryptSign>true</gpgAsymmetricEncryptSign>

    <!-- Base64 编码 -->
    <base64>true</base64>
</configuration>
```

#### 权限保持选项

**解决启动脚本权限丢失问题:**

```xml
<configuration>
    <!-- 推荐方案：保持所有文件权限（跨平台兼容） -->
    <tarExtraOpt>-p</tarExtraOpt>
    <untarExtraOpt>-p</untarExtraOpt>
</configuration>
```

**平台兼容性说明:**

- **Linux/Unix (GNU tar)**：支持更多高级选项
  ```xml
  <configuration>
      <tarExtraOpt>--preserve-permissions</tarExtraOpt>
      <untarExtraOpt>--preserve-permissions</untarExtraOpt>
  </configuration>
  ```

- **macOS (BSD tar)**：使用简化选项
  ```xml
  <configuration>
      <!-- 保持权限（短选项，兼容性好） -->
      <tarExtraOpt>-p</tarExtraOpt>
      <untarExtraOpt>-p</untarExtraOpt>
  </configuration>
  ```

### 完整配置示例

```xml
<plugin>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-makeself-maven-plugin</artifactId>
    <version>2.0.0-SNAPSHOT</version>
    <configuration>
        <archiveDir>my-spring-boot-app-1.0.0</archiveDir>
        <fileName>my-spring-boot-app-1.0.0.run</fileName>
        <label>My Spring Boot Application v1.0.0</label>
        <startupScript>./runner.sh</startupScript>
        <scriptArgs>
            <scriptArg>--spring.profiles.active=prod</scriptArg>
        </scriptArgs>

        <!-- 压缩配置 -->
        <gzip>true</gzip>
        <complevel>9</complevel>

        <!-- 校验配置 -->
        <sha256>true</sha256>
        <nomd5>false</nomd5>

        <!-- 权限保持配置（跨平台兼容） -->
        <tarExtraOpt>-p</tarExtraOpt>
        <untarExtraOpt>-p</untarExtraOpt>

        <!-- 其他选项 -->
        <notemp>true</notemp>
        <nooverwrite>true</nooverwrite>
        <deleteTempFiles>true</deleteTempFiles>
    </configuration>
    <executions>
        <execution>
            <phase>package</phase>
            <goals>
                <goal>makeself</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

> 其他详细的配置可以看 [makeself](https://github.com/megastep/makeself), 这个插件实际上是借鉴了此项目, 然后做了部分改造而成, 感谢 makeself
> 项目维护者.

### 部署使用

1. **构建部署包**：
   ```bash
   mvn clean package
   ```

2. **上传到服务器**：
   ```bash
   scp target/my-app.run user@server:/opt/apps/
   ```

3. **执行部署**：
   ```bash
   # 默认 prod 环境
   ./my-app.run

   # 指定环境
   ./my-app.run test
   ./my-app.run local
   ```

4. **查看部署包信息**：
   ```bash
   ./my-app.run --info
   ./my-app.run --list
   ./my-app.run --help
   ```

### 环境区分

插件支持不同环境的部署配置：

- **生产环境（prod）**：使用 `zekastack` 用户执行，适合服务器环境
- **本地环境（local）**：使用当前用户执行，适合开发测试
- **自定义环境**：通过参数传递环境标识

可通过系统属性控制：

```bash
mvn package -Dpackage.type=local
```

## 实现原理

### makeself 工作机制

`makeself` 是一个 Shell 脚本工具，它将 tar 归档文件与一个解压和执行脚本组合成单个可执行文件。生成的 `.run` 文件的结构如下：

```
xxx.run
├── Shell 脚本头部（makeself-header.sh）
├── 解压和执行逻辑
└── tar.gz 数据（二进制附加在脚本末尾）
```

**执行流程：**

1. 执行 `./xxx.run` 时，Shell 读取脚本头部
2. 脚本自动检测并创建临时目录
3. 从文件末尾提取 tar.gz 数据并解压
4. 执行预设的启动脚本
5. 可选择是否保留解压后的文件

### 核心脚本说明

#### 1. makeself.sh

**作用**：核心打包脚本，负责生成自解压文件

- 将目录打包为 tar 格式
- 应用指定的压缩算法
- 生成校验和（MD5、CRC、SHA256）
- 将脚本头部与 tar 数据合并

#### 2. makeself-header.sh

**作用**：自解压文件的 Shell 脚本模板

- 包含解压逻辑和参数解析
- 提供 `--help`、`--info`、`--list` 等选项
- 处理文件权限和用户切换
- 支持加密和完整性验证

#### 3. runner.sh / runner-local.sh

**作用**：应用启动脚本，决定如何启动服务

**runner.sh（生产环境）：**

```bash
#!/bin/bash
chown -R zekastack:zekastack ./
if [[ ! "$1" ]] ;then
    su zekastack -c "bin/launcher -r prod -t"
else
    su zekastack -c "bin/launcher -r $1 -t"
fi
```

**runner-local.sh（本地环境）：**

```bash
#!/bin/bash
# 本地测试使用，因为没有 zekastack 用户
if [[ ! "$1" ]] ;then
    bin/launcher -T -r local &
else
    bin/launcher -T -r $1 &
fi
```

### 自定义启动脚本

如果业务项目需要自定义启动逻辑，可以在项目的 `bin/` 目录下放置自己的 `runner.sh`：

```
project-root/
├── src/
├── bin/
│   └── runner.sh      # 自定义启动脚本
└── pom.xml
```

插件会优先使用项目中的自定义脚本，如果不存在则使用框架提供的默认脚本。

### 使用细节

#### 1. 重复执行处理

- `xxx.run` 执行一次后会自动解压到同名目录
- 如果存在同名目录，脚本会跳过解压步骤直接启动
- 如需重新解压，重命名或删除已有目录即可

#### 2. 时间戳后缀

- 框架默认为 `.run` 文件添加时间戳后缀（如 `app_20241201_1430.run`）
- 便于版本管理和服务备份
- 支持同时部署多个版本进行灰度发布

#### 3. 环境参数传递

```bash
# 默认生产环境
./app.run

# 指定测试环境
./app.run test

# 本地开发环境
./app.run local
```

### 执行脚本权限问题的处理

在使用自解压部署包时，经常会遇到启动脚本权限丢失的问题，导致解压后的 `launcher` 脚本无法执行。框架通过以下方式解决这个问题：

#### 问题根因

权限丢失可能发生在两个阶段：

1. **Java 解压阶段**：`CompressUtils.decompressTarGz()` 方法在解压 tar.gz 时可能丢失文件权限
2. **makeself 重新打包阶段**：makeself 在重新打包时没有保持原始文件权限

#### 解决方案

##### 1. Java 解压阶段权限保持

框架修改了 `CompressUtils` 工具类，在解压时智能处理文件权限：

```java
// 保持 tar 包中的原始权限
if (entry.getMode() != 0) {
    int mode = entry.getMode();
    if ((mode & 73) != 0) { // 检查执行权限位
        // 设置执行权限
    }
}
// 后备方案：对已知的可执行文件强制设置权限
if (isExecutableFile(file)) {
    setExecutablePermissions(file);
}
```

**特别处理的文件类型：**

- `*.sh` - shell 脚本
- `launcher` - 启动脚本
- `docker-build` - Docker 构建脚本

##### 2. makeself 打包阶段权限保持

通过配置 tar 选项保持文件权限：

```xml
<configuration>
    <!-- 跨平台兼容的权限保持配置 -->
    <tarExtraOpt>-p</tarExtraOpt>
    <untarExtraOpt>-p</untarExtraOpt>
</configuration>
```

**平台兼容性：**

- **Linux/Unix (GNU tar)**：支持`-p` 和 `--preserve-permissions` 等长选项
- **macOS (BSD tar)**：只支持 `-p` 等短选项，避免使用 `--mode` 等扩展选项

#### 三重保障机制

1. **第一层：原始权限检查** - 优先使用 tar 包中的原始权限信息
2. **第二层：文件名识别** - 对已知的可执行文件类型强制设置权限
3. **第三层：makeself 保持** - 通过 tar 选项确保打包和解压时保持权限

这样无论在什么环境下，启动脚本都能获得正确的执行权限。

## 技术实现细节

### 工作流程

1. **预处理阶段**：
    - 检查模块类型（仅对 DEPLOY 类型模块生效）
    - 验证操作系统兼容性（Windows 需要 bash 环境）
    - 解压现有的 tar.gz 部署包

2. **脚本准备**：
    - 从插件 JAR 中提取 makeself 相关脚本
    - 复制 makeself.sh 和 makeself-header.sh 到临时目录
    - 根据环境配置选择合适的 launcher 脚本

3. **自解压包生成**：
    - 调用 makeself.sh 脚本
    - 将解压后的应用目录打包为自解压格式
    - 嵌入启动脚本和配置参数

4. **后处理**：
    - 验证生成的自解压包
    - 附加到 Maven 构建生命周期
    - 清理临时文件

### 核心组件

#### MakeselfMojo 类

主要的 Mojo 实现类，负责：

- 参数解析和验证
- 调用 makeself 脚本
- 处理不同操作系统的兼容性
- 管理临时文件和清理工作

#### Makeself 脚本

基于开源的 makeself 2.4.2 版本：

- `makeself.sh`：主要的打包脚本
- `makeself-header.sh`：自解压包的头部模板

#### 启动脚本

- `launcher.sh`：生产环境启动脚本（切换到 zekastack 用户）
- `launcher-local.sh`：本地环境启动脚本（使用当前用户）

#### 部署脚本

- `install.sh`：自解压逻辑实现
- `addpayload.sh`：附加载荷数据的工具脚本

### 文件结构

```
arco-makeself-maven-plugin/
├── src/main/java/
│   └── dev/dong4j/zeka/maven/plugin/makeself/mojo/
│       └── MakeselfMojo.java              # 主要 Mojo 实现
├── src/main/resources/META-INF/
│   ├── makeself/                          # makeself 脚本
│   │   ├── makeself.sh
│   │   └── makeself-header.sh
│   └── launcher/                          # 启动脚本
│       ├── launcher.sh                    # 生产环境
│       └── launcher-local.sh              # 本地环境
├── bin/                                   # 额外的辅助脚本
│   ├── binary/
│   │   ├── addpayload.sh
│   │   └── install.sh
│   └── uuencode/
│       ├── addpayload.sh
│       └── install.sh
└── assembly/
    └── assembly.xml                       # 插件打包配置
```

### 关键特性

#### 增量部署

- 首次执行时解压应用包
- 后续执行时检测目录是否存在，避免重复解压
- 支持热更新和版本升级

#### 权限管理

- 自动设置脚本执行权限
- 支持用户切换（生产环境使用 zekastack 用户）
- POSIX 文件权限处理

#### 跨平台支持

- Linux/Unix 原生支持
- Windows 通过 Cygwin 或 Git Bash 支持
- 智能检测 bash 环境可用性

#### 完整性校验

- 支持 MD5、CRC、SHA256 校验
- 确保部署包在传输过程中的完整性
- 可配置校验算法组合

### 与其他插件的集成

该插件通常与以下插件配合使用：

1. **Maven Assembly Plugin**：生成初始的 tar.gz 部署包
2. **Spring Boot Maven Plugin**：创建可执行的 Spring Boot 应用
3. **Maven Deploy Plugin**：将生成的 .run 文件部署到仓库

### 故障排除

#### 常见问题

1. **Windows 环境下 bash 未找到**：
    - 安装 Git for Windows 或 Cygwin
    - 将 bash 路径添加到系统 PATH 环境变量

2. **权限问题**：
    - 确保生成的 .run 文件具有执行权限
    - 检查目标目录的写入权限

3. **启动脚本权限丢失问题**：
    - **问题描述**：原始 tar.gz 中的 launcher 脚本有执行权限，但经过 makeself 重新打包后权限丢失
    - **根本原因**：问题可能出现在两个地方：
        1. **Java 解压阶段**：`CompressUtils.decompressTarGz()` 方法只对 `.sh` 文件设置权限，而 `launcher` 脚本没有 `.sh` 后缀
        2. **makeself 打包阶段**：makeself 重新打包时没有保持原始文件权限
    - **解决方案**：双重保障确保权限正确

   **跨平台兼容方案（推荐）：**
    ```xml
    <configuration>
        <!-- 使用短选项，兼容 BSD tar 和 GNU tar -->
        <tarExtraOpt>-p</tarExtraOpt>
        <untarExtraOpt>-p</untarExtraOpt>
    </configuration>
    ```

   **仅 Linux 环境（GNU tar）：**
    ```xml
    <configuration>
        <tarExtraOpt>--preserve-permissions</tarExtraOpt>
        <untarExtraOpt>--preserve-permissions</untarExtraOpt>
    </configuration>
    ```

   **注意**：macOS 的 BSD tar 不支持 `--mode` 等 GNU tar 扩展选项，会报错 `Option --mode='a+x' is not supported`

4. **解压失败**：
    - 检查磁盘空间是否充足
    - 验证压缩包的完整性

5. **用户切换失败**：
    - 确保 zekastack 用户存在
    - 检查 sudo 权限配置

#### 调试选项

```xml
<configuration>
    <autoRun>true</autoRun>          <!-- 自动测试运行 -->
    <deleteTempFiles>false</deleteTempFiles>  <!-- 保留临时文件用于调试 -->
</configuration>
```

启用 Maven 调试模式：

```bash
mvn package -X
```

## 参考资源

- [Makeself 官方网站](https://makeself.io/)
- [Makeself GitHub 仓库](https://github.com/megastep/makeself)
- [Maven Plugin 开发指南](https://maven.apache.org/guides/plugin/guide-java-plugin-development.html)

