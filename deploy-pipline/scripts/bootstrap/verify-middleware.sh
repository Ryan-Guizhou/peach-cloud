#!/usr/bin/env sh
set -eu
wait_container() {
  container=$1; attempts=${2:-90}; i=0
  while [ "$i" -lt "$attempts" ]; do
    if docker inspect "$container" >/dev/null 2>&1; then
      status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
      if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then echo "[OK] $container ($status)"; return 0; fi
    fi
    i=$((i + 1)); sleep 2
  done
  echo "[FAIL] $container is not ready" >&2; return 1
}
for container in peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker; do wait_container "$container"; done
