#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
wait_container() {
  container=$1; attempts=${2:-60}; i=0
  while [ "$i" -lt "$attempts" ]; do
    if docker inspect "$container" >/dev/null 2>&1; then
      status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
      if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then echo "[OK] $container ($status)"; return 0; fi
    fi
    i=$((i + 1)); sleep 2
  done
  echo "[FAIL] $container is not ready" >&2; return 1
}
wait_container local-registry 90
wait_container nexus 90
"$SCRIPT_DIR/verify-middleware.sh"
docker network inspect peach-devops >/dev/null
docker network inspect peach-cloud-runtime >/dev/null
if docker inspect jenkins >/dev/null 2>&1; then
  docker exec jenkins curl -fsS http://nexus:8081/service/rest/v1/status >/dev/null || { echo "[FAIL] Jenkins cannot reach Nexus" >&2; exit 1; }
  docker exec jenkins curl -fsS http://registry:5000/v2/ >/dev/null || { echo "[FAIL] Jenkins cannot reach Registry" >&2; exit 1; }
fi
echo "Critical infrastructure verification passed."
