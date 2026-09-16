#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
PRIVATE_ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
RUNTIME_ENV_FILE="$ROOT/runtime/generated/deploy.env"
node "$ROOT/scripts/config/render-runtime-env.mjs" --secrets "$PRIVATE_ENV_FILE" --output "$RUNTIME_ENV_FILE"
export PEACH_ENV_FILE="$RUNTIME_ENV_FILE"
"$SCRIPT_DIR/inspect-existing.sh"
"$SCRIPT_DIR/ensure-networks.sh"
"$SCRIPT_DIR/ensure-volumes.sh"
"$SCRIPT_DIR/sync-runtime-config.sh"
"$SCRIPT_DIR/start.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
"$ROOT/scripts/init/init-mysql.sh"
"$ROOT/scripts/init/ensure-mongodb-user.sh"
"$ROOT/scripts/init/init-nacos.sh"
"$ROOT/scripts/init/reconcile-rocketmq-topics.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
echo "Peach infrastructure bootstrap completed. Existing containers and protected volumes were preserved when present."
