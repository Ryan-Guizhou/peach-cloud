#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
[ -s "$ENV_FILE" ] || { echo "Missing generated runtime env: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
command -v docker >/dev/null 2>&1 || { echo "Docker CLI is required." >&2; exit 1; }
docker version >/dev/null; docker compose version >/dev/null
docker network inspect peach-devops >/dev/null 2>&1 || { echo "Existing DevOps network peach-devops is required; Jenkins does not recreate DevOps infrastructure." >&2; exit 1; }
for container in local-registry nexus jenkins; do status=$(docker inspect -f '{{.State.Status}}' "$container" 2>/dev/null || true); [ "$status" = "running" ] || { echo "Existing DevOps container must already be running: $container (status=${status:-missing})" >&2; exit 1; }; echo "[preflight] preserved DevOps container: $container"; done
docker exec jenkins curl -fsS http://nexus:8081/service/rest/v1/status >/dev/null || { echo "Jenkins cannot reach existing Nexus." >&2; exit 1; }
docker exec jenkins curl -fsS http://registry:5000/v2/ >/dev/null || { echo "Jenkins cannot reach existing Registry." >&2; exit 1; }
echo "CI preflight passed without changing existing DevOps containers."
