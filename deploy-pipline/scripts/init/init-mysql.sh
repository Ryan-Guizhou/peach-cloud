#!/usr/bin/env sh
set -eu
umask 077

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a

SCHEMA_FILE="$ROOT/init/mysql/schema/ALL_TABLE_CREATE.sql"
DATA_FILE="$ROOT/init/mysql/data/INIT.sql"
RENDERER="$ROOT/scripts/init/render-mysql-baseline.mjs"
[ -f "$SCHEMA_FILE" ] || { echo "Missing MySQL schema: $SCHEMA_FILE" >&2; exit 1; }
[ -f "$DATA_FILE" ] || { echo "Missing MySQL seed data: $DATA_FILE" >&2; exit 1; }
[ -f "$RENDERER" ] || { echo "Missing MySQL baseline renderer: $RENDERER" >&2; exit 1; }

mkdir -p "$ROOT/runtime/generated"
chmod 700 "$ROOT/runtime/generated"
TMP_DIR=$(mktemp -d "$ROOT/runtime/generated/mysql-init.XXXXXX")
cleanup() { rm -rf "$TMP_DIR"; }
trap cleanup EXIT HUP INT TERM

EXISTING_TABLES="$TMP_DIR/existing-tables.txt"
MISSING_SCHEMA="$TMP_DIR/missing-schema.sql"
METADATA="$TMP_DIR/metadata.json"
VERIFY_SCHEMA="$TMP_DIR/verify-schema.sql"
VERIFY_METADATA="$TMP_DIR/verify-metadata.json"
BASELINE_VERSION=baseline-v1

mysql_command() {
  docker exec -i peach-mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
}

mysql_query() {
  query=$1
  printf '%s\n' "$query" | docker exec -i peach-mysql sh -c 'mysql --default-character-set=utf8mb4 -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE" -N'
}

list_tables() {
  mysql_query 'SELECT UPPER(table_name) FROM information_schema.tables WHERE table_schema = DATABASE() ORDER BY table_name;'
}

metadata_value() {
  key=$1
  node -e 'const fs=require("fs");const data=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));const value=data[process.argv[2]];process.stdout.write(Array.isArray(value)?String(value.length):String(value));' "$METADATA" "$key"
}

mysql_query 'CREATE TABLE IF NOT EXISTS PEACH_DEPLOY_SCHEMA_HISTORY (
  VERSION VARCHAR(64) NOT NULL,
  CHECKSUM CHAR(64) NOT NULL,
  APPLIED_AT TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (VERSION)
) COMMENT="Peach deployment schema history";'

list_tables > "$EXISTING_TABLES"
node "$RENDERER" \
  --schema "$SCHEMA_FILE" \
  --existing "$EXISTING_TABLES" \
  --output "$MISSING_SCHEMA" \
  --metadata "$METADATA"

missing_count=$(metadata_value missingTables)
checksum=$(metadata_value checksum)
version=$(metadata_value version)
case "$version" in *[!A-Za-z0-9._-]*) echo "Unsafe baseline version: $version" >&2; exit 2 ;; esac
case "$checksum" in *[!a-f0-9]*) echo "Unsafe baseline checksum: $checksum" >&2; exit 2 ;; esac

if [ "$missing_count" -gt 0 ]; then
  echo "[mysql-init] recovering $missing_count missing baseline table segment(s)."
  mysql_command < "$MISSING_SCHEMA"
else
  echo "[mysql-init] all expected baseline tables already exist."
fi

list_tables > "$EXISTING_TABLES"
node "$RENDERER" \
  --schema "$SCHEMA_FILE" \
  --existing "$EXISTING_TABLES" \
  --output "$VERIFY_SCHEMA" \
  --metadata "$VERIFY_METADATA"
remaining_missing=$(node -e 'const fs=require("fs");const data=JSON.parse(fs.readFileSync(process.argv[1],"utf8"));process.stdout.write(String(data.missingTables.length));' "$VERIFY_METADATA")
[ "$remaining_missing" -eq 0 ] || { echo "[mysql-init] expected schema verification failed; $remaining_missing table(s) are still missing." >&2; exit 1; }

marker_count=$(mysql_query "SELECT COUNT(*) FROM PEACH_DEPLOY_SCHEMA_HISTORY WHERE VERSION = '$BASELINE_VERSION';")
seed_ready=$(mysql_query "SELECT IF(
  EXISTS (SELECT 1 FROM PEACH_TENANT WHERE TENANT_CODE = 'DEFAULT')
  AND EXISTS (SELECT 1 FROM PEACH_APPLICATION WHERE APP_ID = 'f73b300578a5436d82ec7fca2c07c284')
  AND EXISTS (SELECT 1 FROM PEACH_USER WHERE USER_CODE = 'admin'),
  1,
  0
);")

if [ "$marker_count" -eq 0 ] || [ "$missing_count" -gt 0 ] || [ "$seed_ready" != "1" ]; then
  echo "[mysql-init] applying idempotent baseline seed data."
  mysql_command < "$DATA_FILE"
else
  echo "[mysql-init] baseline completion marker and critical seed data are present."
fi

seed_ready=$(mysql_query "SELECT IF(
  EXISTS (SELECT 1 FROM PEACH_TENANT WHERE TENANT_CODE = 'DEFAULT')
  AND EXISTS (SELECT 1 FROM PEACH_APPLICATION WHERE APP_ID = 'f73b300578a5436d82ec7fca2c07c284')
  AND EXISTS (SELECT 1 FROM PEACH_USER WHERE USER_CODE = 'admin'),
  1,
  0
);")
[ "$seed_ready" = "1" ] || { echo "[mysql-init] critical baseline seed verification failed." >&2; exit 1; }

mysql_query "INSERT INTO PEACH_DEPLOY_SCHEMA_HISTORY (VERSION, CHECKSUM, APPLIED_AT)
VALUES ('$version', '$checksum', CURRENT_TIMESTAMP)
ON DUPLICATE KEY UPDATE CHECKSUM = VALUES(CHECKSUM), APPLIED_AT = VALUES(APPLIED_AT);"

echo "[mysql-init] baseline schema and seed verification completed."
