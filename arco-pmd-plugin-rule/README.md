---
published: 2022.03.22
---

# 代码质量检查

## 概述

`arco-pmd-plugin-rule` 是一个为 Maven PMD Plugin 提供自定义检查规则的插件。该插件基于阿里巴巴 P3C 规范，专注于代码质量检查，与 Checkstyle
插件形成互补，共同确保代码的高质量标准。

**重要说明**: 在 Zeka Stack 框架中，PMD 插件默认开启，如果检查到不符合质量要求的代码，会停止编译。如需紧急修复 bug，可临时关闭代码检查。

## Checkstyle vs PMD 的区别

### Checkstyle - 代码样式检查

- **检查范围**: 代码格式、命名规范、注释规范、导入语句等
- **检查时机**: 基于源代码的静态分析
- **主要功能**:
    - 代码缩进、空格、换行等格式问题
    - 类名、方法名、变量名等命名规范
    - Javadoc 注释的完整性和格式
    - 导入语句的顺序和规范
    - 代码长度和复杂度限制

### PMD - 代码质量检查

- **检查范围**: 代码逻辑、设计模式、最佳实践、潜在问题等
- **检查时机**: 基于抽象语法树（AST）的深度分析
- **主要功能**:
    - 代码逻辑错误和潜在 bug
    - 设计模式和最佳实践违规
    - 性能问题和内存泄漏风险
    - 线程安全和并发问题
    - 代码重复和复杂度问题

### 为什么同时使用两个插件？

1. **互补性**: Checkstyle 关注"怎么写"，PMD 关注"写什么"
2. **全面性**: 覆盖代码检查的各个方面，确保代码质量
3. **专业性**: 每个插件都有其专业领域，组合使用效果更佳
4. **实用性**: 在开发过程中及早发现和解决问题

## 使用方式

### 1. 在 Zeka Stack 项目中使用

由于插件已内置到框架中，无需额外配置，直接运行 Maven 命令即可：

```bash
# 运行代码质量检查
mvn pmd:check

# 生成 PMD 报告
mvn pmd:pmd

# 在验证阶段自动运行
mvn validate

# 临时关闭代码检查（紧急修复时使用）
mvn clean compile -Dpmd.skip=true
```

### 2. 在独立项目中使用

如果需要在非 Zeka Stack 项目中使用，可以添加依赖：

```xml
<dependency>
    <groupId>dev.dong4j</groupId>
    <artifactId>arco-pmd-plugin-rule</artifactId>
    <version>${arco-maven-plugin.version}</version>
</dependency>
```

## 功能特性

### 🎯 基于阿里巴巴 P3C 规范

- **权威性**: 基于阿里巴巴 Java 开发手册
- **实用性**: 经过大规模项目验证的最佳实践
- **全面性**: 覆盖代码质量的各个方面

### 🔧 多维度检查规则

插件提供了以下规则集：

#### 1. 注释规范 (zeka-stack-comment.xml)

- **Javadoc 格式检查**: 确保注释符合标准格式
- **抽象方法注释**: 要求抽象方法和接口方法必须有注释
- **类作者信息**: 要求类必须包含作者信息
- **枚举注释**: 要求枚举常量必须有注释
- **注释位置**: 避免在语句后添加注释

#### 2. 并发编程 (zeka-stack-concurrent.xml)

- **线程池创建**: 推荐使用显式创建线程池，避免使用默认线程池
- **避免使用 Timer**: 推荐使用 ScheduledExecutorService
- **线程命名**: 要求线程必须设置名称
- **ThreadLocal 清理**: 要求正确清理 ThreadLocal
- **锁的正确使用**: 确保锁在 finally 块中释放

#### 3. 常量定义 (zeka-stack-constant.xml)

- **Long 类型常量**: 要求使用大写 L 后缀
- **魔法数字**: 禁止使用未定义的魔法数字

#### 4. 异常处理 (zeka-stack-exception.xml)

- **包装类型返回**: 避免方法返回包装类型
- **finally 块返回**: 避免在 finally 块中使用 return
- **事务回滚**: 要求事务方法必须配置回滚

#### 5. 流程控制 (zeka-stack-flowcontrol.xml)

- **Switch 语句**: 要求 switch 语句必须有 default 分支
- **大括号使用**: 要求 if/for/while 等语句必须使用大括号
- **复杂条件**: 避免过于复杂的条件表达式
- **否定操作符**: 避免使用否定操作符

#### 6. 命名规范 (zeka-stack-naming.xml)

- **类命名**: 要求使用驼峰命名法
- **异常类命名**: 要求异常类以 Exception 结尾
- **测试类命名**: 要求测试类以 Test 结尾
- **变量命名**: 要求使用小驼峰命名法
- **常量命名**: 要求常量使用大写字母和下划线

#### 7. 面向对象 (zeka-stack-oop.xml)

- **equals 方法**: 避免在 equals 方法中使用 null
- **包装类型比较**: 使用 equals 方法比较包装类型
- **POJO 字段**: 推荐使用基本类型字段
- **toString 方法**: 要求 POJO 必须重写 toString 方法
- **字符串拼接**: 推荐使用 StringBuilder 进行字符串拼接
- **BigDecimal 构造**: 避免使用 double 构造 BigDecimal

#### 8. ORM 规范 (zeka-stack-orm.xml)

- **iBatis 方法**: 检查 iBatis 相关方法的正确使用

#### 9. 其他规范 (zeka-stack-other.xml)

- **正则表达式**: 避免在方法中编译正则表达式
- **BeanUtils 使用**: 避免使用 Apache BeanUtils
- **时间获取**: 推荐使用 System.currentTimeMillis()
- **随机数生成**: 避免错误使用 Math.random()
- **方法长度**: 限制方法长度
- **日期格式**: 使用正确的日期格式大小写
- **浮点数比较**: 避免直接比较浮点数

#### 10. 集合操作 (zeka-stack-set.xml)

- **数组转换**: 避免 ClassCastException
- **Arrays.asList**: 避免修改 Arrays.asList 返回的列表
- **SubList 转换**: 避免将 SubList 转换为 ArrayList
- **并发修改**: 避免在遍历时修改集合
- **集合初始化**: 推荐指定集合初始容量

## 框架集成配置

### Zeka Stack 框架内置配置

为了简化使用者的配置工作，本插件已内置到 Zeka Stack 框架中，提供开箱即用的代码质量检查功能。

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>${maven-pmd-plugin.version}</version>
    <configuration>
        <includes>dev/dong4j/**/*.java</includes>
        <excludes>dev/dong4j/kernel/generator/**/*.java</excludes>
        <linkXRef>false</linkXRef>
        <printFailingErrors>false</printFailingErrors>
        <rulesets>
            <ruleset>rulesets/zeka-stack-comment.xml</ruleset>
            <ruleset>rulesets/zeka-stack-concurrent.xml</ruleset>
            <ruleset>rulesets/zeka-stack-constant.xml</ruleset>
            <ruleset>rulesets/zeka-stack-exception.xml</ruleset>
            <ruleset>rulesets/zeka-stack-flowcontrol.xml</ruleset>
            <ruleset>rulesets/zeka-stack-naming.xml</ruleset>
            <ruleset>rulesets/zeka-stack-oop.xml</ruleset>
            <ruleset>rulesets/zeka-stack-orm.xml</ruleset>
            <ruleset>rulesets/zeka-stack-other.xml</ruleset>
            <ruleset>rulesets/zeka-stack-set.xml</ruleset>
        </rulesets>
        <analysisCache>true</analysisCache>
        <analysisCacheLocation>${project.build.directory}/pmd/pmd.cache</analysisCacheLocation>
        <skipEmptyReport>true</skipEmptyReport>
        <targetJdk>${java.version}</targetJdk>
        <verbose>true</verbose>
    </configuration>
    <executions>
        <execution>
            <id>validate</id>
            <phase>validate</phase>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
        <execution>
            <id>pmd-pmd-site</id>
            <phase>site</phase>
            <goals>
                <goal>pmd</goal>
            </goals>
        </execution>
    </executions>
    <dependencies>
        <dependency>
            <groupId>io.github.godfather1103.p3c</groupId>
            <artifactId>p3c-pmd</artifactId>
            <version>${p3c-pmd.version}</version>
        </dependency>
        <dependency>
            <groupId>dev.dong4j</groupId>
            <artifactId>arco-pmd-plugin-rule</artifactId>
            <version>${arco-maven-plugin.version}</version>
        </dependency>
    </dependencies>
</plugin>
```

### 配置说明

| 配置项                  | 说明       | 默认值                                     |
|----------------------|----------|-----------------------------------------|
| `includes`           | 检查的文件模式  | `dev/dong4j/**/*.java`                  |
| `excludes`           | 排除的文件模式  | `dev/dong4j/kernel/generator/**/*.java` |
| `rulesets`           | 使用的规则集   | 10 个 Zeka Stack 规则集                     |
| `analysisCache`      | 是否启用增量分析 | `true`                                  |
| `printFailingErrors` | 是否打印失败错误 | `true`                                  |
| `skipEmptyReport`    | 是否跳过空报告  | `true`                                  |

### 标准 Maven PMD Plugin 配置对比

在介绍本插件的简化配置之前，先了解标准的 Maven PMD Plugin 配置方式：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.19.0</version>
    <configuration>
        <rulesets>
            <ruleset>rulesets/java/basic.xml</ruleset>
            <ruleset>rulesets/java/braces.xml</ruleset>
            <ruleset>rulesets/java/comments.xml</ruleset>
            <ruleset>rulesets/java/design.xml</ruleset>
            <ruleset>rulesets/java/empty.xml</ruleset>
            <ruleset>rulesets/java/imports.xml</ruleset>
            <ruleset>rulesets/java/naming.xml</ruleset>
            <ruleset>rulesets/java/strings.xml</ruleset>
            <ruleset>rulesets/java/unnecessary.xml</ruleset>
            <ruleset>rulesets/java/unusedcode.xml</ruleset>
        </rulesets>
        <targetJdk>11</targetJdk>
        <printFailingErrors>false</printFailingErrors>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>check</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 典型问题示例

### 1. 线程池使用问题

```java
// ❌ 错误示例 - 使用默认线程池
ExecutorService executor = Executors.newFixedThreadPool(10);

// ✅ 正确示例 - 显式创建线程池
ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
    .setNameFormat("demo-pool-%d").build();
ExecutorService executor = new ThreadPoolExecutor(5, 200,
    0L, TimeUnit.MILLISECONDS,
    new LinkedBlockingQueue<>(1024), namedThreadFactory,
    new ThreadPoolExecutor.AbortPolicy());
```

### 2. 字符串拼接问题

```java
// ❌ 错误示例 - 使用 + 拼接
String result = "";
for (String item : list) {
    result = result + item;
}

// ✅ 正确示例 - 使用 StringBuilder
StringBuilder sb = new StringBuilder();
for (String item : list) {
    sb.append(item);
}
String result = sb.toString();
```

### 3. 浮点数比较问题

```java
// ❌ 错误示例 - 直接比较
if (a == b) {
    // 可能不准确
}

// ✅ 正确示例 - 使用误差范围
double epsilon = 1e-6;
if (Math.abs(a - b) < epsilon) {
    // 比较准确
}
```

### 4. 集合操作问题

```java
// ❌ 错误示例 - 修改 Arrays.asList 返回的列表
List<String> list = Arrays.asList("a", "b", "c");
list.add("d"); // 会抛出 UnsupportedOperationException

// ✅ 正确示例 - 创建新的 ArrayList
List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
list.add("d"); // 正常工作
```

## 检查报告

### 控制台输出

检查结果会在控制台显示，包括：

- 违规文件路径和行号
- 违规类型和描述
- 建议的修复方案
- 构建失败原因

### HTML 报告

详细的检查报告会生成到：

```
${project.build.directory}/site/pmd.html
```

### XML 报告

机器可读的报告会生成到：

```
${project.build.directory}/pmd.xml
```

### 缓存文件

为了提高检查效率，会生成缓存文件：

```
${project.build.directory}/pmd/pmd.cache
```

## 最佳实践

### 1. 代码质量策略

- 在项目初期就配置 PMD 插件
- 定期检查和更新规则集
- 建立代码质量审查流程

### 2. 规则定制

- 根据项目需求调整规则优先级
- 使用排除规则忽略特定情况
- 定期更新 P3C 规则版本

### 3. 团队协作

- 统一团队的代码质量标准
- 在 CI/CD 流程中集成 PMD 检查
- 建立代码质量培训机制

## 故障排除

### 常见问题

1. **PMD 检查失败但代码看起来正确**
    - 检查是否有排除规则配置
    - 确认规则集版本是否匹配
    - 查看详细的错误信息

2. **检查速度慢**
    - 启用缓存功能
    - 减少检查的文件范围
    - 排除不必要的目录

3. **规则不生效**
    - 确认规则集路径正确
    - 检查依赖是否正确引入
    - 查看 Maven 日志

### 调试技巧

1. **启用详细日志**
   ```bash
   mvn pmd:check -X
   ```

2. **单独运行规则集**
   ```bash
   mvn pmd:check -Drulesets=rulesets/zeka-stack-concurrent.xml
   ```

3. **查看缓存状态**
   ```bash
   mvn pmd:check -DanalysisCache=true
   ```
