#!/bin/bash

DEPLOY_FILE=
DEPLOY_DIR=

if [[ ! -d ${DEPLOY_DIR}  ]];then
  match=$(grep --text --line-number '^PAYLOAD:$' $0 | cut -d ':' -f 1)
  payload_start=$((match + 1))
  # 解压文件
  tail -n +${payload_start} $0 | tar -zxvf ${DEPLOY_FILE}
  # 删除临时文件
  rm -rf '-'
  # 修改目录权限
  # 切换用户
fi

# 执行部署脚本
cd ${DEPLOY_DIR}
bin/launcher -r local -T

exit 0
