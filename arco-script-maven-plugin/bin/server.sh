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
JVM_OPTIONS="-Xms128M -Xmx256M "

################################################################################
# 工具函数区
################################################################################
# 颜色与样式
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

# 美观输出函数
print_title()   { echo -e "${BOLD}${CYAN}\n==== $1 ====\n${NC}"; }
print_success() { echo -e "${GREEN}✅ $1${NC}"; }
print_error()   { echo -e "${RED}❌ $1${NC}"; }
print_warn()    { echo -e "${YELLOW}⚠️  $1${NC}"; }
print_info()    { echo -e "${BLUE}ℹ️  $1${NC}"; }
print_line()    { echo -e "${PURPLE}------------------------------------------------------------------------------------------------------------${NC}"; }

# 兼容原有输出函数
# 用美观输出函数替换
log_info()  { print_info "$*"; }
log_error() { print_error "$*"; }

usage() {
  echo -e "${YELLOW}Usage: $0 [-s|-r|-S|-c] [env] [options]${NC}"
  echo -e "${YELLOW}
  使用说明：
  1. 脚本可在任意目录下执行。
  2. 最简单的用法是不传入任何参数（即：./server.sh，默认以 ${ENV} 环境启动应用）
  3. -s、-r、-S 参数后必须跟环境变量（dev/test/prod）
  4. -d、-t、-T、-i 参数不能单独使用，必须跟在 -s 或 -r 后面
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
  for cmd in awk pgrep mktemp; do
    command -v $cmd >/dev/null 2>&1 || { print_error "缺少依赖命令: $cmd"; exit 1; }
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
    print_success "检测到 Java 路径: $JAVA_EXE"
  else
    print_error "未检测到有效的 Java 可执行文件，请检查 JAVA_HOME 或安装 Java。"
    exit 1
  fi
}

# 是否看起远程 debug 端口
init_debug() {
  DEBUG_OPTS="-Dloader.debug=false"
  if [[ "${DEBUG_PORD}" != "-1" ]]; then
    if (("${DEBUG_PORD}" + 10)) &>/dev/null; then
      DEBUG_OPTS="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:$DEBUG_PORD -Dloader.debug"
      print_warn "开启 DEBUG 模式: DEBUG_OPTS=$DEBUG_OPTS"
    else
      print_error "请输入正确的端口号"
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

      print_warn "开启 JMX 模式: JMX_OPTIONS=${JMX_OPTIONS}"
    else
      print_error "请输入正确的端口号"
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
  print_info "部署目录: $DEPLOY_DIR"
  print_info "应用名: $APP_NAME"
  print_info "JAR 包: $JAR_FILE"
  print_info "日志目录: $FINAL_LOG_PATH"
}

# 处理 apm 参数
init_apm() {
  if [[ ${ENABLE_APM} = "on" ]]; then
    APM_OPTS="-javaagent:/opt/skywalking/agent/skywalking-agent.jar\n      -Dskywalking.agent.service_name=${APP_NAME}@${ENV}"
    print_warn "开启 APM 模式: APM_OPTS=$APM_OPTS"
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
  print_info "日志文件: $log_file"
}

# 设置环境, 与应用配置 bootstrap.yml 的 ${ZEKA_NAME_SPACE} 对应, 此处设置的变量会被 spring 在启动时替换
# 使用 -DIDENTIFY 来区分应用 (由于服务器资源有限, 一台服务器可能会部署同一个应用, 只是环境不同而已)
# 重写 zeka-stack.logging.file.path 配置
running() {
  print_info "JVM 启动参数: ${JVM_OPTIONS}"
  print_title "启动命令"
  echo -e "启动命令:
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
  print_success "应用 ${APP_NAME} 启动命令已执行。"
  print_line
  print_success "启动应用: ${APP_NAME} ${ENV}"
  print_info "日志路径: ${FINAL_LOG_PATH}/${LOG_NAME}"
  print_line
}

# 输出所有参数
show_info() {
  print_title "当前参数信息"
  echo -e "${CYAN}ENV: ${YELLOW}${ENV}${NC}"
  echo -e "${CYAN}FUNC: ${YELLOW}${FUNC}${NC}"
  echo -e "${CYAN}DEBUG_PORD: ${YELLOW}${DEBUG_PORD}${NC}"
  echo -e "${CYAN}SHOW_LOG: ${YELLOW}${SHOW_LOG}${NC}"
  echo -e "${CYAN}TIMEOUT_SHOWLOG: ${YELLOW}${TIMEOUT_SHOWLOG}${NC}"
  echo -e "${CYAN}DEBUG_OPTS: ${YELLOW}${DEBUG_OPTS}${NC}"
  echo -e "${CYAN}JMX_OPTIONS: ${YELLOW}${JMX_OPTIONS}${NC}"
  echo -e "${CYAN}APP_NAME: ${YELLOW}${APP_NAME}${NC}"
  echo -e "${CYAN}DEPLOY_DIR: ${YELLOW}${DEPLOY_DIR}${NC}"
  echo -e "${CYAN}JAR_FILE: ${YELLOW}${JAR_FILE}${NC}"
  echo -e "${CYAN}GC_LOG: ${YELLOW}${GC_LOG}${NC}"
  echo -e "${CYAN}FINAL_LOG_PATH: ${YELLOW}${FINAL_LOG_PATH}${NC}"
  echo -e "${CYAN}JAVA_HOME: ${YELLOW}${JAVA_HOME}${NC}"
  echo -e "${CYAN}IDENTIFY: ${YELLOW}${APP_NAME}@${ENV}${NC}"
  echo -e "${CYAN}ZEKA_NAME_SPACE: ${YELLOW}${ENV}${NC}"
  print_line
}

# 启动应用 (-s env)
start() {
  print_title "启动应用"
  local pid
  pid="$(check_pid)"

  if [[ -z "$pid" ]]; then
    running

    [[ ${SHOW_INFO} == "on" ]] && show_info

    if [[ ${SHOW_LOG} == "on" ]]; then
      tail -n 100 -f "${FINAL_LOG_PATH}/${LOG_NAME}"
    elif [[ ${TIMEOUT_SHOWLOG} == "on" ]]; then
      # 自动适配 timeout 命令（macOS 用 gtimeout）
      if command -v timeout >/dev/null 2>&1; then
        TIMEOUT_CMD=timeout
      elif command -v gtimeout >/dev/null 2>&1; then
        TIMEOUT_CMD=gtimeout
      else
        echo -e "\033[0;31m请先安装 timeout 或 gtimeout（macOS 可用 brew install coreutils）\033[0m"
        exit 1
      fi

      # 让 tail -n 100 -f 命令最多运行 120 秒，超时后自动终止。
      ${TIMEOUT_CMD} 120 tail -n 100 -f "${FINAL_LOG_PATH}/${LOG_NAME}" || true
    fi
  else
    print_warn "${APP_NAME}@${ENV} 已在运行中 [pid: $pid]"
  fi
}

# 关闭应用 (-S env)
stop() {
  print_title "停止应用"
  local pid
  pid="$(check_pid)"
  if [[ -z "${pid}" ]]; then
    print_warn "${APP_NAME}@${ENV} 未启动！"
  else
    local current_pid=${pid}
    print_warn "正在关闭 ${APP_NAME}@${ENV} [pid: ${current_pid}]"
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
    print_success "${APP_NAME}@${ENV} 已成功关闭 [pid: ${current_pid}]"
  fi
}


# 重启应用 (-r env)
restart() {
  print_title "重启应用"
  stop
  sleep 1s
  start
}

# 查看应用状态 (-c env)
status() {
  print_title "应用状态"
  local pid
  pid="$(check_pid)"
  if [[ -z "${pid}" ]]; then
    print_warn "${APP_NAME}@${ENV} 未运行！"
  else
    print_success "${APP_NAME}@${ENV} 正在运行 [pid: ${pid}]"
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
      H) usage;;                                        # 帮助说明
      o) JVM_OPTIONS=${OPTARG};;                          # 设置 JVM 参数
      i) SHOW_INFO="on";;                                 # 输出脚本参数信息
      w) ENABLE_APM="on";;                                # 开启 APM
      \?) print_error "参数列表错误 使用 -H 查看帮助"; exit 1;;
    esac
  done
}

################################################################################
# 主流程区
################################################################################
main() {
  echo
  echo "     __________         __                     .__                                  .__                         "
  echo "     \____    /  ____  |  | _______            |  |  _____    __ __   ____    ____  |  |__    ____  _______     "
  echo "       /     / _/ __ \ |  |/ /\__  \    ______ |  |  \__  \  |  |  \ /    \ _/ ___\ |  |  \ _/ __ \ \_  __ \    "
  echo "      /     /_ \  ___/ |    <  / __ \_ /_____/ |  |__ / __ \_|  |  /|   |  \\  \___ |   Y  \\  ___/  |  | \/    "
  echo "     /_______ \ \___  >|__|_ \(____  /         |____/(____  /|____/ |___|  / \___  >|___|  / \___  > |__|       "
  echo "             \/     \/      \/     \/                     \/             \/      \/      \/      \/             "
  echo "                                        :: Zeka.Stack Boot Startup Script ::                                    "
  echo

  check_dependencies
  parse_args "$@"
  print_line
  print_info "检测 Java 环境"
  detect_java_exe
  print_line
  print_info "处理 debug 参数"
  init_debug
  print_info "处理 JMX 参数"
  init_jmx
  print_info "处理部署相关参数"
  prepare
  print_info "初始化 APM 参数"
  init_apm
  print_line
  case ${FUNC} in
    start)   start   ;;
    stop)    stop    ;;
    restart) restart ;;
    status)  status  ;;
    *) print_error "参数错误 require -s|-r|-S|-c" ;;
  esac
}

main "$@"
