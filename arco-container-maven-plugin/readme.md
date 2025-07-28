Dockerfile-B: 基础镜像
Dockerfile-M: 提供分层, 充分利用分层缓存
Dockerfile-S: 无法利用分层缓存

## 构建基础镜像

```shell
cd docker && docker build -f Dockerfile-B -t zeka-stack-java8-base:latest .
```

## 构建应用镜像

## 构建多平台镜像

```shell
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t your-registry/your-image-name:tag \
  -f Dockerfile . \
  --push
```

参数说明：

- `--platform`: 构建目标平台，可指定多个
- `-t`: 镜像名和标签
- `-f`: Dockerfile 路径
- `.`: 构建上下文（当前目录）
- `--push`: 构建完成后直接推送到镜像仓库（如 DockerHub）

## 一键构建

```
.
├── Dockerfile-B
├── Dockerfile-M           # 应用服务的 Dockerfile
├── docker-compose.yml
├── xxx/
│   ├── bin/
│   ├── config/
│   ├── lib/
│   └── xxx.jar
```

```yaml
services:
  # 构建基础镜像
  base:
    build:
      context: .
      dockerfile: Dockerfile-B
    image: zeka-stack-java8-base:latest
    # 不需要运行该服务
    entrypoint: [ "true" ]

  # 构建并运行应用服务
  app:
    build:
      context: .
      dockerfile: Dockerfile-M
    image: app:latest
    ports:
      - "8080:8080"
    volumes:
      - ./app:/app
    depends_on:
      - base
    command: ["bin/launcher", "-s", "dev", "-t", "-i"]
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
```

```shell
# 一键构建并运行
docker-compose up --build
```
