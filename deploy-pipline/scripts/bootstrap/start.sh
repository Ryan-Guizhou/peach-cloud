#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
ensure_service() {
  compose_file=$1; service=$2; container=$3
  if docker inspect "$container" >/dev/null 2>&1; then
    state=$(docker inspect -f '{{.State.Status}}' "$container")
    if [ "$state" != "running" ]; then docker start "$container" >/dev/null; echo "[start] existing container: $container"; else echo "[start] already running: $container"; fi
  else
    echo "[start] create service: $service"
    docker compose --env-file "$ENV_FILE" -f "$compose_file" up -d "$service"
  fi
}
connect_network() {
  network=$1; container=$2
  docker inspect "$container" >/dev/null 2>&1 || return 0
  if docker network inspect "$network" -f '{{range .Containers}}{{println .Name}}{{end}}' | grep -Fxq "$container"; then return 0; fi
  docker network connect "$network" "$container" 2>/dev/null || true
}
DEVOPS="$ROOT/compose/devops/docker-compose.yml"
MIDDLEWARE="$ROOT/compose/middleware/docker-compose.yml"
OBS="$ROOT/compose/observability/docker-compose.yml"
ensure_service "$DEVOPS" registry local-registry
ensure_service "$DEVOPS" registry-ui registry-ui
ensure_service "$DEVOPS" gitlab gitlab
ensure_service "$DEVOPS" nexus nexus
ensure_service "$DEVOPS" jenkins jenkins
ensure_service "$DEVOPS" nginx peach-devops-nginx
ensure_service "$MIDDLEWARE" mysql peach-mysql
ensure_service "$MIDDLEWARE" redis peach-redis
ensure_service "$MIDDLEWARE" nacos peach-nacos
ensure_service "$MIDDLEWARE" mongodb peach-mongo
ensure_service "$MIDDLEWARE" rocketmq-namesrv peach-rocketmq-namesrv
ensure_service "$MIDDLEWARE" rocketmq-broker peach-rocketmq-broker
ensure_service "$OBS" prometheus peach-prometheus
ensure_service "$OBS" tempo peach-tempo
ensure_service "$OBS" otel-collector peach-otel-collector
ensure_service "$OBS" loki peach-loki
ensure_service "$OBS" alloy peach-alloy
ensure_service "$OBS" grafana peach-grafana
for container in jenkins peach-devops-nginx peach-prometheus peach-otel-collector; do connect_network peach-devops "$container"; connect_network peach-cloud-runtime "$container"; done
for container in peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker; do connect_network peach-cloud-runtime "$container"; done
echo "Infrastructure containers are started without recreating existing protected containers."
