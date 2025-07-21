#!/bin/bash

set -euo pipefail

# 通用启动脚本, 如果需要自定义, 可在 `${project.basedir}/bin` 目录下创建同名文件, 框架在打包时将会使用自定义脚本.

#     __________         __                     .__                                  .__
#     \____    /  ____  |  | _______            |  |  _____    __ __   ____    ____  |  |__    ____  _______
#       /     / _/ __ \ |  |/ /\__  \    ______ |  |  \__  \  |  |  \ /    \ _/ ___\ |  |  \ _/ __ \ \_  __ \
#      /     /_ \  ___/ |    <  / __ \_ /_____/ |  |__ / __ \_|  |  /|   |  \\  \___ |   Y  \\  ___/  |  | \/
#     /_______ \ \___  >|__|_ \(____  /         |____/(____  /|____/ |___|  / \___  >|___|  / \___  > |__|
#             \/     \/      \/     \/                     \/             \/      \/      \/      \/
#                                       :: Zeka.Stack Boot Startup Script ::


################################################################################
# 变量定义区
################################################################################
# ANSI Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
NC='\033[0m'

# 默认参数、全局变量
ENV=${ENV:-"prod"}
# 默认为启动
FUNC="restart"
# 默认 debug 关闭
DEBUG_PORD="-1"
JMX_PORD="-1"
# 默认启动后 tail 日志
SHOW_LOG="off"
TIMEOUT_SHOWLOG="off"
SHOW_INFO="on"
ENABLE_APM="off"
# zeka.stack 的默认日志目录, 如果使用此目录, 日志会在 /mnt/syslogs/zeka.stack/{环境}/{应用名} 目录下, 如果要使用此配置, 需要将 FINAL_LOG_PATH 删除或置为空
LOG_PATH=${LOG_PATH:-"/mnt/syslogs/zeka.stack"}
# 设置日志路径为应用目录下的 logs 目录(启动脚本会覆盖应用中配置的 zeka-stack.logging.file.path)
FINAL_LOG_PATH=${FINAL_LOG_PATH:-"./logs"}
LOG_NAME=${LOG_NAME:-"all.log"}
# 自定义 JVM 参数
JVM_OPTIONS="#{jvmOptions}"

################################################################################
# 工具函数区
################################################################################
echo_red()    { echo -e "${RED}$1${NC}"; }
echo_green()  { echo -e "${GREEN}$1${NC}"; }
echo_yellow() { echo -e "${YELLOW}$1${NC}"; }

log_info() {
  echo -e "[$(date +'%Y-%m-%dT%H:%M:%S%z')][$$]: \033[32m [info] \033[0m $*" >&2
}

log_error() {
  echo -e "[$(date +'%Y-%m-%dT%H:%M:%S%z')][$$]: \033[31m [error] \033[0m $*" >&2
}

usage() {
  echo -e "${YELLOW}Usage: $0 [-s|-r|-S|-c] [env] [options]${NC}"
  echo -e "${YELLOW}
  使用说明：
  1. 脚本可在任意目录下执行。
  2. 最简单的用法是不传入任何参数（即：./server.sh，默认以 ${ENV} 环境启动应用）。
  3. -s、-r、-S 参数后必须跟环境变量（dev/test/prod）。
  4. -d、-t、-T、-i 参数不能单独使用，必须跟在 -s 或 -r 后面。
  5. -h 参数用于内部调用，用于通过 helper 子进程重启主应用，无需手动传入。
  ${NC}"
  echo -e "${YELLOW}
  可用参数列表：

  -s    启动应用
        示例：bin/server.sh                  （默认以 ${ENV} 环境启动）
              bin/server.sh -s test          （以 test 环境启动）

  -r    重启应用
        示例：bin/server.sh -r prod          （以 prod 环境重启）

  -S    停止应用
        示例：bin/server.sh -S test          （停止 test 环境应用）

  -c    查看状态
        示例：bin/server.sh -c test          （查看 test 环境运行状态）

  -t    启动后 tail 全量日志（默认带超时时间）
        示例：bin/server.sh -s dev -t

  -q    启动后 tail 日志（无超时时间，-t 与 -q 选一，-t 优先）
        示例：bin/server.sh -s dev -q

  -T    启动后将日志输出到临时文件（仅用于测试，不建议生产使用）
        示例：bin/server.sh -s dev -T

  -d    启用 Debug 模式（默认端口 5005）
        示例：bin/server.sh -s dev -d 5005

  -i    启动时输出所有参数信息
        示例：bin/server.sh -s dev -i

  -w    启用 APM（应用性能监控）
        示例：bin/server.sh -s dev -w

  -m    启用 JMX 远程监控（需指定端口）
        示例：bin/server.sh -s dev -m 10089

  -o    覆盖 JVM 启动参数
        示例：bin/server.sh -s dev -o '-Xms256M -Xmx512M'
  ${NC}"
  exit 1
}

check_dependencies() {
  for cmd in awk timeout pgrep mktemp; do
    command -v $cmd >/dev/null 2>&1 || { echo_red "缺少依赖命令: $cmd"; exit 1; }
  done
}

################################################################################
# 核心功能函数区
################################################################################
# 检测 Java 路径
detect_java_exe() {
  if [[ -n "${JAVA_HOME}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
    JAVA_EXE="${JAVA_HOME}/bin/java"
  elif JAVA_PATH=$(which java 2>/dev/null) && [[ -x "$JAVA_PATH" ]]; then
    JAVA_EXE="$JAVA_PATH"
  elif [[ -x "/usr/bin/java" ]]; then
    JAVA_EXE="/usr/bin/java"
  else
    JAVA_EXE=$(find /opt /usr/local /usr/lib/jvm -type f -name "java" -perm +111 2>/dev/null | head -n 1)
  fi
  if [[ -x "$JAVA_EXE" ]]; then
    echo_green "✅ 检测到 Java 路径: $JAVA_EXE"
  else
    echo_red "❌ 未检测到有效的 Java 可执行文件，请检查 JAVA_HOME 或安装 Java。"
    exit 1
  fi
}

# 是否看起远程 debug 端口
init_debug() {
  DEBUG_OPTS="-Dloader.debug=false"
  if [[ "${DEBUG_PORD}" != "-1" ]]; then
    if (("${DEBUG_PORD}" + 10)) &>/dev/null; then
      DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORD -Dloader.debug"
      echo_yellow "开启 DEBUG 模式: DEBUG_OPTS=$DEBUG_OPTS"
    else
      echo_red "请输入正确的端口号"
      exit 1
    fi
  fi
}

# 初始化 JMX 参数
init_jmx() {
  JMX_OPTIONS="-Dcom.sun.management.jmxremote=false"
  if [[ "${JMX_PORD}" != "-1" ]]; then
    if (("${JMX_PORD}" + 10)) &>/dev/null; then
      local local_ip
      local_ip=$(ifconfig -a | grep inet | grep -v 127.0.0.1 | grep -v inet6 | awk '{print $2}' | sed -e 's/addr://g')

      JMX_OPTIONS="-Dcom.sun.management.jmxremote
      -Dcom.sun.management.jmxremote.port=$JMX_PORD
      -Dcom.sun.management.jmxremote.ssl=false
      -Dcom.sun.management.jmxremote.authenticate=false
      -Djava.rmi.server.hostname=${local_ip}"

      echo_yellow "开启 JMX 模式: JMX_OPTIONS=${JMX_OPTIONS}"
    else
      echo_red "请输入正确的端口号"
      exit 1
    fi
  fi
}

# 获取应用部署路径等相关环境
prepare() {
  local app_home
  app_home="$(pwd)"
  if dirname "$0" | grep "^/" >/dev/null; then
    app_home=$(dirname "$0")
  else
    dirname "$0" | grep "^\." >/dev/null
    local retval=$?
    if [[ ${retval} -eq 0 ]]; then
      app_home=$(dirname "$0" | sed "s#^.#$app_home#")
    else
      app_home=$(dirname "$0" | sed "s#^#$app_home/#")
    fi
  fi

  # 默认使用打包后的 artifactId 作为应用名
  DEPLOY_DIR=$(dirname "$app_home")
  # 从 build-info.properties 读取应用名
  APP_NAME=$(awk -F '=' '{if($1~/build.project.name/) printf $2}' "${DEPLOY_DIR}"/config/build-info.properties)
  JAR_FILE=${DEPLOY_DIR}/${APP_NAME}.jar

  mkdir_log_file

  GC_LOG=${FINAL_LOG_PATH}/gc.log
}

# 处理 apm 参数
init_apm() {
  if [[ ${ENABLE_APM} = "on" ]]; then
    APM_OPTS="-javaagent:/opt/skywalking/agent/skywalking-agent.jar\n      -Dskywalking.agent.service_name=${APP_NAME}@${ENV}"
    echo_yellow "开启 APM 模式: APM_OPTS=$APM_OPTS"
  fi
}

# 获取 pid
check_pid() {
  local identify
  identify=${APP_NAME}@${ENV}
  echo $(pgrep -f $identify)
}

# 创建日志目录和文件
mkdir_log_file() {
    # 如果FINAL_LOG_PATH 未设置或为空字符串，则拼接默认路径
  if [[ -z "${FINAL_LOG_PATH}" ]]; then
    FINAL_LOG_PATH="${LOG_PATH}/${ENV}/${APP_NAME}"
  fi
  mkdir -p "${FINAL_LOG_PATH}"
  local log_file="${FINAL_LOG_PATH}/${LOG_NAME}"
  [[ -f "${log_file}" ]] || touch "${log_file}"
}

# 设置环境, 与应用配置 bootstrap.yml 的 ${ZEKA_NAME_SPACE} 对应, 此处设置的变量会被 spring 在启动时替换
# 使用 -DIDENTIFY 来区分应用 (由于服务器资源有限, 一台服务器可能会部署同一个应用, 只是环境不同而已)
# 重写 zeka-stack.logging.file.path 配置
running() {
  echo -e "JVM 启动参数: ${JVM_OPTIONS}"
  echo
  echo_green "启动命令:
    nohup ${JAVA_EXE} -jar
      -Djava.security.egd=file:/dev/./urandom
      ${JVM_OPTIONS}
      -Xloggc:${GC_LOG}
      -XX:ErrorFile=${DEPLOY_DIR}/app_error_%p.log
      -XX:+HeapDumpOnOutOfMemoryError
      -XX:HeapDumpPath=${DEPLOY_DIR}/app_error.hprof
      -XX:OnOutOfMemoryError='kill -9 %p'
      -Dloader.home=${DEPLOY_DIR}/
      -Dloader.path=lib/
      -DAPP_NAME=${APP_NAME}
      -DIDENTIFY=${APP_NAME}@${ENV}
      -DZEKA_NAME_SPACE=${ENV}
      -Ddeploy.path=${DEPLOY_DIR}
      -Dstart.type=${START_TYPE:-shell}
      -Dconfig.path=${DEPLOY_DIR}/config/
      -Dzeka-stack.logging.file.path=${FINAL_LOG_PATH}
      -Dzeka-stack.logging.file.name=${LOG_NAME}
      -Djar.file=${JAR_FILE}
      ${JMX_OPTIONS}
      ${DEBUG_OPTS}
      ${APM_OPTS}
      ${JAR_FILE}
      --spring.profiles.active=${ENV}
      --spring.config.location=${DEPLOY_DIR}/config/
      --slot.root=${DEPLOY_DIR}/
      --slot.path=patch/
      --slot.path=plugin/ >${FINAL_LOG_PATH}/${LOG_NAME} 2>&1 &"

  nohup "$JAVA_EXE" -jar \
    -Djava.security.egd=file:/dev/./urandom \
    ${JVM_OPTIONS} \
    -Xloggc:"$GC_LOG" \
    -XX:ErrorFile="$DEPLOY_DIR"/app_error_%p.log \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath="$DEPLOY_DIR"/app_error.hprof \
    -XX:OnOutOfMemoryError='kill -9 %p' \
    -Dloader.home="$DEPLOY_DIR"/ \
    -Dloader.path=lib/ \
    -DAPP_NAME="$APP_NAME" \
    -DIDENTIFY="$APP_NAME"@"$ENV" \
    -DZEKA_NAME_SPACE="$ENV" \
    -Ddeploy.path="$DEPLOY_DIR" \
    -Dstart.type="${START_TYPE:-shell}" \
    -Dconfig.path="$DEPLOY_DIR"/config/ \
    -Dzeka-stack.logging.file.path="$FINAL_LOG_PATH" \
    -Dzeka-stack.logging.file.name="$LOG_NAME" \
    -Djar.file="$JAR_FILE" \
    ${JMX_OPTIONS} \
    ${DEBUG_OPTS} \
    ${APM_OPTS} \
    "$JAR_FILE" \
    --spring.profiles.active="$ENV" \
    --spring.config.location="$DEPLOY_DIR"/config/ \
    --slot.root="$DEPLOY_DIR"/ \
    --slot.path=patch/ \
    --slot.path=plugin/ >"$FINAL_LOG_PATH"/"$LOG_NAME" 2>&1 &
  echo
  echo_green "----------------------------------------------------------------------------"
  echo_green " 启动应用: ${APP_NAME} ${ENV}  "
  echo_green " 日志路径: ${FINAL_LOG_PATH}/${LOG_NAME}"
  echo_green "----------------------------------------------------------------------------"
}

# 输出所有参数
show_info() {
  echo
  echo -e "\033[31m ENV: ${ENV} \033[0m"
  echo -e "\033[32m FUNC: ${FUNC} \033[0m"
  echo -e "\033[33m DEBUG_PORD: ${DEBUG_PORD} \033[0m"
  echo -e "\033[34m SHOW_LOG: ${SHOW_LOG} \033[0m"
  echo -e "\033[34m TIMEOUT_SHOWLOG: ${TIMEOUT_SHOWLOG} \033[0m"
  echo -e "\033[35m DEBUG_OPTS: ${DEBUG_OPTS} \033[0m"
  echo -e "\033[35m JMX_OPTIONS: ${JMX_OPTIONS} \033[0m"
  echo -e "\033[36m APP_NAME: ${APP_NAME} \033[0m"
  echo -e "\033[31m DEPLOY_DIR: ${DEPLOY_DIR} \033[0m"
  echo -e "\033[32m JAR_FILE: ${JAR_FILE} \033[0m"
  echo -e "\033[34m GC_LOG: ${GC_LOG} \033[0m"
  echo -e "\033[35m FINAL_LOG_PATH: ${FINAL_LOG_PATH} \033[0m"
  echo -e "\033[36m JAVA_HOME: ${JAVA_HOME} \033[0m"
  echo -e "\033[32m IDENTIFY: ${APP_NAME}@${ENV} \033[0m"
  echo -e "\033[33m ZEKA_NAME_SPACE: ${ENV} \033[0m"
  echo -e "\n"
}

# 启动应用 (-s env)
start() {
  echo
  echo_green "invoke start()"
  local pid
  pid="$(check_pid)"
  if [[ -z "$pid" ]]; then
    running
    if [[ ${SHOW_INFO} = "on" ]]; then
      show_info
    fi
    if [[ ${SHOW_LOG} = "on" ]]; then
      tail -n 100 -f "${FINAL_LOG_PATH}"/"${LOG_NAME}"
    else
      if [[ ${TIMEOUT_SHOWLOG} = "on" ]]; then
        # 让 tail -n 100 -f 命令最多运行 120 秒，超时后自动终止。
        timeout 120 tail -n 100 -f "${FINAL_LOG_PATH}"/"${LOG_NAME}"
      fi
    fi
  else
    echo_green "${APP_NAME}@${ENV} is running. [pid: $pid]"
  fi
}

# 关闭应用 (-S env)
stop() {
  echo
  echo_green "invoke stop()"
  local pid
  pid="$(check_pid)"
  if [[ -z "${pid}" ]]; then
    echo_red "The ${APP_NAME}@${ENV} does not started!"
  else
    local current_pid=${pid}
    echo_red "shudown the ${APP_NAME}@${ENV} [pid: ${current_pid}]"
    kill "${pid}" >/dev/null 2>&1
    local count=0
    local kill_count=0
    while [[ ${count} -lt 1 ]]; do
      echo -e ".\c"
      ((kill_count++))
      if [[ ${kill_count} -gt 5 ]]; then
        echo -e "\n"
        kill -9 "${pid}" >/dev/null 2>&1
      fi
      pid="$(check_pid)"
      if [[ -z "${pid}" ]]; then
        count=1
      fi
      sleep 1s
    done
    echo
    echo_green "${APP_NAME}@${ENV} shudown success [pid: ${current_pid}]"
  fi
}


# 重启应用 (-r env)
restart() {
  echo
  echo_green "invoke restart()"
  stop
  sleep 1s
  start
}

# 查看应用状态 (-c env)
status() {
  echo
  echo_green "invoke status()"
  local pid
  pid="$(check_pid)"
  if [[ -z "${pid}" ]]; then
    echo_red "${APP_NAME}@${ENV} not running!"
  else
    echo_green "${APP_NAME}@${ENV} running. [pid: ${pid}]"
  fi
}

################################################################################
# 参数解析区
################################################################################
parse_args() {
  while getopts "s:r:S:d:m:c:n:h:o:tqTiwH" opt; do
    case ${opt} in
      s) ENV=${OPTARG}; FUNC="start";;                    # 启动应用, 跟环境变量
      r) ENV=${OPTARG}; FUNC="restart";;                  # 重启应用 跟环境变量
      S) ENV=${OPTARG}; FUNC="stop";;                     # 关闭应用
      c) ENV=${OPTARG}; FUNC="status";;                   # 查看状态
      d) DEBUG_PORD=${OPTARG};;                           # 使用 debug 模式 跟监听端口
      m) JMX_PORD=${OPTARG};;                             # JMX 端口
      t) SHOW_LOG="on";;                                  # 开启日志输出
      q) TIMEOUT_SHOWLOG="on";;                           # 限制日志输出时间
      T) SHOW_LOG="on"; LOG_PATH=$(mktemp -d);;           # 将日志输出到临时目录
      h|H) usage;;                                        # 帮助说明
      o) JVM_OPTIONS=${OPTARG};;                          # 设置 JVM 参数
      i) SHOW_INFO="on";;                                 # 输出脚本参数信息
      w) ENABLE_APM="on";;                                # 开启 APM
      \?) echo_red "参数列表错误 使用 -H 查看帮助"; exit 1;;
    esac
  done
}

################################################################################
# 主流程区
################################################################################
main() {
  echo
  echo_green "     __________         __                     .__                                  .__                         "
  echo_green "     \____    /  ____  |  | _______            |  |  _____    __ __   ____    ____  |  |__    ____  _______     "
  echo_green "       /     / _/ __ \ |  |/ /\__  \    ______ |  |  \__  \  |  |  \ /    \ _/ ___\ |  |  \ _/ __ \ \_  __ \    "
  echo_green "      /     /_ \  ___/ |    <  / __ \_ /_____/ |  |__ / __ \_|  |  /|   |  \\  \___ |   Y  \\  ___/  |  | \/    "
  echo_green "     /_______ \ \___  >|__|_ \(____  /         |____/(____  /|____/ |___|  / \___  >|___|  / \___  > |__|       "
  echo_green "             \/     \/      \/     \/                     \/             \/      \/      \/      \/             "
  echo_green "                                        :: Zeka.Stack Boot Startup Script ::                                    "
  echo
  check_dependencies
  detect_java_exe
  parse_args "$@"
  echo
  echo_green "--------------------------------------"
  echo_green " 1. 处理 debug 参数 "
  echo_green "--------------------------------------"
  init_debug
  echo
  echo_green "--------------------------------------"
  echo_green " 2. 处理 JMX 参数 "
  echo_green "--------------------------------------"
  init_jmx
  echo
  echo_green "--------------------------------------"
  echo_green " 3. 处理部署相关参数 "
  echo_green "--------------------------------------"
  prepare
  echo
  echo_green "--------------------------------------"
  echo_green " 4. 初始化 APM 参数 "
  echo_green "--------------------------------------"
  init_apm
  case ${FUNC} in
    start)   start   ;;
    stop)    stop    ;;
    restart) restart ;;
    status)  status  ;;
    *) echo_red "参数错误 require -s|-r|-S|-c" ;;
  esac
}

main "$@"
