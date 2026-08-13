#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
REPO_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
COMPOSE_FILE="$SCRIPT_DIR/docker-compose.yml"
ENV_FILE="$SCRIPT_DIR/.env"

cd "$SCRIPT_DIR"

compose() {
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" "$@"
}

ensure_env() {
  if [ ! -f "$ENV_FILE" ]; then
    cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
    echo "Created deploy/.env from .env.example. Edit passwords and ports before production use."
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
    "$SCRIPT_DIR/runtime/data/mysql" \
    "$SCRIPT_DIR/runtime/data/redis" \
    "$SCRIPT_DIR/runtime/data/nacos" \
    "$SCRIPT_DIR/runtime/logs/mysql" \
    "$SCRIPT_DIR/runtime/logs/redis" \
    "$SCRIPT_DIR/runtime/logs/nacos" \
    "$SCRIPT_DIR/runtime/logs/nginx" \
    "$SCRIPT_DIR/runtime/logs/peach-gateway" \
    "$SCRIPT_DIR/runtime/logs/peach-auth" \
    "$SCRIPT_DIR/runtime/logs/peach-monitor" \
    "$SCRIPT_DIR/runtime/logs/peach-fileservice" \
    "$SCRIPT_DIR/runtime/logs/peach-message" \
    "$SCRIPT_DIR/runtime/logs/peach-setting" \
    "$SCRIPT_DIR/runtime/logs/peach-generator" \
    "$SCRIPT_DIR/runtime/upload/peach-fileservice" \
    "$SCRIPT_DIR/runtime/config/services/peach-gateway" \
    "$SCRIPT_DIR/runtime/config/services/peach-auth" \
    "$SCRIPT_DIR/runtime/config/services/peach-monitor" \
    "$SCRIPT_DIR/runtime/config/services/peach-fileservice" \
    "$SCRIPT_DIR/runtime/config/services/peach-message" \
    "$SCRIPT_DIR/runtime/config/services/peach-setting" \
    "$SCRIPT_DIR/runtime/config/services/peach-generator" \
    "$SCRIPT_DIR/runtime/build/front-dist" \
    "$SCRIPT_DIR/runtime/nacos"
}

need_cmd() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing command: $1" >&2
    exit 1
  fi
}

check_tools() {
  need_cmd docker
  docker compose version >/dev/null
}

wait_container_healthy() {
  name="$1"
  i=0
  while [ "$i" -lt 90 ]; do
    status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$name" 2>/dev/null || true)
    if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
      return 0
    fi
    i=$((i + 1))
    sleep 2
  done
  echo "Container is not healthy: $name" >&2
  exit 1
}

check_mysql_case_insensitive() {
  load_env
  value=$(compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" -Nse "SHOW VARIABLES LIKE '\''lower_case_table_names'\'';"' | awk '{print $2}')
  if [ "$value" != "1" ]; then
    echo "MySQL lower_case_table_names must be 1, actual: $value" >&2
    echo "This variable must be set before MySQL data directory initialization." >&2
    exit 1
  fi
}

mysql_seed_count() {
  compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT MIN(seed_count) FROM (SELECT COUNT(*) seed_count FROM PEACH_TENANT UNION ALL SELECT COUNT(*) FROM PEACH_APPLICATION UNION ALL SELECT COUNT(*) FROM PEACH_USER UNION ALL SELECT COUNT(*) FROM PEACH_MENU) seed_check;"'
}

import_mysql_seed_data() {
  echo "Importing MySQL seed data from sql/INIT.sql..."
  compose exec -T mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" < /docker-entrypoint-initdb.d/01_INIT.sql'
}

ensure_mysql_seed_data() {
  load_env
  seed_count=$(mysql_seed_count)
  if [ "${seed_count:-0}" -lt 1 ]; then
    import_mysql_seed_data
    seed_count=$(mysql_seed_count)
  fi
  if [ "${seed_count:-0}" -lt 1 ]; then
    echo "MySQL seed data is incomplete. Check sql/INIT.sql and MySQL logs." >&2
    exit 1
  fi
}

backend_build() {
  cd "$REPO_DIR"
  ./mvnw -DskipTests package 2>/dev/null || mvn -DskipTests package
  cd "$SCRIPT_DIR"
  compose build peach-gateway peach-auth peach-monitor peach-fileservice peach-message peach-setting peach-generator
}

front_build() {
  if [ ! -d "$REPO_DIR/peach-cloud-front" ]; then
    echo "peach-cloud-front not found. The peach-front image cannot be built." >&2
    exit 1
  fi
  cd "$REPO_DIR/peach-cloud-front"
  npm ci
  npm run build
  rm -rf "$SCRIPT_DIR/runtime/build/front-dist"
  mkdir -p "$SCRIPT_DIR/runtime/build/front-dist"
  cp -R dist/. "$SCRIPT_DIR/runtime/build/front-dist/"
  cd "$SCRIPT_DIR"
  compose build peach-front
}

init_all() {
  ensure_env
  ensure_dirs
  check_tools
  chmod +x "$SCRIPT_DIR/nacos/import-nacos.sh" 2>/dev/null || true
  echo "Deploy workspace initialized."
}

up_all() {
  load_env
  ensure_dirs
  check_tools
  compose up -d mysql redis nacos
  wait_container_healthy peach-mysql
  wait_container_healthy peach-redis
  wait_container_healthy peach-nacos
  check_mysql_case_insensitive
  ensure_mysql_seed_data
  "$SCRIPT_DIR/nacos/import-nacos.sh"
  compose up -d peach-gateway peach-auth peach-monitor peach-fileservice peach-message peach-setting peach-generator peach-front
}

down_all() {
  load_env
  compose down
}

restart_target() {
  load_env
  target="${1:-}"
  if [ "$target" = "nginx" ]; then
    target="peach-front"
  fi
  if [ "$target" = "" ]; then
    compose restart
  else
    compose restart "$target"
  fi
}

logs_target() {
  load_env
  target="${1:-}"
  if [ "$target" = "nginx" ]; then
    target="peach-front"
  fi
  if [ "$target" = "" ]; then
    compose logs -f --tail=200
  else
    compose logs -f --tail=200 "$target"
  fi
}

health_all() {
  load_env
  compose ps
  check_mysql_case_insensitive
  ensure_mysql_seed_data
  "$SCRIPT_DIR/nacos/import-nacos.sh" >/dev/null
  echo "Health check completed."
}

clean_logs() {
  find "$SCRIPT_DIR/runtime/logs" -type f -name "*.log" -delete 2>/dev/null || true
  echo "Logs cleaned."
}

clean_data() {
  echo "This will remove deploy/runtime/data and deploy/runtime/upload. Type DELETE to continue:"
  read answer
  if [ "$answer" != "DELETE" ]; then
    echo "Canceled."
    exit 1
  fi
  rm -rf "$SCRIPT_DIR/runtime/data" "$SCRIPT_DIR/runtime/upload"
  echo "Runtime data removed."
}

usage() {
  cat <<'USAGE'
Usage: ./peach.sh <command> [service]

Commands:
  init             Create .env and runtime directories
  build            Build backend jars and service images
  front:build      Build frontend dist and peach-front image
  up               Start mysql/redis/nacos, import Nacos config, start services and peach-front
  down             Stop containers without deleting data
  restart [svc]    Restart all containers or one service
  logs [svc]       Follow all logs or one service
  ps               Show container status
  health           Check compose status, MySQL case mode, and Nacos config import
  mysql:init       Import idempotent MySQL seed data from sql/INIT.sql
  nacos:import     Import Nacos config templates
  nginx:reload     Reload peach-front nginx config
  clean:logs       Delete log files under runtime/logs
  clean:data       Delete runtime data after explicit confirmation
USAGE
}

case "${1:-}" in
  init) init_all ;;
  build) load_env; ensure_dirs; check_tools; backend_build ;;
  front:build) ensure_dirs; front_build ;;
  up) up_all ;;
  down) down_all ;;
  restart) restart_target "${2:-}" ;;
  logs) logs_target "${2:-}" ;;
  ps) load_env; compose ps ;;
  health) health_all ;;
  mysql:init) load_env; import_mysql_seed_data; ensure_mysql_seed_data ;;
  nacos:import) load_env; "$SCRIPT_DIR/nacos/import-nacos.sh" ;;
  nginx:reload) load_env; compose exec peach-front nginx -s reload ;;
  clean:logs) clean_logs ;;
  clean:data) clean_data ;;
  *) usage; exit 1 ;;
esac
