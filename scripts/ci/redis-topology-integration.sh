#!/usr/bin/env bash
set -euo pipefail

# 只在临时 GitHub Actions runner 上使用，不连接生产环境 Redis。
topology="${1:?topology is required}"
redis_image="redis:7.2-alpine"
tmp_dir="$(mktemp -d "${RUNNER_TEMP:-/tmp}/peach-redis-it.XXXXXX")"
declare -a containers=()
cleanup() {
  if (("${#containers[@]}" > 0)); then
    docker rm -f "${containers[@]}" >/dev/null 2>&1 || true
  fi
  rm -rf "$tmp_dir"
}
trap cleanup EXIT

redis_cli() {
  docker run --rm --network host "$redis_image" redis-cli "$@"
}
await_ping() {
  local port="$1"
  local attempt
  for attempt in $(seq 1 50); do
    if [[ "$(redis_cli -h 127.0.0.1 -p "$port" ping 2>/dev/null || true)" == "PONG" ]]; then
      return 0
    fi
    sleep 1
  done
  echo "Redis port $port failed to start" >&2
  return 1
}
start_redis() {
  local name="$1"
  shift
  containers+=("$name")
  docker run -d --name "$name" --network host "$redis_image" redis-server "$@" >/dev/null
}
export RUN_REDIS_TOPOLOGY_IT=true
export REDIS_TOPOLOGY_TLS=false
export REDIS_TOPOLOGY_SENTINEL_MASTER=peach-master

case "$topology" in
  sentinel)
    export REDIS_TOPOLOGY_MODE=sentinel
    export REDIS_TOPOLOGY_HOSTS=127.0.0.1:26379,127.0.0.1:26380,127.0.0.1:26381
    start_redis peach-it-master --port 6380 --bind 127.0.0.1 --protected-mode no --save ""
    await_ping 6380
    start_redis peach-it-replica-1 --port 6381 --bind 127.0.0.1 --protected-mode no --save "" --replicaof 127.0.0.1 6380
    start_redis peach-it-replica-2 --port 6382 --bind 127.0.0.1 --protected-mode no --save "" --replicaof 127.0.0.1 6380
    await_ping 6381
    await_ping 6382
    chmod 777 "$tmp_dir"
    for port in 26379 26380 26381; do
      cat >"$tmp_dir/sentinel-$port.conf" <<EOF
port $port
bind 127.0.0.1
protected-mode no
sentinel monitor peach-master 127.0.0.1 6380 2
sentinel down-after-milliseconds peach-master 5000
sentinel failover-timeout peach-master 30000
sentinel parallel-syncs peach-master 1
EOF
      chmod 666 "$tmp_dir/sentinel-$port.conf"
      containers+=("peach-it-sentinel-$port")
      docker run -d --name "peach-it-sentinel-$port" --network host --user 0:0 \
        --entrypoint redis-server -v "$tmp_dir:/ci:rw" "$redis_image" "/ci/sentinel-$port.conf" --sentinel >/dev/null
      await_ping "$port"
    done
    ;;
  cluster)
    export REDIS_TOPOLOGY_MODE=cluster
    export REDIS_TOPOLOGY_HOSTS=127.0.0.1:7001,127.0.0.1:7002,127.0.0.1:7003
    for port in 7001 7002 7003 7004 7005 7006; do
      start_redis "peach-it-cluster-$port" --port "$port" --bind 127.0.0.1 \
        --cluster-enabled yes --cluster-config-file nodes.conf --cluster-node-timeout 5000 \
        --cluster-announce-ip 127.0.0.1 --cluster-announce-port "$port" \
        --cluster-announce-bus-port "$((port+10000))" --protected-mode no --save ""
      await_ping "$port"
    done
    redis_cli --cluster create \
      127.0.0.1:7001 127.0.0.1:7002 127.0.0.1:7003 \
      127.0.0.1:7004 127.0.0.1:7005 127.0.0.1:7006 \
      --cluster-replicas 1 --cluster-yes
    for attempt in $(seq 1 50); do
      if redis_cli -p 7001 cluster info | grep -q 'cluster_state:ok'; then
        break
      fi
      if ((attempt == 50)); then
        echo "Redis Cluster did not form" >&2
        exit 1
      fi
      sleep 1
    done
    ;;
  tls)
    export REDIS_TOPOLOGY_MODE=standalone
    export REDIS_TOPOLOGY_HOSTS=127.0.0.1:6389
    export REDIS_TOPOLOGY_TLS=true
    openssl req -x509 -newkey rsa:2048 -sha256 -days 1 -nodes \
      -keyout "$tmp_dir/redis.key" -out "$tmp_dir/redis.crt" -subj "/CN=localhost" \
      -addext "subjectAltName=DNS:localhost,IP:127.0.0.1" >/dev/null 2>&1
    chmod 644 "$tmp_dir/redis.key" "$tmp_dir/redis.crt"
    keytool -importcert -noprompt -alias peach-ci-redis -file "$tmp_dir/redis.crt" \
      -keystore "$tmp_dir/truststore.p12" -storetype PKCS12 -storepass changeit >/dev/null
    export JAVA_TOOL_OPTIONS="-Djavax.net.ssl.trustStore=$tmp_dir/truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit -Djavax.net.ssl.trustStoreType=PKCS12"
    containers+=("peach-it-tls")
    docker run -d --name peach-it-tls --network host --entrypoint redis-server \
      -v "$tmp_dir:/tls:ro" "$redis_image" --port 0 --tls-port 6389 \
      --tls-cert-file /tls/redis.crt --tls-key-file /tls/redis.key \
      --tls-ca-cert-file /tls/redis.crt --tls-auth-clients no --protected-mode no --save "" >/dev/null
    ready=false
    for attempt in $(seq 1 50); do
      if [[ "$(docker run --rm --network host -v "$tmp_dir:/tls:ro" "$redis_image" \
        redis-cli --tls --cacert /tls/redis.crt -h 127.0.0.1 -p 6389 ping 2>/dev/null || true)" == "PONG" ]]; then
        ready=true
        break
      fi
      sleep 1
    done
    if [[ "$ready" != true ]]; then
      echo "Redis TLS endpoint failed readiness" >&2
      exit 1
    fi
    ;;
  *)
    echo "Unknown Redis topology: $topology" >&2
    exit 2
    ;;
esac

echo "Running real Redis $topology tests with TLS verification preserved"
# Use an explicit HTTPS public mirror for the repository's HTTP-only Nexus IDs.
# The ephemeral GitHub runner must not rely on an inherited ~/.m2 cache.
# Local developers keep their own Nexus settings; CI deliberately resolves public releases via HTTPS.
mvn -B -ntp -s scripts/ci/github-actions-maven-settings.xml \
  -pl peach-middleware/peach-redis/peach-redis-quickstart,peach-middleware/peach-redis/peach-redis-tool-quickstart,peach-middleware/peach-redission/peach-redisson-quickstart,peach-middleware/peach-satoken/peach-satoken-quickstart \
  -am \
  -Dtest=RedisTopologyServerIntegrationTest,RedisToolTopologyServerIntegrationTest,RedissonTopologyServerIntegrationTest,SaTokenSearchTopologyServerIntegrationTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

suites = {
    "peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redis/peach-redis-tool-quickstart/target/surefire-reports/TEST-com.peach.redis.tool.quickstart.RedisToolTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redission/peach-redisson-quickstart/target/surefire-reports/TEST-com.peach.redisson.quickstart.RedissonTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-satoken/peach-satoken-quickstart/target/surefire-reports/TEST-com.peach.satoken.quickstart.SaTokenSearchTopologyServerIntegrationTest.xml",
}
for file in sorted(suites):
    path = Path(file)
    if not path.exists():
        raise SystemExit(f"Missing surefire integration report: {path}")
    result = ET.parse(path).getroot()
    values = {key: int(result.attrib.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
    if values["tests"] != 1 or any(values[key] for key in ("failures", "errors", "skipped")):
        raise SystemExit(f"Invalid integration result {file}: {values}")
    print(f"PASS {file}: {values}")
PY
