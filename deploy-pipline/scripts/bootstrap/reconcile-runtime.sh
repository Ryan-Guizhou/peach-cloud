#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
export PEACH_ENV_FILE="$ENV_FILE"
[ -s "$ENV_FILE" ] || { echo "Missing runtime env: $ENV_FILE" >&2; exit 1; }
"$SCRIPT_DIR/ensure-networks.sh"
"$SCRIPT_DIR/ensure-volumes.sh"
"$SCRIPT_DIR/sync-runtime-config.sh"
"$SCRIPT_DIR/start-runtime.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
"$ROOT/scripts/init/init-mysql.sh"
"$ROOT/scripts/init/ensure-mongodb-user.sh"
"$ROOT/scripts/init/init-nacos.sh"
"$ROOT/scripts/init/reconcile-rocketmq-topics.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
echo "Peach runtime reconciliation completed. Existing DevOps containers were not recreated."
