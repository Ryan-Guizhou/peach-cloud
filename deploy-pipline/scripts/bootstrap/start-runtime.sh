#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
[ -s "$ENV_FILE" ] || { echo "Missing runtime env: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
MIDDLEWARE="$ROOT/compose/middleware/docker-compose.yml"
OBSERVABILITY="$ROOT/compose/observability/docker-compose.yml"
connect_network() { network=$1; container=$2; docker inspect "$container" >/dev/null 2>&1 || return 0; if docker network inspect "$network" -f '{{range .Containers}}{{println .Name}}{{end}}' 2>/dev/null | grep -Fxq "$container"; then return 0; fi; docker network connect "$network" "$container" >/dev/null 2>&1 || true; echo "[runtime] connected $container to $network"; }
ensure_service() { compose_file=$1; service=$2; container=$3; if docker inspect "$container" >/dev/null 2>&1; then state=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true); if [ "$state" != "running" ]; then docker start "$container" >/dev/null; echo "[runtime] started existing container: $container"; else echo "[runtime] preserved existing container: $container"; fi; return 0; fi; echo "[runtime] creating missing service: $service"; docker compose --env-file "$ENV_FILE" -f "$compose_file" up -d --no-deps "$service"; }
connect_network peach-cloud-runtime jenkins
ensure_service "$MIDDLEWARE" mysql peach-mysql
ensure_service "$MIDDLEWARE" redis peach-redis
ensure_service "$MIDDLEWARE" nacos peach-nacos
ensure_service "$MIDDLEWARE" mongodb peach-mongo
ensure_service "$MIDDLEWARE" rocketmq-namesrv peach-rocketmq-namesrv
if ! docker inspect peach-rocketmq-broker >/dev/null 2>&1; then docker run --rm --user 0:0 -v peach-rocketmq-store:/home/rocketmq/store "apache/rocketmq:${ROCKETMQ_VERSION:-5.3.1}" sh -c 'chown -R 3000:3000 /home/rocketmq/store'; fi
ensure_service "$MIDDLEWARE" rocketmq-broker peach-rocketmq-broker
ensure_service "$MIDDLEWARE" rocketmq-dashboard peach-rocketmq-dashboard
for container in peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker peach-rocketmq-dashboard; do connect_network peach-cloud-runtime "$container"; done
ensure_service "$OBSERVABILITY" prometheus peach-prometheus
ensure_service "$OBSERVABILITY" tempo peach-tempo
ensure_service "$OBSERVABILITY" otel-collector peach-otel-collector
ensure_service "$OBSERVABILITY" loki peach-loki
ensure_service "$OBSERVABILITY" alloy peach-alloy
ensure_service "$OBSERVABILITY" grafana peach-grafana
for container in peach-prometheus peach-tempo peach-otel-collector peach-loki peach-alloy peach-grafana; do connect_network peach-devops "$container"; done
for container in peach-prometheus peach-otel-collector; do connect_network peach-cloud-runtime "$container"; done
echo "Runtime containers reconciled without recreating existing DevOps containers."
