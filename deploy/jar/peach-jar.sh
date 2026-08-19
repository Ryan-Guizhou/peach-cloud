#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
REPO_DIR=$(CDPATH= cd -- "$DEPLOY_DIR/.." && pwd)
ENV_FILE="$SCRIPT_DIR/.env"
RUNTIME_DIR="$SCRIPT_DIR/runtime"
PID_DIR="$RUNTIME_DIR/pids"
LOG_DIR="$RUNTIME_DIR/logs"

SERVICES="peach-auth peach-monitor peach-fileservice peach-message peach-setting peach-generator peach-gateway"
STOP_SERVICES="peach-gateway peach-generator peach-setting peach-message peach-fileservice peach-monitor peach-auth"

usage() {
  cat <<'USAGE'
Usage: ./peach-jar.sh <command> [service|all] [options]

Commands:
  init                         Create deploy/jar/.env and runtime directories
  start [service|all]          Start all services or one service with nohup java -jar
  stop [service|all]           Stop all services or one service by pid file
  restart [service|all]        Restart all services or one service
  status [service|all]         Show process status
  logs [service|all] [options] Show logs

Log options:
  --tail <lines>               Lines to show, default: 200
  -f, --follow                 Follow log output

Services:
  peach-gateway peach-auth peach-monitor peach-fileservice
  peach-message peach-setting peach-generator
USAGE
}

ensure_env() {
  if [ ! -f "$ENV_FILE" ]; then
    cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
    echo "Created deploy/jar/.env from .env.example. Edit middleware addresses and passwords before starting services."
  fi
}

load_env() {
  ensure_env
  set -a
  . "$ENV_FILE"
  set +a
}

ensure_dirs() {
  mkdir -p \
    "$PID_DIR" \
    "$LOG_DIR/peach-gateway" \
    "$LOG_DIR/peach-auth" \
    "$LOG_DIR/peach-monitor" \
    "$LOG_DIR/peach-fileservice" \
    "$LOG_DIR/peach-message" \
    "$LOG_DIR/peach-setting" \
    "$LOG_DIR/peach-generator" \
    "$RUNTIME_DIR/upload/peach-fileservice"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 1
  fi
}

check_java_runtime() {
  java_bin="${JAVA_BIN:-java}"
  required="${JAVA_REQUIRED_MAJOR:-21}"
  version_output=$("$java_bin" -version 2>&1 | sed -n '1p')
  major=$(printf '%s\n' "$version_output" | sed -E 's/.*version "([0-9]+).*/\1/')
  if [ -z "$major" ] || [ "$major" = "$version_output" ]; then
    echo "Unable to detect Java version from: $version_output" >&2
    exit 1
  fi
  if [ "$major" -lt "$required" ]; then
    echo "Java $required or later is required, actual: $version_output" >&2
    exit 1
  fi
}

init_all() {
  ensure_env
  ensure_dirs
  echo "Jar deploy workspace initialized."
}

service_config() {
  service="$1"
  case "$service" in
    peach-gateway)
      SERVICE_PORT=${GATEWAY_PORT:-18080}
      SERVICE_JAR="peach-gateway/peach-gateway-launch/target/peach-gateway-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-auth)
      SERVICE_PORT=${AUTH_PORT:-18081}
      SERVICE_JAR="peach-auth/peach-auth-launch/target/peach-auth-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-monitor)
      SERVICE_PORT=${MONITOR_PORT:-18082}
      SERVICE_JAR="peach-monitor/peach-monitor-launch/target/peach-monitor-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-fileservice)
      SERVICE_PORT=${FILESERVICE_PORT:-18083}
      SERVICE_JAR="peach-fileservice/peach-fileservice-launch/target/peach-fileservice-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-message)
      SERVICE_PORT=${MESSAGE_PORT:-18084}
      SERVICE_JAR="peach-message/peach-message-launch/target/peach-message-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-setting)
      SERVICE_PORT=${SETTING_PORT:-18085}
      SERVICE_JAR="peach-setting/peach-setting-launch/target/peach-setting-launch-1.0.0-SNAPSHOT.jar"
      ;;
    peach-generator)
      SERVICE_PORT=${GENERATOR_PORT:-18086}
      SERVICE_JAR="peach-generator/peach-generator-launch/target/peach-generator-launch-1.0.0-SNAPSHOT.jar"
      ;;
    *)
      echo "Unknown service: $service" >&2
      exit 1
      ;;
  esac
}

pid_file() {
  printf '%s/%s.pid' "$PID_DIR" "$1"
}

log_file() {
  printf '%s/%s/nohup.log' "$LOG_DIR" "$1"
}

is_pid_alive() {
  pid="$1"
  kill -0 "$pid" >/dev/null 2>&1
}

pid_matches_service() {
  pid="$1"
  service="$2"
  args=$(ps -p "$pid" -o args= 2>/dev/null || true)
  printf '%s' "$args" | grep -q "$service"
}

running_pid() {
  service="$1"
  file=$(pid_file "$service")
  if [ ! -f "$file" ]; then
    return 1
  fi
  pid=$(cat "$file" 2>/dev/null || true)
  if [ -z "$pid" ]; then
    return 1
  fi
  if is_pid_alive "$pid" && pid_matches_service "$pid" "$service"; then
    printf '%s' "$pid"
    return 0
  fi
  rm -f "$file"
  return 1
}

start_service() {
  service="$1"
  service_config "$service"
  if pid=$(running_pid "$service"); then
    echo "$service already running, pid=$pid"
    return 0
  fi

  jar_path="$REPO_DIR/$SERVICE_JAR"
  if [ ! -f "$jar_path" ]; then
    echo "Jar not found: $jar_path" >&2
    echo "Build backend jars first, for example: ./mvnw -DskipTests package" >&2
    exit 1
  fi

  mkdir -p "$LOG_DIR/$service"
  log=$(log_file "$service")
  pidfile=$(pid_file "$service")
  upload_root_abs="$SCRIPT_DIR/${PEACH_UPLOAD_ROOT:-runtime/upload/peach-fileservice}"
  mkdir -p "$upload_root_abs"

  (
    cd "$REPO_DIR"
    export TZ=${TZ:-Asia/Shanghai}
    export NACOS_SERVER_ADDR=${NACOS_SERVER_ADDR:-127.0.0.1:8849}
    export SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=${SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR:-${NACOS_SERVER_ADDR}}
    export SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=${SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR:-${NACOS_SERVER_ADDR}}
    export SPRING_CLOUD_NACOS_CONFIG_NAMESPACE=${SPRING_CLOUD_NACOS_CONFIG_NAMESPACE:-peach-cloud}
    export SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE=${SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE:-peach-cloud}
    export SPRING_CLOUD_NACOS_CONFIG_GROUP=${SPRING_CLOUD_NACOS_CONFIG_GROUP:-PEACH-CLOUD}
    export SPRING_CLOUD_NACOS_DISCOVERY_GROUP=${SPRING_CLOUD_NACOS_DISCOVERY_GROUP:-PEACH-CLOUD}
    export SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION=${SPRING_CLOUD_NACOS_CONFIG_FILE_EXTENSION:-yml}
    export SPRING_CLOUD_NACOS_CONFIG_ENCODE=${SPRING_CLOUD_NACOS_CONFIG_ENCODE:-UTF-8}
    export MYSQL_DATABASE=${MYSQL_DATABASE:-peach_cloud}
    export MYSQL_HOST=${MYSQL_HOST:-127.0.0.1:3307}
    export MYSQL_ROOT_PASSWORD=${MYSQL_ROOT_PASSWORD:-}
    export REDIS_HOST=${REDIS_HOST:-127.0.0.1:6380}
    export REDIS_PASSWORD=${REDIS_PASSWORD:-}
    export PEACH_UPLOAD_ROOT="$upload_root_abs"
    nohup ${JAVA_BIN:-java} ${JAVA_OPTS:-} -jar "$jar_path" \
      --server.port="$SERVICE_PORT" \
      --spring.application.name="$service" \
      --spring.profiles.active="${SPRING_PROFILES_ACTIVE:-docker}" \
      >> "$log" 2>&1 &
    echo $! > "$pidfile"
  )

  sleep 1
  if pid=$(running_pid "$service"); then
    echo "Started $service, pid=$pid, port=$SERVICE_PORT, log=$log"
  else
    echo "Failed to start $service. Check log: $log" >&2
    exit 1
  fi
}

stop_service() {
  service="$1"
  service_config "$service"
  if ! pid=$(running_pid "$service"); then
    echo "$service is not running"
    return 0
  fi

  kill "$pid"
  timeout=${STOP_TIMEOUT:-30}
  i=0
  while [ "$i" -lt "$timeout" ]; do
    if ! is_pid_alive "$pid"; then
      rm -f "$(pid_file "$service")"
      echo "Stopped $service"
      return 0
    fi
    i=$((i + 1))
    sleep 1
  done

  if pid_matches_service "$pid" "$service"; then
    kill -9 "$pid" >/dev/null 2>&1 || true
    rm -f "$(pid_file "$service")"
    echo "Stopped $service with SIGKILL after ${timeout}s"
  else
    echo "Refused to force stop pid=$pid because it no longer matches $service" >&2
    exit 1
  fi
}

status_service() {
  service="$1"
  service_config "$service"
  if pid=$(running_pid "$service"); then
    echo "$service RUNNING pid=$pid port=$SERVICE_PORT"
  else
    echo "$service STOPPED port=$SERVICE_PORT"
  fi
}

restart_service() {
  service="$1"
  stop_service "$service"
  start_service "$service"
}

target_services() {
  target="${1:-all}"
  if [ "$target" = "all" ] || [ -z "$target" ]; then
    printf '%s\n' $SERVICES
  else
    service_config "$target" >/dev/null
    printf '%s\n' "$target"
  fi
}

target_stop_services() {
  target="${1:-all}"
  if [ "$target" = "all" ] || [ -z "$target" ]; then
    printf '%s\n' $STOP_SERVICES
  else
    service_config "$target" >/dev/null
    printf '%s\n' "$target"
  fi
}

logs_target() {
  target="${1:-all}"
  shift || true
  lines=200
  follow=false
  while [ "$#" -gt 0 ]; do
    case "$1" in
      --tail)
        lines="${2:-200}"
        shift 2
        ;;
      -f|--follow)
        follow=true
        shift
        ;;
      *)
        echo "Unknown logs option: $1" >&2
        exit 1
        ;;
    esac
  done

  if [ "$target" = "all" ] || [ -z "$target" ]; then
    files=""
    for service in $SERVICES; do
      file=$(log_file "$service")
      [ -f "$file" ] && files="$files $file"
    done
    if [ -z "$files" ]; then
      echo "No log files found under $LOG_DIR"
      return 0
    fi
    if [ "$follow" = "true" ]; then
      tail -n "$lines" -f $files
    else
      tail -n "$lines" $files
    fi
    return 0
  fi

  service_config "$target"
  file=$(log_file "$target")
  if [ ! -f "$file" ]; then
    echo "Log file not found: $file"
    return 0
  fi
  if [ "$follow" = "true" ]; then
    tail -n "$lines" -f "$file"
  else
    tail -n "$lines" "$file"
  fi
}

run_for_each() {
  action="$1"
  target="${2:-all}"
  if [ "$action" = "stop" ]; then
    services=$(target_stop_services "$target")
  else
    services=$(target_services "$target")
  fi
  for service in $services; do
    "${action}_service" "$service"
  done
}

command="${1:-}"
target="${2:-all}"

case "$command" in
  init)
    init_all
    ;;
  start)
    load_env
    ensure_dirs
    need_cmd "${JAVA_BIN:-java}"
    check_java_runtime
    run_for_each start "$target"
    ;;
  stop)
    load_env
    ensure_dirs
    run_for_each stop "$target"
    ;;
  restart)
    load_env
    ensure_dirs
    if [ "$target" = "all" ] || [ -z "$target" ]; then
      run_for_each stop all
      run_for_each start all
    else
      restart_service "$target"
    fi
    ;;
  status)
    load_env
    ensure_dirs
    run_for_each status "$target"
    ;;
  logs)
    load_env
    ensure_dirs
    shift || true
    logs_target "$@"
    ;;
  *)
    usage
    exit 1
    ;;
esac
