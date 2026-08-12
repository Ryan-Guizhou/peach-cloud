#!/usr/bin/env sh
set -eu

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEPLOY_DIR=$(CDPATH= cd -- "$SCRIPT_DIR/.." && pwd)
CONFIG_DIR="$SCRIPT_DIR/config"
RUNTIME_NACOS_DIR="$DEPLOY_DIR/runtime/nacos"

if [ -f "$DEPLOY_DIR/.env" ]; then
  set -a
  . "$DEPLOY_DIR/.env"
  set +a
else
  echo "Missing $DEPLOY_DIR/.env. Copy .env.example to .env first." >&2
  exit 1
fi

NACOS_HOST_PORT=${NACOS_HOST_PORT:-8849}
NACOS_ADDR=${NACOS_ADDR:-"http://127.0.0.1:$NACOS_HOST_PORT"}
NACOS_NAMESPACE_ID=${NACOS_NAMESPACE_ID:-peach-cloud}
NACOS_NAMESPACE_NAME=${NACOS_NAMESPACE_NAME:-$NACOS_NAMESPACE_ID}
NACOS_GROUP=${NACOS_GROUP:-PEACH-CLOUD}
NACOS_USERNAME=${NACOS_USERNAME:-nacos}
NACOS_PASSWORD=${NACOS_PASSWORD:-nacos}
NACOS_AUTH_ENABLE=${NACOS_AUTH_ENABLE:-false}
MYSQL_DATABASE=${MYSQL_DATABASE:-peach_cloud}
MYSQL_HOST=${MYSQL_HOST:-mysql:3306}
REDIS_HOST=${REDIS_HOST:-redis:6379}

mkdir -p "$RUNTIME_NACOS_DIR"

sed_escape() {
  printf '%s' "$1" | sed 's/[\/&]/\\&/g'
}

wait_nacos() {
  i=0
  while [ "$i" -lt 90 ]; do
    if curl -fsS "$NACOS_ADDR/nacos/actuator/health" >/dev/null 2>&1; then
      return 0
    fi
    i=$((i + 1))
    sleep 2
  done
  echo "Nacos is not ready: $NACOS_ADDR" >&2
  exit 1
}

nacos_token() {
  if [ "$NACOS_AUTH_ENABLE" = "true" ]; then
    response=$(curl -fsS -X POST "$NACOS_ADDR/nacos/v1/auth/users/login" \
      -d "username=$NACOS_USERNAME" \
      --data-urlencode "password=$NACOS_PASSWORD")
    printf '%s' "$response" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
  fi
}

api_suffix() {
  if [ -n "${ACCESS_TOKEN:-}" ]; then
    printf 'accessToken=%s' "$ACCESS_TOKEN"
  else
    printf ''
  fi
}

with_token_url() {
  suffix=$(api_suffix)
  if [ -n "$suffix" ]; then
    printf '%s?%s' "$1" "$suffix"
  else
    printf '%s' "$1"
  fi
}

ensure_namespace() {
  list_url=$(with_token_url "$NACOS_ADDR/nacos/v1/console/namespaces")
  namespaces=$(curl -fsS "$list_url")
  if printf '%s' "$namespaces" | grep -q "\"namespace\":\"$NACOS_NAMESPACE_ID\""; then
    return 0
  fi

  create_url=$(with_token_url "$NACOS_ADDR/nacos/v1/console/namespaces")
  curl -fsS -X POST "$create_url" \
    -d "customNamespaceId=$NACOS_NAMESPACE_ID" \
    --data-urlencode "namespaceName=$NACOS_NAMESPACE_NAME" \
    --data-urlencode "namespaceDesc=Created by peach deploy script" >/dev/null || true

  namespaces=$(curl -fsS "$list_url")
  if ! printf '%s' "$namespaces" | grep -q "\"namespace\":\"$NACOS_NAMESPACE_ID\""; then
    echo "Failed to ensure Nacos namespace: $NACOS_NAMESPACE_ID" >&2
    exit 1
  fi
}

render_template() {
  src="$1"
  dst="$2"
  cp "$src" "$dst"
  mysql_database_escaped=$(sed_escape "$MYSQL_DATABASE")
  mysql_host_escaped=$(sed_escape "$MYSQL_HOST")
  mysql_password_escaped=$(sed_escape "$MYSQL_ROOT_PASSWORD")
  redis_host_escaped=$(sed_escape "$REDIS_HOST")
  redis_password_escaped=$(sed_escape "$REDIS_PASSWORD")
  oss_access_key_escaped=$(sed_escape "$OSS_ACCESS_KEY")
  oss_secret_key_escaped=$(sed_escape "$OSS_SECRET_KEY")
  cos_access_key_escaped=$(sed_escape "$COS_ACCESS_KEY")
  cos_secret_key_escaped=$(sed_escape "$COS_SECRET_KEY")
  sed -i \
    -e "s/@MYSQL_HOST@/$mysql_host_escaped/g" \
    -e "s/@MYSQL_DATABASE@/$mysql_database_escaped/g" \
    -e "s/@MYSQL_ROOT_PASSWORD@/$mysql_password_escaped/g" \
    -e "s/@REDIS_HOST@/$redis_host_escaped/g" \
    -e "s/@REDIS_PASSWORD@/$redis_password_escaped/g" \
    -e "s/@OSS_ACCESS_KEY@/$oss_access_key_escaped/g" \
    -e "s/@OSS_SECRET_KEY@/$oss_secret_key_escaped/g" \
    -e "s/@COS_ACCESS_KEY@/$cos_access_key_escaped/g" \
    -e "s/@COS_SECRET_KEY@/$cos_secret_key_escaped/g" \
    "$dst"
}

publish_config() {
  file="$1"
  data_id=$(basename "$file")
  tmp="$RUNTIME_NACOS_DIR/$data_id.rendered"
  tmp_name="$data_id.rendered"
  render_template "$file" "$tmp"

  config_url=$(with_token_url "$NACOS_ADDR/nacos/v1/cs/configs")
  (
    cd "$RUNTIME_NACOS_DIR"
    curl -fsS -X POST "$config_url" \
      -d "tenant=$NACOS_NAMESPACE_ID" \
      -d "group=$NACOS_GROUP" \
      -d "dataId=$data_id" \
      -d "type=yaml" \
      --data-urlencode "content@$tmp_name" >/dev/null
  )

  rm -f "$tmp"
  echo "Imported $data_id to namespace=$NACOS_NAMESPACE_ID group=$NACOS_GROUP"
}

write_runtime_env() {
  {
    printf 'NACOS_NAMESPACE_ID=%s\n' "$NACOS_NAMESPACE_ID"
    printf 'NACOS_NAMESPACE_NAME=%s\n' "$NACOS_NAMESPACE_NAME"
    printf 'NACOS_GROUP=%s\n' "$NACOS_GROUP"
  } > "$RUNTIME_NACOS_DIR/nacos.env"
}

wait_nacos
ACCESS_TOKEN=$(nacos_token)
ensure_namespace
write_runtime_env

for file in "$CONFIG_DIR"/*.yml; do
  publish_config "$file"
done
