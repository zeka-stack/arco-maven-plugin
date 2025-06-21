#!/bin/bash
# https://www.linuxjournal.com/content/add-binary-payload-your-shell-scripts

DEPLOY_FILE=$1
DEPLOY_DIR=$(echo ${DEPLOY_FILE} | sed 's/\.tar\.gz//')

# 将当前处理的 tar.gz 写入到部署脚本中
sed \
-e "s/DEPLOY_FILE=/DEPLOY_FILE=${DEPLOY_FILE}/" \
-e "s/DEPLOY_DIR=/DEPLOY_DIR=${DEPLOY_DIR}/" \
install.sh > ${DEPLOY_DIR}.run

# 写入标识位
echo "PAYLOAD:" >> ${DEPLOY_DIR}.run
# 写入部署包数据到启动脚本
cat $1 >> ${DEPLOY_DIR}.run
