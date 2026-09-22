#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a

DEVOPS="$ROOT/compose/devops/docker-compose.yml"

ensure_protected_service() {
  service=$1
  container=$2
  if docker inspect "$container" >/dev/null 2>&1; then
    state=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true)
    if [ "$state" != "running" ]; then
      docker start "$container" >/dev/null
      echo "[start] started protected DevOps container: $container"
    else
      echo "[start] preserved protected DevOps container: $container"
    fi
    return 0
  fi
  echo "[start] creating missing DevOps service: $service"
  docker compose --env-file "$ENV_FILE" -f "$DEVOPS" up -d --no-deps "$service"
}

connect_network() {
  network=$1
  container=$2
  docker inspect "$container" >/dev/null 2>&1 || return 0
  if docker network inspect "$network" -f '{{range .Containers}}{{println .Name}}{{end}}' | grep -Fxq "$container"; then
    return 0
  fi
  docker network connect "$network" "$container" >/dev/null
  echo "[start] connected $container to $network"
}

ensure_protected_service registry local-registry
ensure_protected_service registry-ui registry-ui
ensure_protected_service gitlab gitlab
ensure_protected_service nexus nexus
ensure_protected_service jenkins jenkins
ensure_protected_service nginx peach-devops-nginx

# Runtime middleware and observability services are always reconciled from the
# current Compose definitions. External volumes remain unchanged.
"$SCRIPT_DIR/start-runtime.sh"

connect_network peach-devops jenkins
connect_network peach-cloud-runtime jenkins
connect_network peach-devops peach-devops-nginx
connect_network peach-cloud-runtime peach-devops-nginx

echo "Infrastructure startup completed: protected DevOps containers were preserved and Runtime services were reconciled from current Compose definitions."
