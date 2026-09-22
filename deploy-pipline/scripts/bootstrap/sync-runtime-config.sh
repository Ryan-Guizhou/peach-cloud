#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
[ -s "$ENV_FILE" ] || { echo "Missing runtime env: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
: "${PEACH_RUNTIME_ROOT:?PEACH_RUNTIME_ROOT is required}"
CONFIG_SYNC_IMAGE=${CONFIG_SYNC_IMAGE:-nginx:${NGINX_VERSION:-1.25-alpine}}
echo "[runtime-config] synchronizing repository config into Docker-host-visible runtime root"
tar -C "$ROOT/config" -cf - . | docker run --rm -i --entrypoint /bin/sh -v "$PEACH_RUNTIME_ROOT:/runtime" "$CONFIG_SYNC_IMAGE" -c 'set -eu; mkdir -p /runtime/config; tar -C /runtime/config -xf -'
echo "[runtime-config] synchronized to $PEACH_RUNTIME_ROOT/config"
