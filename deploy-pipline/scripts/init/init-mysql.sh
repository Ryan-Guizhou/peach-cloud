#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
SCHEMA_FILE="$ROOT/init/mysql/schema/ALL_TABLE_CREATE.sql"
DATA_FILE="$ROOT/init/mysql/data/INIT.sql"
[ -f "$SCHEMA_FILE" ] || { echo "Missing MySQL schema: $SCHEMA_FILE" >&2; exit 1; }
[ -f "$DATA_FILE" ] || { echo "Missing MySQL seed data: $DATA_FILE" >&2; exit 1; }
count=$(docker exec peach-mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -Nse "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE();"')
if [ "$count" != "0" ]; then echo "[mysql-init] existing schema detected ($count tables); preserving database and skipping baseline initialization."; exit 0; fi
echo "[mysql-init] empty database detected; applying baseline schema."
docker exec -i peach-mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$SCHEMA_FILE"
echo "[mysql-init] applying baseline seed data."
docker exec -i peach-mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < "$DATA_FILE"
echo "[mysql-init] baseline initialization completed."
