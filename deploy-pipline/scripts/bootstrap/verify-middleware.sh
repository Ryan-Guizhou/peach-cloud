#!/usr/bin/env sh
set -eu

wait_container() {
  container=$1
  attempts=${2:-90}
  i=0
  while [ "$i" -lt "$attempts" ]; do
    if docker inspect "$container" >/dev/null 2>&1; then
      status=$(docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container" 2>/dev/null || true)
      if [ "$status" = "healthy" ] || [ "$status" = "running" ]; then
        echo "[OK] $container ($status)"
        return 0
      fi
    fi
    i=$((i + 1))
    sleep 2
  done
  echo "[FAIL] $container is not ready" >&2
  return 1
}

verify_mysql() {
  wait_container peach-mysql
  docker exec peach-mysql sh -c 'mysqladmin ping -h 127.0.0.1 -uroot -p"$MYSQL_ROOT_PASSWORD" --silent' >/dev/null
  echo "[OK] MySQL accepted an authenticated ping"
}

verify_redis() {
  wait_container peach-redis
  docker exec peach-redis sh -c 'redis-cli -a "$REDIS_PASSWORD" ping' | grep -q PONG
  echo "[OK] Redis accepted an authenticated ping"
}

verify_nacos() {
  wait_container peach-nacos
  docker exec peach-nacos curl -fsS http://127.0.0.1:8848/nacos/actuator/health >/dev/null
  echo "[OK] Nacos health endpoint is reachable"
}

verify_mongodb() {
  wait_container peach-mongo
  docker exec peach-mongo sh -c 'mongosh --quiet --username "$MONGO_INITDB_ROOT_USERNAME" --password "$MONGO_INITDB_ROOT_PASSWORD" --authenticationDatabase admin --eval "db.runCommand({ ping: 1 }).ok"' | grep -q 1
  echo "[OK] MongoDB accepted an authenticated ping"
}

verify_rocketmq() {
  wait_container peach-rocketmq-namesrv
  wait_container peach-rocketmq-broker
  docker exec peach-rocketmq-broker sh mqadmin clusterList -n rocketmq-namesrv:9876 >/dev/null
  echo "[OK] RocketMQ broker is registered with NameServer"
}

verify_target() {
  case "$1" in
    mysql) verify_mysql ;;
    redis) verify_redis ;;
    nacos) verify_nacos ;;
    mongodb) verify_mongodb ;;
    rocketmq) verify_rocketmq ;;
    *) echo "Unsupported middleware verification target: $1" >&2; exit 2 ;;
  esac
}

if [ "$#" -gt 0 ]; then
  for target in "$@"; do
    verify_target "$target"
  done
else
  for target in mysql redis nacos mongodb rocketmq; do
    verify_target "$target"
  done
fi
