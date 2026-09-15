#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
export PEACH_ENV_FILE="$ENV_FILE"
"$SCRIPT_DIR/inspect-existing.sh"
"$SCRIPT_DIR/ensure-networks.sh"
"$SCRIPT_DIR/ensure-volumes.sh"
"$SCRIPT_DIR/start.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
"$ROOT/scripts/init/init-mysql.sh"
"$ROOT/scripts/init/init-mongodb.sh"
"$ROOT/scripts/init/init-nacos.sh"
"$SCRIPT_DIR/verify-infrastructure.sh"
echo "Peach infrastructure bootstrap completed."
