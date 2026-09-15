#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd); ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd); ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}; tag=${PEACH_IMAGE_TAG:?Set PEACH_IMAGE_TAG}; services=${SELECTED_SERVICES:?Set SELECTED_SERVICES}
set -a; . "$ENV_FILE"; set +a; export PEACH_IMAGE_TAG="$tag"; export PEACH_IMAGE_PREFIX=${IMAGE_PREFIX:-${PEACH_IMAGE_PREFIX:-localhost:5000/peach-cloud}}
compose="$ROOT/compose/application/docker-compose.yml"; docker compose --env-file "$ENV_FILE" -f "$compose" pull $services
if [ "${STOP_UNSELECTED:-false}" = "true" ] && [ -n "${UNSELECTED_SERVICES:-}" ]; then docker compose --env-file "$ENV_FILE" -f "$compose" stop $UNSELECTED_SERVICES; fi
docker compose --env-file "$ENV_FILE" -f "$compose" up -d --no-build --no-deps $services; docker compose --env-file "$ENV_FILE" -f "$compose" ps
