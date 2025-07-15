# 使用 dockerfile-base 构建的基础镜像
FROM zeka-stack-java8-base:latest

# 添加构建元信息
LABEL maintainer="dong4j <dong4j@gmail.com>"
LABEL project="zeka.stack"
LABEL description="基于 JRE 8 的 Spring Boot 应用容器镜像"

# 定义应用启动方式
ENV START_TYPE=docker
# 创建工作目录
WORKDIR /app
# 拷贝并解压部署包, package.name 变量由插件自动注入
ADD ${PACKAGE.NAME}.tar.gz /app/
# 进入部署目录
WORKDIR /app/${PACKAGE.NAME}
# 暴露端口, export.port 变量由插件自动注入
${EXPORT.PORT}
# 健康检查
${HEALTHCHECK}
# 启动 docker run 时还要加上 -s dev/test/prev/prod 和其他支持的命令和参数
ENTRYPOINT ["bin/server.sh"]
