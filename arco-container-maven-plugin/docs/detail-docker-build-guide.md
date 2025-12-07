---
published: 2022.01.03
---

# Docker 构建指南

## 🚀 快速开始

这是一个交互式的 Docker 构建脚本，支持 Arco Container Maven Plugin 的两种构建模式和两种执行方式。

### 使用方法

```bash
# 在项目根目录执行
./docker-build.sh
```

## 📋 功能特性

### 🎯 构建模式选择

**1. 单层构建模式**

- 命令：`mvn package -Ddockerfile.skip=false`
- 适用：开发测试环境
- 特点：构建简单，适合快速验证

**2. 多层构建模式（推荐）**

- 命令：`mvn package -Pdocker`
- 适用：生产环境，频繁迭代
- 特点：充分利用 Docker 分层缓存

### 🎮 执行模式选择

- 执行 `docker build` 命令
- 生成 Docker 镜像

### 🔍 智能配置检测

脚本会自动检测项目配置：

- ✅ 检测 `src/docker/` 目录是否存在
- ✅ 自动选择对应的 Dockerfile
- ✅ 适配不同的构建上下文

## 🗂️ 配置优先级

### 自定义配置模式

当项目根目录存在 `docker/` 目录时：

```
project/
├── docker/
│   ├── Dockerfile-B        # 基础镜像
│   ├── Dockerfile-M        # 多层构建
│   └── Dockerfile-S        # 单层构建
├── src/
└── pom.xml
```

- **构建上下文**: maven 编译目录 (`target`)
- **Dockerfile 路径**: `docker/Dockerfile-M` 或 `docker/Dockerfile-S`

### 插件生成模式

当项目根目录不存在 `docker/` 目录时：

```
project/
├── target/
│   └── docker/
│       ├── Dockerfile-B
│       ├── Dockerfile-M
│       └── Dockerfile-S
├── src/
└── pom.xml
```

- **构建上下文**: target 目录 (`target`)
- **Dockerfile 路径**: `docker/Dockerfile-M` 或 `docker/Dockerfile-S`

## 🖥️ 使用演示

### 交互式界面

```
╔══════════════════════════════════════════════════════════════╗
║                                                              ║
║    🐳 Arco Container Maven Plugin - Docker Build Script    ║
║                                                              ║
║    交互式 Docker 构建和部署工具                               ║
║    支持单层/多层构建，构建/运行模式                           ║
║                                                              ║
╚══════════════════════════════════════════════════════════════╝

============================================================
🚀 环境检查
============================================================

📦 检查必要命令...
✅ 环境检查完成
ℹ️ 项目名称: my-spring-boot-app

============================================================
🚀 选择构建模式
============================================================

请选择构建模式：

  1) 单层构建模式 (简单快速)
     ・ 命令: mvn package -Ddockerfile.skip=false
     ・ 适用: 开发测试环境
     ・ 特点: 构建简单，适合快速验证

  2) 多层构建模式 (推荐)
     ・ 命令: mvn package -Pdocker
     ・ 适用: 生产环境，频繁迭代
     ・ 特点: 充分利用 Docker 分层缓存

请选择 [1-2]: 2
✅ 已选择: 多层构建模式
```

### 执行流程

1. **环境检查** - 验证 Maven、Docker、docker-compose
2. **模式选择** - 交互式选择构建和执行模式
3. **配置检测** - 自动检测 Docker 配置路径
4. **执行确认** - 显示执行计划，等待用户确认
5. **Maven 构建** - 执行相应的 Maven 命令
6. **Docker 操作** - 根据选择执行构建或运行
7. **完成提示** - 显示结果和后续操作建议

## 🔧 高级功能

### 错误处理

- ✅ 命令执行失败时自动停止
- ✅ 详细的错误提示和建议
- ✅ 支持 Ctrl+C 中断操作

### 日志输出

- ✅ 彩色输出，美观友好
- ✅ 分步骤显示进度
- ✅ 详细的命令执行日志

### 智能检测

- ✅ 自动获取项目名称
- ✅ 检测构建产物完整性
- ✅ 验证 Docker 配置有效性

## 📝 注意事项

1. **执行位置**: 必须在 Maven 项目根目录执行
2. **权限要求**: 需要 Docker 执行权限
3. **端口占用**: 确保相关端口未被占用
4. **网络环境**: 需要能够访问 Docker Hub

## 🆘 常见问题

### Q: 脚本执行失败怎么办？

A: 检查以下几点：

- 是否在正确的项目目录
- Maven、Docker 是否正确安装
- 网络连接是否正常

### Q: 如何查看容器运行状态？

A: 使用以下命令：

```bash
# 查看运行状态
docker-compose -f docker/docker-compose.yml ps

# 查看日志
docker-compose -f docker/docker-compose.yml logs -f

# 停止服务
docker-compose -f docker/docker-compose.yml down
```

### Q: 如何自定义 Docker 配置？

A: 参考主文档中的"自定义 Dockerfile"章节，将插件生成的配置拷贝到项目根目录的 `docker/` 目录进行修改。

---

🎉 **Happy Coding!** - 如有问题，请参考完整的 README.md 文档。
