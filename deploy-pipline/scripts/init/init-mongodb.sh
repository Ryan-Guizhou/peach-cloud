#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/env/deploy.env}
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
: "${MONGO_DATABASE:?Set MONGO_DATABASE}"
: "${MONGO_ROOT_USERNAME:?Set MONGO_ROOT_USERNAME}"
: "${MONGO_ROOT_PASSWORD:?Set MONGO_ROOT_PASSWORD}"
: "${MONGO_APP_USERNAME:?Set MONGO_APP_USERNAME}"
: "${MONGO_APP_PASSWORD:?Set MONGO_APP_PASSWORD}"
create_user_script='const d=db.getSiblingDB(process.env.APP_DB); const u=process.env.APP_USER; if (d.getUser(u) == null) { d.createUser({user:u,pwd:process.env.APP_PASSWORD,roles:[{role:"readWrite",db:process.env.APP_DB}]}); print("created"); } else { print("preserved"); }'
result=$(docker exec -e APP_DB="$MONGO_DATABASE" -e APP_USER="$MONGO_APP_USERNAME" -e APP_PASSWORD="$MONGO_APP_PASSWORD" peach-mongo mongosh --quiet --username "$MONGO_ROOT_USERNAME" --password "$MONGO_ROOT_PASSWORD" --authenticationDatabase admin --eval "$create_user_script")
echo "[mongo-init] application user: $result"
run_scripts() {
  directory=$1; label=$2; found=false
  for file in "$directory"/*.js; do
    [ -f "$file" ] || continue; found=true; echo "[mongo-init] applying $label: $(basename "$file")"
    docker exec -i peach-mongo mongosh --quiet --username "$MONGO_APP_USERNAME" --password "$MONGO_APP_PASSWORD" --authenticationDatabase "$MONGO_DATABASE" "$MONGO_DATABASE" < "$file"
  done
  [ "$found" = true ] || echo "[mongo-init] no $label scripts; nothing to apply."
}
run_scripts "$ROOT/init/mongodb/schema" "schema"
run_scripts "$ROOT/init/mongodb/indexes" "index"
run_scripts "$ROOT/init/mongodb/data" "seed"
echo "[mongo-init] initialization completed without destructive operations."
