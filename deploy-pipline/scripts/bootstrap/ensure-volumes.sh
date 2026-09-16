#!/usr/bin/env sh
set -eu
volumes="peach-registry-data peach-gitlab-config peach-gitlab-data peach-jenkins-data peach-nexus-data peach-mysql-data peach-redis-data peach-nacos-data peach-mongo-data peach-rocketmq-store peach-prometheus-data peach-tempo-data peach-loki-data peach-alloy-data peach-grafana-data peach-gateway-config peach-auth-config peach-monitor-config peach-fileservice-config peach-fileservice-upload peach-message-config peach-setting-config peach-generator-config peach-scheduled-config"
for volume in $volumes; do
  if docker volume inspect "$volume" >/dev/null 2>&1; then
    echo "[volume] exists: $volume"
  else
    docker volume create "$volume" >/dev/null
    echo "[volume] created: $volume"
  fi
done
