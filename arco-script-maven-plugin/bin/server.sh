#!/bin/bash

# 通用启动脚本, 如果需要自定义, 可在 `${project.basedir}/bin` 目录下创建同名文件, 框架在打包时将会使用自定义脚本.

#     __________         __                     .__                                  .__
#     \____    /  ____  |  | _______            |  |  _____    __ __   ____    ____  |  |__    ____  _______
#       /     / _/ __ \ |  |/ /\__  \    ______ |  |  \__  \  |  |  \ /    \ _/ ___\ |  |  \ _/ __ \ \_  __ \
#      /     /_ \  ___/ |    <  / __ \_ /_____/ |  |__ / __ \_|  |  /|   |  \\  \___ |   Y  \\  ___/  |  | \/
#     /_______ \ \___  >|__|_ \(____  /         |____/(____  /|____/ |___|  / \___  >|___|  / \___  > |__|
#             \/     \/      \/     \/                     \/             \/      \/      \/      \/
#                                       :: Zeka.Stack Boot Startup Script ::

# ANSI Colors
echo_red() { echo $'\e[0;31m'"$1"$'\e[0m'; }
echo_green() { echo $'\e[0;32m'"$1"$'\e[0m'; }
echo_yellow() { echo $'\e[0;33m'"$1"$'\e[0m'; }

# info级别的日志
log_info() {
  echo -e "[$(date +'%Y-%m-%dT%H:%M:%S%z')][$$]: \033[32m [info] \033[0m $*" >&2
}

# error级别的日志
log_error() {
  echo -e "[$(date +'%Y-%m-%dT%H:%M:%S%z')][$$]: \033[31m [error] \033[0m $*" >&2
}

# 初始化 debug 参数
function init_debug() {
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
function init_jmx() {
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

# 获取应用部署路径
function prepare() {
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
function init_apm() {
  if [[ ${ENABLE_APM} = "on" ]]; then
      APM_OPTS="-javaagent:/opt/skywalking/agent/skywalking-agent.jar
      -Dskywalking.agent.service_name=${APP_NAME}@${ENV}"
      echo_yellow "开启 APM 模式: APM_OPTS=$APM_OPTS"
  fi
}

# 获取 pid
function check_pid() {
  local identify
  identify=${APP_NAME}@${ENV}
  echo $(pgrep -f $identify)
}

# 创建日志目录和文件
function mkdir_log_file() {
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
function running() {
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
function show_info() {
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
function start() {
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
			timeout 120 tail -n 100 -f "${FINAL_LOG_PATH}"/"${LOG_NAME}"
		fi
    fi

  else
    echo_green "${APP_NAME}@${ENV} is running. [pid: $pid]"
  fi
}

# 关闭应用 (-S env)
function stop() {
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
    # count 小于 1
    while [[ ${count} -lt 1 ]]; do
      echo -e ".\c"
      ((kill_count++))
      if [[ ${kill_count} -gt 5 ]]; then
        echo -e "\n"
        kill -9 "${pid}" >/dev/null 2>&1
      fi
      # 检查是否干掉 app
      pid="$(check_pid)"
      # 如果为空, 则退出循环
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
function restart() {
  echo
  echo_green "invoke restart()"

  stop
  sleep 1s
  start
}

# 查看应用状态 (-c env)
function status() {
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

############################################################
#                       从这里开始执行                        #
############################################################
echo
echo_green "     __________         __                     .__                                  .__                         "
echo_green "     \____    /  ____  |  | _______            |  |  _____    __ __   ____    ____  |  |__    ____  _______     "
echo_green "       /     / _/ __ \ |  |/ /\__  \    ______ |  |  \__  \  |  |  \ /    \ _/ ___\ |  |  \ _/ __ \ \_  __ \    "
echo_green "      /     /_ \  ___/ |    <  / __ \_ /_____/ |  |__ / __ \_|  |  /|   |  \\  \___ |   Y  \\  ___/  |  | \/    "
echo_green "     /_______ \ \___  >|__|_ \(____  /         |____/(____  /|____/ |___|  / \___  >|___|  / \___  > |__|       "
echo_green "             \/     \/      \/     \/                     \/             \/      \/      \/      \/             "
echo_green "                                        :: Zeka.Stack Boot Startup Script ::                                    "
echo

# shell 脚本设置的都是环境变量, 在 Java 代码中使用 System.getenv 获取, 而在应用中通过 System.setProperty 设置的属于 JVM 系统属性(等同于 -D)
# 所以下面的环境变量都你可以被应用覆盖.
# Spring Boot 中用于配置解析的 Environment 是分层的：
# 	1.	JVM 系统属性（System.getProperty，如 -DAPP_NAME=...，或在代码中 setProperty）
#	2.	操作系统环境变量（System.getenv()）
#	3.	application.yml、application.properties
# Spring Cloud 中 ${APP_NAME} 解析优先级仍然是：
#	1.	JVM 启动参数（-DAPP_NAME=xxx）
#	2.	System.setProperty("APP_NAME", ...)
#	3.	操作系统环境变量（export APP_NAME=xxx）
#	4.	application.yml / bootstrap.yml
#	5.	Spring Cloud Config Server 远程配置

# 自动检测 Java 路径
if [[ -n "${JAVA_HOME}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]]; then
  JAVA_EXE="${JAVA_HOME}/bin/java"
elif JAVA_PATH=$(which java 2>/dev/null) && [[ -x "$JAVA_PATH" ]]; then
  JAVA_EXE="$JAVA_PATH"
elif [[ -x "/usr/bin/java" ]]; then
  JAVA_EXE="/usr/bin/java"
else
  # 在常见路径中查找 Java（只取第一个）
  JAVA_EXE=$(find /opt /usr/local /usr/lib/jvm -type f -name "java" -perm +111 2>/dev/null | head -n 1)
fi

# 校验并输出结果
if [[ -x "$JAVA_EXE" ]]; then
  echo_green "✅ 检测到 Java 路径: $JAVA_EXE"
else
  echo_red "❌ 未检测到有效的 Java 可执行文件，请检查 JAVA_HOME 或安装 Java。"
  exit 1
fi

# 默认 dev 环境
ENV="prod"
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
# 设置日志路径为应用目录下的 logs 目录
FINAL_LOG_PATH=${FINAL_LOG_PATH:-"./logs"}
LOG_NAME="all.log"
# 自定义 JVM 参数
JVM_OPTIONS="#{jvmOptions}"
# 自定义参数, 先设置变量
while getopts "s:r:S:d:m:c:n:h:o:tqTiwH" opt; do
  case ${opt} in
  # 启动应用, 跟环境变量
  s)
    ENV=${OPTARG}
    FUNC="start"
    ;;
  # 重启应用 跟环境变量
  r)
    ENV=${OPTARG}
    FUNC="restart"
    ;;
  c)
    ENV=${OPTARG}
    FUNC="status"
    ;;
  # 关闭应用
  S)
    ENV=${OPTARG}
    FUNC="stop"
    ;;
  # 使用 debug 模式 跟监听端口
  d)
    DEBUG_PORD=${OPTARG}
    ;;
  m)
    JMX_PORD=${OPTARG}
    ;;
  t)
    SHOW_LOG="on"
    ;;
  q)
    TIMEOUT_SHOWLOG="on"
    ;;
  T)
    SHOW_LOG="on"
    # 创建一个临时目录，并将其路径赋值给 LOG_PATH 变量
    LOG_PATH=$(mktemp -d)
    ;;
  h)
    ENV=${OPTARG}
    FUNC="helper"
    ;;
  o)
    JVM_OPTIONS=${OPTARG}
    ;;
  i)
    SHOW_INFO="on"
    ;;
  w)
    ENABLE_APM="on"
    ;;
  H)
    echo -e "\033[0;36m
1. 在任何目录下执行此脚本都可以.
2. 最简单的命令就是不输入任何参数 (./server.sh 即以${ENV}环境启动应用).
3. 输入 -s , -r 和 -S 参数, 后面必须跟环境变量 (dev/test/prod);
4. -d, -t, -T -i 参数不能单独存在, 且必须跟在 -s 或者 -r 后面;
5. -h 参数不需要手动调用, 是程序自动调用, 用于启动 helper 进程来对主应用进行重启操作.\033[0m  \033[0;34m

-s:启动应用             exp: bin/server.sh                      (默认以 ${ENV} 环境启动应用, 不需要输入任何参数)
-s:启动应用             exp: bin/server.sh -s test              (以 test 环境启动应用)
-r:重启应用             exp: bin/server.sh -r prod              (以 prod 环境重启应用)
-S:关闭应用             exp: bin/server.sh -S test              (关闭应用)
-c:查看状态             exp: bin/server.sh -c test              (查看应用状态)
-t:查看日志             exp: bin/server.sh -s dev               (启动时自动 tail 全量日志)
-q:查看日志(0秒超时时间)exp：bin/server.sh -s dev -q   		   (-t和-q全是查看日志，一个有超时时间，一个没有，二择其一就可以，-t优先级要高)
-T:输出日志到临时文件\texp: bin/server.sh -s dev -T\t\t(启动时自动 tail 全量日志, 主要用于部署测试用, 日志会写入到临时文件, 生产环境不要使用)
-d:debug 模式\t\texp: bin/server.sh -s dev -d 5005\t(以 dev 环境启动应用, 端口默认为 5005, 并且开启 debug 模式, 用于 idea 远程调试)
-i:输出所有参数信息\texp: bin/server.sh -s dev -i\t\t(启动时输出参数信息)
-w:启用 APM\texp: bin/server.sh -s dev -w\t\t(启动时输出参数信息)
-m:启用 JMX 远程监控\texp: bin/server.sh -s dev -m 10089\t\t(需要添加监听端口)
-o:重写 JVM 配置\texp: bin/server.sh -s dev -o '-Xms256M -Xmx512M'\t\t(启动时输出参数信息)
\033[0m"
    exit 1
    ;;
  \?)
    echo_red "参数列表错误 使用 -H 查看帮助"
    exit 1
    ;;
  esac
done

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

# 通过 func 来执行具体方法
case ${FUNC} in
start) start ;;
stop) stop ;;
restart) restart ;;
status) status ;;
*) echo_red "参数错误 require -s|-r|-S|-c" ;;
esac
