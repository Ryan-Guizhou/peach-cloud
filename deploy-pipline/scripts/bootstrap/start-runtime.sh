#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
[ -s "$ENV_FILE" ] || { echo "Missing runtime env: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a

MIDDLEWARE="$ROOT/compose/middleware/docker-compose.yml"
OBSERVABILITY="$ROOT/compose/observability/docker-compose.yml"

connect_network() {
  network=$1
  container=$2
  docker inspect "$container" >/dev/null 2>&1 || return 0
  if docker network inspect "$network" -f '{{range .Containers}}{{println .Name}}{{end}}' | grep -Fxq "$container"; then
    return 0
  fi
  docker network connect "$network" "$container" >/dev/null
  echo "[runtime] connected $container to $network"
}

has_compose_identity() {
  container=$1
  expected_project=$2
  expected_service=$3
  current_project=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.project"}}' "$container" 2>/dev/null || true)
  current_service=$(docker inspect -f '{{index .Config.Labels "com.docker.compose.service"}}' "$container" 2>/dev/null || true)
  [ "$current_project" = "$expected_project" ] && [ "$current_service" = "$expected_service" ]
}

remove_legacy_container() {
  container=$1
  expected_project=$2
  expected_service=$3
  docker inspect "$container" >/dev/null 2>&1 || return 1
  if has_compose_identity "$container" "$expected_project" "$expected_service"; then
    return 1
  fi
  echo "[runtime] replacing legacy runtime container while preserving external data: $container"
  docker rm -f "$container" >/dev/null
  return 0
}

reconcile_service() {
  compose_file=$1
  expected_project=$2
  service=$3
  container=$4

  remove_legacy_container "$container" "$expected_project" "$service" || true
  docker compose --env-file "$ENV_FILE" -f "$compose_file" up -d --no-deps "$service"
  state=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true)
  [ "$state" = "running" ] || { echo "Runtime container did not start: $container (status=${state:-missing})" >&2; exit 1; }
  echo "[runtime] reconciled Compose definition: $container"
}

connect_network peach-cloud-runtime jenkins

reconcile_service "$MIDDLEWARE" peach-middleware-runtime mysql peach-mysql
reconcile_service "$MIDDLEWARE" peach-middleware-runtime redis peach-redis
reconcile_service "$MIDDLEWARE" peach-middleware-runtime nacos peach-nacos
reconcile_service "$MIDDLEWARE" peach-middleware-runtime mongodb peach-mongo
reconcile_service "$MIDDLEWARE" peach-middleware-runtime rocketmq-namesrv peach-rocketmq-namesrv

prepare_rocketmq_store=false
if ! docker inspect peach-rocketmq-broker >/dev/null 2>&1; then
  prepare_rocketmq_store=true
elif remove_legacy_container peach-rocketmq-broker peach-middleware-runtime rocketmq-broker; then
  prepare_rocketmq_store=true
fi
if [ "$prepare_rocketmq_store" = "true" ]; then
  docker run --rm --user 0:0 \
    -v peach-rocketmq-store:/home/rocketmq/store \
    "apache/rocketmq:${ROCKETMQ_VERSION:-5.3.1}" \
    sh -c 'chown -R 3000:3000 /home/rocketmq/store'
fi
reconcile_service "$MIDDLEWARE" peach-middleware-runtime rocketmq-broker peach-rocketmq-broker
reconcile_service "$MIDDLEWARE" peach-middleware-runtime rocketmq-dashboard peach-rocketmq-dashboard

for container in peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker peach-rocketmq-dashboard; do
  connect_network peach-cloud-runtime "$container"
done

reconcile_service "$OBSERVABILITY" peach-observability prometheus peach-prometheus
reconcile_service "$OBSERVABILITY" peach-observability tempo peach-tempo
reconcile_service "$OBSERVABILITY" peach-observability otel-collector peach-otel-collector
reconcile_service "$OBSERVABILITY" peach-observability loki peach-loki
reconcile_service "$OBSERVABILITY" peach-observability alloy peach-alloy
reconcile_service "$OBSERVABILITY" peach-observability grafana peach-grafana

for container in peach-prometheus peach-tempo peach-otel-collector peach-loki peach-alloy peach-grafana; do
  connect_network peach-devops "$container"
done
for container in peach-prometheus peach-otel-collector; do
  connect_network peach-cloud-runtime "$container"
done

echo "Runtime containers reconciled from current Compose definitions without recreating protected DevOps services."
