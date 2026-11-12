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
export REDIS_TOPOLOGY_ACL=false
export RUN_REDIS_ACL_IT=false
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
    # Preserve the JDK default CA roots for Maven HTTPS dependency resolution.
    # Add only the ephemeral Redis CI certificate; never disable TLS verification.
    keytool -importkeystore -noprompt \
      -srckeystore "$JAVA_HOME/lib/security/cacerts" -srcstorepass changeit \
      -destkeystore "$tmp_dir/truststore.p12" -deststoretype PKCS12 \
      -deststorepass changeit >/dev/null
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
  acl)
    # Use a new, random CI-only password. No credentials are committed or printed.
    export REDIS_TOPOLOGY_MODE=standalone
    export REDIS_TOPOLOGY_HOSTS=127.0.0.1:6390
    export REDIS_TOPOLOGY_ACL=true
    export RUN_REDIS_ACL_IT=true
    export REDIS_TOPOLOGY_USERNAME=peach_ci_acl
    export REDIS_TOPOLOGY_PASSWORD="$(openssl rand -hex 24)"
    chmod 755 "$tmp_dir"
    cat >"$tmp_dir/users.acl" <<EOF
user default off
user $REDIS_TOPOLOGY_USERNAME on >$REDIS_TOPOLOGY_PASSWORD ~* &* +@all
EOF
    chmod 644 "$tmp_dir/users.acl"
    containers+=("peach-it-acl")
    docker run -d --name peach-it-acl --network host -v "$tmp_dir:/ci:ro" \
      "$redis_image" redis-server --port 6390 --bind 127.0.0.1 \
      --protected-mode no --aclfile /ci/users.acl --save "" >/dev/null
    ready=false
    for attempt in $(seq 1 50); do
      if [[ "$(docker run --rm --network host \
        -e REDISCLI_AUTH="$REDIS_TOPOLOGY_PASSWORD" "$redis_image" \
        redis-cli --user "$REDIS_TOPOLOGY_USERNAME" -h 127.0.0.1 -p 6390 ping 2>/dev/null || true)" == "PONG" ]]; then
        ready=true
        break
      fi
      sleep 1
    done
    if [[ "$ready" != true ]]; then
      echo "Isolated Redis ACL service never became ready" >&2
      exit 1
    fi
    ;;
  *)
    echo "Unknown Redis topology: $topology" >&2
    exit 2
    ;;
esac

topology_tests="RedisTopologyServerIntegrationTest,RedisToolTopologyServerIntegrationTest,RedissonTopologyServerIntegrationTest,SaTokenSearchTopologyServerIntegrationTest"
if [[ "$topology" == "acl" ]]; then
  topology_tests+=",RedisAclAuthenticationServerIntegrationTest"
fi
echo "Running real Redis $topology tests with TLS and ACL verification preserved"
# Use an explicit HTTPS public mirror for the repository's HTTP-only Nexus IDs.
# The ephemeral GitHub runner must not rely on an inherited ~/.m2 cache.
# Local developers keep their own Nexus settings; CI deliberately resolves public releases via HTTPS.
mvn -B -ntp -s scripts/ci/github-actions-maven-settings.xml \
  -pl peach-middleware/peach-redis/peach-redis-quickstart,peach-middleware/peach-redis/peach-redis-tool-quickstart,peach-middleware/peach-redission/peach-redisson-quickstart,peach-middleware/peach-satoken/peach-satoken-quickstart \
  -am \
  -Dtest="$topology_tests" \
  -Dsurefire.failIfNoSpecifiedTests=false test

python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
import os

suites = {
    "peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redis/peach-redis-tool-quickstart/target/surefire-reports/TEST-com.peach.redis.tool.quickstart.RedisToolTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redission/peach-redisson-quickstart/target/surefire-reports/TEST-com.peach.redisson.quickstart.RedissonTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-satoken/peach-satoken-quickstart/target/surefire-reports/TEST-com.peach.satoken.quickstart.SaTokenSearchTopologyServerIntegrationTest.xml",
}
if os.environ.get("REDIS_TOPOLOGY_ACL") == "true":
    suites.add("peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisAclAuthenticationServerIntegrationTest.xml")
for file in sorted(suites):
    path = Path(file)
    if not path.exists():
        raise SystemExit(f"Missing surefire integration report: {path}")
    result = ET.parse(path).getroot()
    values = {key: int(result.attrib.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
    expected = 2 if ("RedisAclAuthenticationServerIntegrationTest" in file or "RedisToolTopologyServerIntegrationTest" in file) else 1
    if values["tests"] != expected or any(values[key] for key in ("failures", "errors", "skipped")):
        raise SystemExit(f"Invalid integration result {file}: {values}")
    print(f"PASS {file}: {values}")
PY

# A topology smoke test alone does not prove Sentinel master outage recovery.
# Stop the actual Redis master, require all three sentinels to agree on a promoted
# replica, and run the same real-client suites again with newly created clients.
if [[ "$topology" == "sentinel" ]]; then
  echo "Validating long-lived Jedis and Redisson clients across real Sentinel master outage"
  RUN_REDIS_SENTINEL_LIVE_IT=true mvn -B -ntp -s scripts/ci/github-actions-maven-settings.xml \
    -pl peach-middleware/peach-redis/peach-redis-quickstart -am \
    -Dtest=SentinelLiveClientFailoverServerIntegrationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test
  python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET
report = Path("peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.SentinelLiveClientFailoverServerIntegrationTest.xml")
if not report.exists():
    raise SystemExit(f"Long-lived Sentinel test did not execute: {report}")
root = ET.parse(report).getroot()
values = {key: int(root.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
if values["tests"] != 1 or any(values[k] for k in ("failures", "errors", "skipped")):
    raise SystemExit(f"Long-lived Sentinel test did not pass: {values}")
print(f"PASS existing Jedis/Redisson client recovery after master outage: {values}")
PY
  echo "Verifying Sentinel consensus and newly established client connections"
  # The long-lived client test stops the original master itself. A still-running
  # master would mean the test never injected the required fault.
  if [[ "$(docker inspect --format '{{.State.Running}}' peach-it-master)" != "false" ]]; then
    echo "Live-client integration did not stop the original Sentinel master" >&2
    exit 1
  fi
  new_master_port=""
  for attempt in $(seq 1 90); do
    agree=true
    agreed_port=""
    for sentinel_port in 26379 26380 26381; do
      port="$(redis_cli -h 127.0.0.1 -p "$sentinel_port" --raw \
        SENTINEL get-master-addr-by-name peach-master 2>/dev/null | tail -n 1 | tr -d '\r' || true)"
      if [[ "$port" != "6381" && "$port" != "6382" ]]; then
        agree=false
        break
      fi
      if [[ -n "$agreed_port" && "$agreed_port" != "$port" ]]; then
        agree=false
        break
      fi
      agreed_port="$port"
    done
    if [[ "$agree" == true && -n "$agreed_port" ]]; then
      role="$(redis_cli -h 127.0.0.1 -p "$agreed_port" INFO replication 2>/dev/null \
        | tr -d '\r' | grep '^role:' | cut -d: -f2 || true)"
      if [[ "$role" == "master" ]]; then
        new_master_port="$agreed_port"
        break
      fi
    fi
    sleep 1
  done
  if [[ -z "$new_master_port" ]]; then
    echo "Sentinel did not promote a replica with unanimous monitor agreement" >&2
    exit 1
  fi
  echo "Promoted Redis master port: $new_master_port (original 6380 still down)"
  test_key="peach:sentinel:ci:failover:$RANDOM"
  if [[ "$(redis_cli -h 127.0.0.1 -p "$new_master_port" SET "$test_key" verified EX 40)" != "OK" ]]; then
    echo "Promoted master rejected a TTL write" >&2
    exit 1
  fi
  if [[ "$(redis_cli -h 127.0.0.1 -p "$new_master_port" GET "$test_key")" != "verified" ]]; then
    echo "Promoted master returned an invalid value" >&2
    exit 1
  fi
  redis_cli -h 127.0.0.1 -p "$new_master_port" DEL "$test_key" >/dev/null

  echo "Re-run actual independent clients and Sa-Token against promoted master"
  mvn -B -ntp -s scripts/ci/github-actions-maven-settings.xml \
    -pl peach-middleware/peach-redis/peach-redis-quickstart,peach-middleware/peach-redis/peach-redis-tool-quickstart,peach-middleware/peach-redission/peach-redisson-quickstart,peach-middleware/peach-satoken/peach-satoken-quickstart \
    -am \
    -Dtest=RedisTopologyServerIntegrationTest,RedisToolTopologyServerIntegrationTest,RedissonTopologyServerIntegrationTest,SaTokenSearchTopologyServerIntegrationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test

  python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

suites = [
    "peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redis/peach-redis-tool-quickstart/target/surefire-reports/TEST-com.peach.redis.tool.quickstart.RedisToolTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-redission/peach-redisson-quickstart/target/surefire-reports/TEST-com.peach.redisson.quickstart.RedissonTopologyServerIntegrationTest.xml",
    "peach-middleware/peach-satoken/peach-satoken-quickstart/target/surefire-reports/TEST-com.peach.satoken.quickstart.SaTokenSearchTopologyServerIntegrationTest.xml",
]
for file in suites:
    p = Path(file)
    if not p.exists():
        raise SystemExit(f"Missing post-promotion integration report: {p}")
    result = ET.parse(p).getroot()
    stats = {k: int(result.get(k, "0")) for k in ("tests", "failures", "errors", "skipped")}
    expected = 2 if "RedisToolTopologyServerIntegrationTest" in file else 1
    if stats["tests"] != expected or any(stats[k] for k in ("failures", "errors", "skipped")):
        raise SystemExit(f"Post-failover suite failed/skipped: {p} {stats}")
    print(f"PASS after Sentinel promotion: {p}")
PY
fi

# Redis Cluster failure injection is deliberately after the healthy topology checks.
# The failed master stays down; we require its former replica to be promoted before
# testing new Jedis, Redisson, Tool and Sa-Token connections.
if [[ "$topology" == "cluster" ]]; then
  echo "Injecting isolated Redis Cluster master outage and validating replica promotion"
  before_nodes="$(redis_cli -p 7002 CLUSTER NODES)"
  old_master_id="$(printf '%s\n' "$before_nodes" | awk '$2 ~ /:7001@/ && $3 ~ /master/ {print $1}')"
  if [[ -z "$old_master_id" ]]; then
    echo "Redis Cluster master 7001 identity unavailable before fault" >&2
    exit 1
  fi
  old_replica_port="$(printf '%s\n' "$before_nodes" | awk -v id="$old_master_id" '
    $4 == id && $3 ~ /slave/ {
      split($2, address, ":")
      split(address[2], port, "@")
      print port[1]
    }')"
  if [[ ! "$old_replica_port" =~ ^700[4-6]$ ]]; then
    echo "No identified replica for original Redis Cluster master: $old_replica_port" >&2
    exit 1
  fi

  docker stop --time 1 peach-it-cluster-7001 >/dev/null
  if [[ "$(docker inspect --format '{{.State.Running}}' peach-it-cluster-7001)" != "false" ]]; then
    echo "Redis Cluster master container did not stop" >&2
    exit 1
  fi

  recovered=false
  for attempt in $(seq 1 90); do
    cluster_state="$(redis_cli -p 7002 CLUSTER INFO 2>/dev/null | tr -d '\r' | grep '^cluster_state:' || true)"
    nodes="$(redis_cli -p 7002 CLUSTER NODES 2>/dev/null || true)"
    live_masters="$(printf '%s\n' "$nodes" | awk '
      $3 ~ /master/ && $8 == "connected" { count++ }
      END { print count+0 }')"
    new_role="$(redis_cli -p "$old_replica_port" INFO replication 2>/dev/null \
      | tr -d '\r' | grep '^role:' | cut -d: -f2 || true)"
    if [[ "$cluster_state" == "cluster_state:ok" && "$live_masters" == "3" && "$new_role" == "master" ]]; then
      recovered=true
      break
    fi
    sleep 1
  done
  if [[ "$recovered" != true ]]; then
    echo "Cluster did not recover three masters or promote expected replica $old_replica_port" >&2
    exit 1
  fi
  echo "Redis Cluster recovered; former replica $old_replica_port was promoted"

  test_key="peach:cluster:ci:failover:$RANDOM"
  if [[ "$(redis_cli -c -p 7002 SET "$test_key" verified EX 40)" != "OK" ]]; then
    echo "Recovered Redis Cluster rejected a redirected TTL write" >&2
    exit 1
  fi
  if [[ "$(redis_cli -c -p 7002 GET "$test_key")" != "verified" ]]; then
    echo "Recovered Redis Cluster returned invalid redirected value" >&2
    exit 1
  fi
  ttl="$(redis_cli -c -p 7002 TTL "$test_key" | tr -d '\r')"
  if ((ttl <= 0)); then
    echo "Recovered Redis Cluster did not persist key TTL" >&2
    exit 1
  fi
  redis_cli -c -p 7002 DEL "$test_key" >/dev/null

  # Only living nodes are used as new-client seeds after the failed master.
  export REDIS_TOPOLOGY_HOSTS="127.0.0.1:7002,127.0.0.1:7003,127.0.0.1:$old_replica_port"
  for file in \
    peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisTopologyServerIntegrationTest.xml \
    peach-middleware/peach-redis/peach-redis-tool-quickstart/target/surefire-reports/TEST-com.peach.redis.tool.quickstart.RedisToolTopologyServerIntegrationTest.xml \
    peach-middleware/peach-redission/peach-redisson-quickstart/target/surefire-reports/TEST-com.peach.redisson.quickstart.RedissonTopologyServerIntegrationTest.xml \
    peach-middleware/peach-satoken/peach-satoken-quickstart/target/surefire-reports/TEST-com.peach.satoken.quickstart.SaTokenSearchTopologyServerIntegrationTest.xml; do
    rm -f "$file"
  done
  echo "Rerunning all independent client and Sa-Token suites after Redis Cluster promotion"
  mvn -B -ntp -s scripts/ci/github-actions-maven-settings.xml \
    -pl peach-middleware/peach-redis/peach-redis-quickstart,peach-middleware/peach-redis/peach-redis-tool-quickstart,peach-middleware/peach-redission/peach-redisson-quickstart,peach-middleware/peach-satoken/peach-satoken-quickstart \
    -am \
    -Dtest=RedisTopologyServerIntegrationTest,RedisToolTopologyServerIntegrationTest,RedissonTopologyServerIntegrationTest,SaTokenSearchTopologyServerIntegrationTest \
    -Dsurefire.failIfNoSpecifiedTests=false test

  python3 - <<'PY'
from pathlib import Path
import xml.etree.ElementTree as ET

reports = [
    Path("peach-middleware/peach-redis/peach-redis-quickstart/target/surefire-reports/TEST-com.peach.redis.quickstart.RedisTopologyServerIntegrationTest.xml"),
    Path("peach-middleware/peach-redis/peach-redis-tool-quickstart/target/surefire-reports/TEST-com.peach.redis.tool.quickstart.RedisToolTopologyServerIntegrationTest.xml"),
    Path("peach-middleware/peach-redission/peach-redisson-quickstart/target/surefire-reports/TEST-com.peach.redisson.quickstart.RedissonTopologyServerIntegrationTest.xml"),
    Path("peach-middleware/peach-satoken/peach-satoken-quickstart/target/surefire-reports/TEST-com.peach.satoken.quickstart.SaTokenSearchTopologyServerIntegrationTest.xml"),
]
for report in reports:
    if not report.exists():
        raise SystemExit(f"Missing post-promotion Redis Cluster suite: {report}")
    suite = ET.parse(report).getroot()
    result = {key: int(suite.get(key, "0")) for key in ("tests", "failures", "errors", "skipped")}
    expected = 2 if "RedisToolTopologyServerIntegrationTest" in str(report) else 1
    if result["tests"] != expected or any(result[key] for key in ("failures", "errors", "skipped")):
        raise SystemExit(f"Post-promotion suite did not execute successfully: {report} {result}")
    print(f"PASS after Redis Cluster replica promotion: {report}")
PY
fi
