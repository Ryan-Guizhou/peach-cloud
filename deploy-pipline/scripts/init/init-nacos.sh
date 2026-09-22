#!/usr/bin/env sh
set -eu
umask 077

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
CONFIG_DIR="$ROOT/init/nacos/config"
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
RENDERER="$ROOT/scripts/config/render-nacos-config.mjs"
SYNC=false
[ "${1:-}" = "--sync" ] && SYNC=true
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
[ -f "$RENDERER" ] || { echo "Missing Nacos config renderer: $RENDERER" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a

NACOS_NAMESPACE_ID=${NACOS_NAMESPACE_ID:-peach-cloud}
NACOS_NAMESPACE_NAME=${NACOS_NAMESPACE_NAME:-$NACOS_NAMESPACE_ID}
NACOS_GROUP=${NACOS_GROUP:-PEACH-CLOUD}
NACOS_USERNAME=${NACOS_USERNAME:-nacos}
NACOS_PASSWORD=${NACOS_PASSWORD:-nacos}
NACOS_AUTH_ENABLE=${NACOS_AUTH_ENABLE:-false}
MYSQL_DATABASE=${MYSQL_DATABASE:-peach_cloud}
MYSQL_HOST=${MYSQL_HOST:-mysql:3306}
REDIS_HOST=${REDIS_HOST:-redis:6379}
export MYSQL_DATABASE MYSQL_HOST REDIS_HOST

mkdir -p "$ROOT/runtime/generated"
chmod 700 "$ROOT/runtime/generated"
GENERATED_DIR=$(mktemp -d "$ROOT/runtime/generated/nacos.XXXXXX")
TOKEN_FILE="$GENERATED_DIR/nacos.token"
cleanup() { rm -rf "$GENERATED_DIR"; }
trap cleanup EXIT HUP INT TERM

resolve_nacos_addr() {
  if [ -n "${NACOS_ADDR:-}" ]; then
    printf '%s' "$NACOS_ADDR"
    return 0
  fi
  if curl --max-time 2 -fsS http://nacos:8848/nacos/actuator/health >/dev/null 2>&1; then
    printf '%s' 'http://nacos:8848'
  else
    printf 'http://127.0.0.1:%s' "${NACOS_HOST_PORT:-8849}"
  fi
}
NACOS_ADDR=$(resolve_nacos_addr)

wait_nacos() {
  i=0
  while [ "$i" -lt 90 ]; do
    if curl --max-time 3 -fsS "$NACOS_ADDR/nacos/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    i=$((i + 1))
    sleep 2
  done
  echo "Nacos is not ready: $NACOS_ADDR" >&2
  exit 1
}

login() {
  [ "$NACOS_AUTH_ENABLE" = "true" ] || return 0
  username_file="$GENERATED_DIR/nacos.username"
  password_file="$GENERATED_DIR/nacos.password"
  printf '%s' "$NACOS_USERNAME" > "$username_file"
  printf '%s' "$NACOS_PASSWORD" > "$password_file"
  response=$(curl -fsS -X POST "$NACOS_ADDR/nacos/v1/auth/users/login" \
    --data-urlencode "username@$username_file" \
    --data-urlencode "password@$password_file")
  rm -f "$username_file" "$password_file"
  printf '%s' "$response" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

nacos_get() {
  endpoint=$1
  shift
  if [ -s "$TOKEN_FILE" ]; then
    curl -fsS -G "$endpoint" "$@" --data-urlencode "accessToken@$TOKEN_FILE"
  else
    curl -fsS -G "$endpoint" "$@"
  fi
}

nacos_post() {
  endpoint=$1
  shift
  if [ -s "$TOKEN_FILE" ]; then
    curl -fsS -X POST "$endpoint" "$@" --data-urlencode "accessToken@$TOKEN_FILE"
  else
    curl -fsS -X POST "$endpoint" "$@"
  fi
}

ensure_namespace() {
  if nacos_get "$NACOS_ADDR/nacos/v1/console/namespaces" | grep -q "\"namespace\":\"$NACOS_NAMESPACE_ID\""; then
    echo "[nacos-init] namespace preserved: $NACOS_NAMESPACE_ID"
    return 0
  fi
  nacos_post "$NACOS_ADDR/nacos/v1/console/namespaces" \
    --data-urlencode "customNamespaceId=$NACOS_NAMESPACE_ID" \
    --data-urlencode "namespaceName=$NACOS_NAMESPACE_NAME" \
    --data-urlencode "namespaceDesc=Created by Peach Docker bootstrap" >/dev/null
  echo "[nacos-init] namespace created: $NACOS_NAMESPACE_ID"
}

render() {
  src=$1
  dst=$2
  node "$RENDERER" --source "$src" --output "$dst"
}

exists_config() {
  data_id=$1
  existing_file="$GENERATED_DIR/existing.$$.tmp"
  if [ -s "$TOKEN_FILE" ]; then
    code=$(curl -sS -G -o "$existing_file" -w '%{http_code}' "$NACOS_ADDR/nacos/v1/cs/configs" \
      --data-urlencode "dataId=$data_id" \
      --data-urlencode "group=$NACOS_GROUP" \
      --data-urlencode "tenant=$NACOS_NAMESPACE_ID" \
      --data-urlencode "accessToken@$TOKEN_FILE" || true)
  else
    code=$(curl -sS -G -o "$existing_file" -w '%{http_code}' "$NACOS_ADDR/nacos/v1/cs/configs" \
      --data-urlencode "dataId=$data_id" \
      --data-urlencode "group=$NACOS_GROUP" \
      --data-urlencode "tenant=$NACOS_NAMESPACE_ID" || true)
  fi
  [ "$code" = "200" ] && [ -s "$existing_file" ]
}

publish() {
  file=$1
  data_id=$(basename "$file")
  if [ "$SYNC" != "true" ] && exists_config "$data_id"; then
    echo "[nacos-init] config preserved: $data_id"
    return 0
  fi

  rendered="$GENERATED_DIR/$data_id"
  render "$file" "$rendered"
  case "$data_id" in
    *.yml|*.yaml) type=yaml ;;
    *.json) type=json ;;
    *) type=text ;;
  esac
  nacos_post "$NACOS_ADDR/nacos/v1/cs/configs" \
    --data-urlencode "tenant=$NACOS_NAMESPACE_ID" \
    --data-urlencode "group=$NACOS_GROUP" \
    --data-urlencode "dataId=$data_id" \
    --data-urlencode "type=$type" \
    --data-urlencode "content@$rendered" >/dev/null
  rm -f "$rendered"
  echo "[nacos-init] config published: $data_id"
}

wait_nacos
if [ "$NACOS_AUTH_ENABLE" = "true" ]; then
  access_token=$(login)
  [ -n "$access_token" ] || { echo "Nacos authentication did not return an access token." >&2; exit 1; }
  printf '%s' "$access_token" > "$TOKEN_FILE"
  access_token=
fi
ensure_namespace
for file in "$CONFIG_DIR"/*.yml "$CONFIG_DIR"/*.yaml "$CONFIG_DIR"/*.json; do
  [ -f "$file" ] && publish "$file"
done

echo "[nacos-init] completed (sync=$SYNC, addr=$NACOS_ADDR)."
