#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}

[ -s "$ENV_FILE" ] || { echo "Missing deployment env: $ENV_FILE" >&2; exit 1; }
set -a
. "$ENV_FILE"
set +a

: "${PEACH_LOG_ROOT:?Set PEACH_LOG_ROOT}"
: "${MYSQL_ROOT_PASSWORD:?Set MYSQL_ROOT_PASSWORD}"
: "${REDIS_PASSWORD:?Set REDIS_PASSWORD}"
: "${MONGO_ROOT_PASSWORD:?Set MONGO_ROOT_PASSWORD}"
: "${MONGO_APP_PASSWORD:?Set MONGO_APP_PASSWORD}"
: "${PEACH_MONGO_URI:?Set PEACH_MONGO_URI}"
: "${MAVEN_NEXUS_URL:?Set MAVEN_NEXUS_URL}"
: "${MAVEN_NEXUS_USERNAME:?Set MAVEN_NEXUS_USERNAME}"
: "${MAVEN_NEXUS_PASSWORD:?Set MAVEN_NEXUS_PASSWORD}"

command -v docker >/dev/null 2>&1 || { echo "Docker CLI is required." >&2; exit 1; }
docker version >/dev/null
docker compose version >/dev/null
docker network inspect peach-devops >/dev/null
docker network inspect peach-cloud-runtime >/dev/null

"$ROOT/scripts/bootstrap/verify-infrastructure.sh"
echo "CI preflight passed."
