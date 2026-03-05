#!/usr/bin/env sh
set -eu

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose.yml}"

if [ ! -f "$COMPOSE_FILE" ]; then
  echo "docker compose file not found: $COMPOSE_FILE"
  exit 1
fi

ACTION="${1:-up}"

case "$ACTION" in
  up)
    docker compose -f "$COMPOSE_FILE" up -d --build
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down
    ;;
  restart)
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d --build
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f --tail=200
    ;;
  ps)
    docker compose -f "$COMPOSE_FILE" ps
    ;;
  build)
    docker compose -f "$COMPOSE_FILE" build
    ;;
  *)
    echo "usage: sh bin/start.sh [up|down|restart|logs|ps|build]"
    exit 1
    ;;
esac
