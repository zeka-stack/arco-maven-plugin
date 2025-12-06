# **可重现构建说明文档**

## **1. 背景与问题**

在传统构建过程中，打包产物（如 JAR、ZIP、TAR）会包含以下可能导致差异的因素：

- 文件的 **时间戳（last modified time）**
- 构建时环境变量（如操作系统、用户目录）
- 工具链细微差异（例如不同 Maven 插件版本）

这意味着即使源代码完全相同，在不同时间、不同机器上执行 mvn package，得到的产物二进制结果可能不同，难以保证 **一致性与可追溯性**。

## **2. 可重现构建（Reproducible Build）**

**定义**：

> 给定相同的源代码、依赖和构建配置，在不同的时间和环境下构建，应该得到字节级别完全一致的产物。

### **2.1 为什么需要可重现构建？**

- **安全性**：防止供应链攻击，确保二进制产物与源代码一致。
- **合规性**：开源项目越来越强调 reproducible build（如 Debian、Apache）。
- **一致性**：避免因为时间戳或环境差异导致的 CI/CD 构建产物不一致。
- **可追溯性**：同一版本的产物在任何地方重构都完全一致，方便问题排查和验证。

## **3. Maven 中的 outputTimestamp**

在 maven-assembly-plugin、maven-jar-plugin 等插件中，都支持配置：

```xml
<outputTimestamp>...</outputTimestamp>
```

它的作用是：

- 覆盖构建产物内所有文件的 lastModifiedTime
- 确保归档文件（如 JAR、TAR）的时间戳一致
- 从而消除因构建时间不同导致的产物差异

### **3.1 常见配置方式**

1. **固定值**（推荐方式，用于可重现构建）

    ```
    <outputTimestamp>2025-01-01T00:00:00Z</outputTimestamp>
    ```

   > 同一版本永远产出一致的文件。

2. **使用 Git 提交时间**

    ```
    <outputTimestamp>${git.commit.time}</outputTimestamp>
    ```

   > 确保同一 commit 构建产物一致。

3. **与版本号绑定**

    ```
    <outputTimestamp>${outputTimestamp.${project.version}}</outputTimestamp>
    ```

   > 保证同一版本号构建产物一致，不同版本可以有不同时间戳。

```xml
<properties>
    <!-- 默认固定时间戳（保证可重现构建） -->
    <project.build.outputTimestamp>2025-01-01T00:00:00Z</project.build.outputTimestamp>
</properties>

<build>
    <pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>${maven-assembly-plugin.version}</version>
                <configuration>
                    ...
                    <!--构建结果可重现的配置, 每个版本应该保持一致 -->
                    <!-- 优先级：
                         1) 用户显式传入 -Dproject.build.outputTimestamp=xxxx
                         2) SOURCE_DATE_EPOCH 环境变量（Maven 原生支持）
                         3) Git commit 时间（git-commit-id-plugin 注入的属性）
                         4) 默认固定时间（2025-01-01T00:00:00Z） -->
                    <outputTimestamp>${project.build.outputTimestamp}</outputTimestamp>
                </configuration>
                ...
            </plugin>
        </plugins>
    </pluginManagement>
</build>
```

1. **默认情况**（什么都不配）：
    - `outputTimestamp = 1980-01-01T00:00:00Z`
    - 产物稳定、可重现。
2. **跟随 Git commit 时间**：
    - `mvn clean package -Puse-git-timestamp`
    - 会自动把 outputTimestamp 设置为 git.commit.time。
3. **遵循开源标准**（CI/CD 推荐）：

    ```shell
    export SOURCE_DATE_EPOCH=$(git log -1 --pretty=%ct)
    mvn clean package
    ```

   Maven 会自动读取 SOURCE_DATE_EPOCH，覆盖 outputTimestamp。

4. **用户手动指定**：

    ```shell
    mvn clean package -Dproject.build.outputTimestamp=2025-08-30T12:00:00Z
    ```

---

## **4. 与框架版本绑定的方案**

为了简化维护，本框架将 outputTimestamp 与框架版本号关联：

### **4.1 配置示例**

```xml
<properties>
  <!-- 每个版本固定一个 outputTimestamp -->
  <outputTimestamp.1.0.0>2025-08-01T00:00:00Z</outputTimestamp.1.0.0>
  <outputTimestamp.1.1.0>2025-08-15T00:00:00Z</outputTimestamp.1.1.0>
  <outputTimestamp.1.2.0>2025-08-30T00:00:00Z</outputTimestamp.1.2.0>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>org.apache.maven.plugins</groupId>
      <artifactId>maven-assembly-plugin</artifactId>
      <version>${maven-assembly-plugin.version}</version>
      <configuration>
        <outputTimestamp>${outputTimestamp.project.version}</outputTimestamp>
      </configuration>
    </plugin>
  </plugins>
</build>
```

### **4.2 原理**

- **同一版本号 → 固定的 outputTimestamp → 构建产物完全一致**
- **不同版本号 → 不同的 outputTimestamp → 产物可区分，但仍然在同版本内可重现**

因为这个插件是在框架层配置的, 所以按照 "约定大于配置" 的思路, 使用者只需要在项目配置中添加 `properties` 即可, 比如现在发布 2.0.0 的新版本:

```properties
<properties>
  <!-- 每个版本固定一个 outputTimestamp -->
  <outputTimestamp.1.0.0>2025-08-01T00:00:00Z</outputTimestamp.1.0.0>
  <outputTimestamp.1.1.0>2025-08-15T00:00:00Z</outputTimestamp.1.1.0>
  <outputTimestamp.1.2.0>2025-08-30T00:00:00Z</outputTimestamp.1.2.0>

  <outputTimestamp.2.0.0>2025-09-01T00:00:00Z</outputTimestamp.2.0.0>
</properties>
```

为了能够正确读取到 `outputTimestamp.project.version` 这个配置, 我们使用 mojo 来自动注入:

```java
@Mojo(name = "assembly-outputTimestamp-property", defaultPhase = LifecyclePhase.INITIALIZE)
public class AssemblyOutputTimestampMojo extends AbstractMojo {

    /** 注入 MavenProject 对象，获取版本号等信息 */
    @SuppressWarnings("deprecation")
    @Component
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException {
        String version = project.getVersion();
        // 属性名固定为 outputTimestamp.project.version
        String propertyName = "outputTimestamp.project.version";
        // 属性值为 outputTimestamp.<project.version>
        String propertyKey = "outputTimestamp." + version;

        final String propertyValue = project.getProperties().getProperty(propertyKey);
        if (StringUtils.isBlank(propertyValue)) {
            // 如果属性值不存在，则使用默认时间戳
            getLog().error("[" + propertyKey + "] 未配置, 请添加对应的配置, 确保 value 格式正确");
        }
        // 注入到 MavenProject properties
        this.project.getProperties().put(propertyName, propertyValue);
        getLog().info("Injected property: " + propertyName + "=" + propertyValue);
    }
}
```

**约定:**

1. 在升级框架版本时, 在  `arco-supreme` 的 pom.xml 中添加配置 `outputTimestamp` 配置, 格式为 `outputTimestamp.{project.version}`;
2. 业务项目在父项目中添加配置 `outputTimestamp` 配置, 会覆盖掉框架的内置配置;
3. `outputTimestamp` 的 value 格式支持多种:
    1. ISO 8601 UTC 时间: `yyyy-MM-dd'T'HH:mm:ss'Z'`
    2. Unix epoch 秒数, 如将 `git log -1 --pretty=%ct` 的输出作为 value;

---

### **4.3 版本发布时的操作规范**

1. 在准备发布新版本时（例如 1.3.0），在 pom.xml 中新增：

```
<outputTimestamp.1.3.0>2025-09-01T00:00:00Z</outputTimestamp.1.3.0>
```

1. 发布后不得修改该版本的 outputTimestamp，确保历史版本可重现。
2. 新版本发布时再增加新的时间戳。

## **5. 总结**

- outputTimestamp 用于消除构建产物的时间戳差异，保证可重现性。

- 本框架采用 **版本号绑定固定时间戳** 的方案：

    - 同一版本，任何时间、任何环境构建结果一致；
    - 不同版本允许有不同时间戳。


- 该方案兼顾了 **可重现构建** 与 **版本区分** 的需求。
