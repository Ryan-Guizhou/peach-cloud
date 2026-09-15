#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
CONFIG_DIR="$ROOT/init/nacos/config"
GENERATED_DIR="$ROOT/runtime/generated/nacos"
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
SYNC=false
[ "${1:-}" = "--sync" ] && SYNC=true
[ -f "$ENV_FILE" ] || { echo "Missing env file: $ENV_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
NACOS_NAMESPACE_ID=${NACOS_NAMESPACE_ID:-peach-cloud}; NACOS_NAMESPACE_NAME=${NACOS_NAMESPACE_NAME:-$NACOS_NAMESPACE_ID}; NACOS_GROUP=${NACOS_GROUP:-PEACH-CLOUD}; NACOS_USERNAME=${NACOS_USERNAME:-nacos}; NACOS_PASSWORD=${NACOS_PASSWORD:-nacos}; NACOS_AUTH_ENABLE=${NACOS_AUTH_ENABLE:-false}; MYSQL_DATABASE=${MYSQL_DATABASE:-peach_cloud}; MYSQL_HOST=${MYSQL_HOST:-mysql:3306}; REDIS_HOST=${REDIS_HOST:-redis:6379}
resolve_nacos_addr() { if [ -n "${NACOS_ADDR:-}" ]; then printf '%s' "$NACOS_ADDR"; return 0; fi; if curl --max-time 2 -fsS http://nacos:8848/nacos/actuator/health >/dev/null 2>&1; then printf '%s' 'http://nacos:8848'; else printf 'http://127.0.0.1:%s' "${NACOS_HOST_PORT:-8849}"; fi; }
NACOS_ADDR=$(resolve_nacos_addr)
mkdir -p "$GENERATED_DIR"
wait_nacos() { i=0; while [ "$i" -lt 90 ]; do if curl --max-time 3 -fsS "$NACOS_ADDR/nacos/actuator/health" >/dev/null 2>&1; then return 0; fi; i=$((i + 1)); sleep 2; done; echo "Nacos is not ready: $NACOS_ADDR" >&2; exit 1; }
login() { [ "$NACOS_AUTH_ENABLE" = "true" ] || return 0; curl -fsS -X POST "$NACOS_ADDR/nacos/v1/auth/users/login" -d "username=$NACOS_USERNAME" --data-urlencode "password=$NACOS_PASSWORD" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'; }
with_token() { url=$1; if [ -n "${ACCESS_TOKEN:-}" ]; then case "$url" in *\?*) printf '%s&accessToken=%s' "$url" "$ACCESS_TOKEN" ;; *) printf '%s?accessToken=%s' "$url" "$ACCESS_TOKEN" ;; esac; else printf '%s' "$url"; fi; }
ensure_namespace() { url=$(with_token "$NACOS_ADDR/nacos/v1/console/namespaces"); if curl -fsS "$url" | grep -q "\"namespace\":\"$NACOS_NAMESPACE_ID\""; then echo "[nacos-init] namespace preserved: $NACOS_NAMESPACE_ID"; return 0; fi; create_url=$(with_token "$NACOS_ADDR/nacos/v1/console/namespaces"); curl -fsS -X POST "$create_url" -d "customNamespaceId=$NACOS_NAMESPACE_ID" --data-urlencode "namespaceName=$NACOS_NAMESPACE_NAME" --data-urlencode "namespaceDesc=Created by Peach Docker bootstrap" >/dev/null; echo "[nacos-init] namespace created: $NACOS_NAMESPACE_ID"; }
escape_sed() { printf '%s' "$1" | sed 's/[\/&]/\\&/g'; }
render() { src=$1; dst=$2; cp "$src" "$dst"; sed -i -e "s/@MYSQL_HOST@/$(escape_sed "$MYSQL_HOST")/g" -e "s/@MYSQL_DATABASE@/$(escape_sed "$MYSQL_DATABASE")/g" -e "s/@MYSQL_ROOT_PASSWORD@/$(escape_sed "$MYSQL_ROOT_PASSWORD")/g" -e "s/@REDIS_HOST@/$(escape_sed "$REDIS_HOST")/g" -e "s/@REDIS_PASSWORD@/$(escape_sed "$REDIS_PASSWORD")/g" -e "s/@OSS_ACCESS_KEY@/$(escape_sed "${OSS_ACCESS_KEY:-}")/g" -e "s/@OSS_SECRET_KEY@/$(escape_sed "${OSS_SECRET_KEY:-}")/g" -e "s/@COS_ACCESS_KEY@/$(escape_sed "${COS_ACCESS_KEY:-}")/g" -e "s/@COS_SECRET_KEY@/$(escape_sed "${COS_SECRET_KEY:-}")/g" "$dst"; }
exists_config() { data_id=$1; url=$(with_token "$NACOS_ADDR/nacos/v1/cs/configs?dataId=$data_id&group=$NACOS_GROUP&tenant=$NACOS_NAMESPACE_ID"); code=$(curl -sS -o "$GENERATED_DIR/existing.tmp" -w '%{http_code}' "$url" || true); [ "$code" = "200" ] && [ -s "$GENERATED_DIR/existing.tmp" ]; }
publish() { file=$1; data_id=$(basename "$file"); if [ "$SYNC" != "true" ] && exists_config "$data_id"; then echo "[nacos-init] config preserved: $data_id"; return 0; fi; rendered="$GENERATED_DIR/$data_id"; render "$file" "$rendered"; case "$data_id" in *.yml|*.yaml) type=yaml ;; *.json) type=json ;; *) type=text ;; esac; url=$(with_token "$NACOS_ADDR/nacos/v1/cs/configs"); curl -fsS -X POST "$url" -d "tenant=$NACOS_NAMESPACE_ID" -d "group=$NACOS_GROUP" -d "dataId=$data_id" -d "type=$type" --data-urlencode "content@$rendered" >/dev/null; rm -f "$rendered"; echo "[nacos-init] config published: $data_id"; }
wait_nacos; ACCESS_TOKEN=$(login); ensure_namespace
for file in "$CONFIG_DIR"/*.yml "$CONFIG_DIR"/*.yaml "$CONFIG_DIR"/*.json; do [ -f "$file" ] && publish "$file"; done
rm -f "$GENERATED_DIR/existing.tmp"
echo "[nacos-init] completed (sync=$SYNC, addr=$NACOS_ADDR)."
