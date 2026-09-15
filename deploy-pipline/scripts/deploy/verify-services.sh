#!/usr/bin/env sh
set -eu
services=${SELECTED_SERVICES:?Set SELECTED_SERVICES}
port_for() { case "$1" in peach-gateway) echo "${GATEWAY_PORT:-18080}" ;; peach-auth) echo "${AUTH_PORT:-18081}" ;; peach-monitor) echo "${MONITOR_PORT:-18082}" ;; peach-fileservice) echo "${FILESERVICE_PORT:-18083}" ;; peach-message) echo "${MESSAGE_PORT:-18084}" ;; peach-setting) echo "${SETTING_PORT:-18085}" ;; peach-generator) echo "${GENERATOR_PORT:-18086}" ;; peach-scheduled) echo "${SCHEDULED_PORT:-18087}" ;; peach-front) echo "80" ;; *) return 1 ;; esac; }
for service in $services; do
  status=$(docker inspect -f '{{.State.Status}}' "$service" 2>/dev/null || true); [ "$status" = "running" ] || { echo "[FAIL] $service container status: ${status:-missing}" >&2; exit 1; }; echo "[OK] $service container is running"
  if [ "$service" != "peach-front" ] && command -v getent >/dev/null 2>&1 && getent hosts "$service" >/dev/null 2>&1; then
    port=$(port_for "$service"); i=0; until [ "$i" -ge 60 ]; do if curl -fsS "http://$service:$port/actuator/health" | grep -q '"status"[[:space:]]*:[[:space:]]*"UP"'; then echo "[OK] $service actuator health is UP"; break; fi; i=$((i + 1)); sleep 2; done; [ "$i" -lt 60 ] || { echo "[FAIL] $service actuator health did not become UP" >&2; exit 1; }
  fi
done
