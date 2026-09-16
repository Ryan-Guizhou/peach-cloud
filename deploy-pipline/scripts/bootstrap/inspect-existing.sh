#!/usr/bin/env sh
set -eu
containers="local-registry registry-ui gitlab nexus jenkins peach-devops-nginx peach-mysql peach-redis peach-nacos peach-mongo peach-rocketmq-namesrv peach-rocketmq-broker peach-prometheus peach-tempo peach-otel-collector peach-loki peach-alloy peach-grafana"
volumes="peach-registry-data peach-gitlab-config peach-gitlab-data peach-jenkins-data peach-nexus-data peach-mysql-data peach-redis-data peach-nacos-data peach-mongo-data peach-rocketmq-store peach-prometheus-data peach-tempo-data peach-loki-data peach-alloy-data peach-grafana-data"
networks="peach-devops peach-cloud-runtime"
printf '%s\n' '=== Containers ==='
for item in $containers; do
  if docker inspect "$item" >/dev/null 2>&1; then state=$(docker inspect -f '{{.State.Status}}' "$item"); echo "[FOUND] $item ($state)"; else echo "[MISS ] $item"; fi
done
printf '%s\n' '=== Volumes ==='
for item in $volumes; do if docker volume inspect "$item" >/dev/null 2>&1; then echo "[FOUND] $item"; else echo "[MISS ] $item"; fi; done
printf '%s\n' '=== Networks ==='
for item in $networks; do if docker network inspect "$item" >/dev/null 2>&1; then echo "[FOUND] $item"; else echo "[MISS ] $item"; fi; done
