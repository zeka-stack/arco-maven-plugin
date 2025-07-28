# Zeka.Stack Boot 通用启动脚本

---

## 简介

本脚本用于统一管理 Java 应用的启动、停止、重启、状态查看等操作，支持多环境、日志管理、JVM 参数、APM、JMX 等功能。适用于 Linux/macOS，便于日常运维和自动化部署。

---

## 适用环境与依赖

- 支持操作系统：Linux、macOS
- 依赖命令：`awk`、`pgrep`、`mktemp`、`timeout`（macOS 可用 `gtimeout`，需 `brew install coreutils`）
- 需安装 Java 8 及以上版本

---

## 快速开始

1. 赋予脚本可执行权限：
   ```bash
   chmod +x bin/launcher
   ```
2. 在项目根目录下执行脚本：
   ```bash
   ./bin/launcher
   ```
   默认以 `prod` 环境启动应用。

---

## 参数说明

| 参数 | 说明                                  | 示例                                             |
|----|-------------------------------------|------------------------------------------------|
| -s | 启动应用，需指定环境                          | `./bin/launcher -s dev`                        |
| -r | 重启应用，需指定环境                          | `./bin/launcher -r prod`                       |
| -S | 停止应用，需指定环境                          | `./bin/launcher -S test`                       |
| -c | 查看应用状态，需指定环境                        | `./bin/launcher -c test`                       |
| -t | 启动后 tail 全量日志（默认带超时时间）              | `./bin/launcher -s dev -t`                     |
| -q | 启动后 tail 日志（无超时时间，-t 与 -q 选一，-t 优先） | `./bin/launcher -s dev -q`                     |
| -T | 启动后将日志输出到临时文件（仅用于测试，不建议生产使用）        | `./bin/launcher -s dev -T`                     |
| -d | 启用 Debug 模式（默认端口 5005）              | `./bin/launcher -s dev -d 5005`                |
| -i | 启动时输出所有参数信息                         | `./bin/launcher -s dev -i`                     |
| -w | 启用 APM（应用性能监控）                      | `./bin/launcher -s dev -w`                     |
| -m | 启用 JMX 远程监控（需指定端口）                  | `./bin/launcher -s dev -m 10089`               |
| -o | 覆盖 JVM 启动参数                         | `./bin/launcher -s dev -o '-Xms256M -Xmx512M'` |
| -H | 显示帮助信息                              | `./bin/launcher -H`                            |

> **注意：**
> - `-s`、`-r`、`-S`、`-c` 参数后必须跟环境变量（如 dev/test/prod）。
> - `-d`、`-t`、`-T`、`-i` 参数不能单独使用，必须跟在 `-s` 或 `-r` 后面。
> - `-h` 参数为内部调用，无需手动传入。

---

## 常见问题与注意事项

- **日志目录**：默认日志目录为 `./logs`，可通过 `LOG_PATH` 或 `FINAL_LOG_PATH` 环境变量自定义。
- **JVM 参数**：可通过 `-o` 参数或修改脚本内 `JVM_OPTIONS` 变量自定义。
- **APM/JMX**：需提前配置好相关 agent 或端口。
- **依赖缺失**：如提示缺少 `timeout`，请在 macOS 上执行 `brew install coreutils` 并用 `gtimeout` 替代。
- **多实例部署**：建议通过 `APP_NAME` 区分不同实例。

---

## 联系方式与版权

- 作者：dong4j
- 邮箱：dong4j@outlook.com
- 版权声明：本脚本遵循 MIT License，可自由使用和修改。

---

如有问题或建议，欢迎 issue 或邮件联系！
