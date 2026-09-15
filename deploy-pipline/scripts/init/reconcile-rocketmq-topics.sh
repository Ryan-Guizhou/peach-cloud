#!/usr/bin/env sh
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT=$(CDPATH= cd -- "$SCRIPT_DIR/../.." && pwd)
ENV_FILE=${PEACH_ENV_FILE:-$ROOT/runtime/generated/deploy.env}
TOPICS_FILE=${ROCKETMQ_TOPICS_FILE:-$ROOT/config/rocketmq/topics.json}
REPORT_DIR="$ROOT/runtime/reports"; REPORT_FILE="$REPORT_DIR/rocketmq-status.txt"
[ -s "$ENV_FILE" ] || { echo "Missing runtime env: $ENV_FILE" >&2; exit 1; }; [ -s "$TOPICS_FILE" ] || { echo "Missing RocketMQ topic catalog: $TOPICS_FILE" >&2; exit 1; }
set -a; . "$ENV_FILE"; set +a
mode=${ROCKETMQ_TOPIC_PROVISION_MODE:-ensure}; case "$mode" in ensure|verify|off) ;; *) echo "Unsupported ROCKETMQ_TOPIC_PROVISION_MODE: $mode" >&2; exit 2 ;; esac
mkdir -p "$REPORT_DIR"; tmp_topics="$REPORT_DIR/.rocketmq-topics.tmp"; trap 'rm -f "$tmp_topics"' EXIT
mqadmin() { docker exec peach-rocketmq-broker sh mqadmin "$@"; }
name_server=${ROCKETMQ_NAME_SERVER:-rocketmq-namesrv:9876}
local_broker=127.0.0.1:10911
broker_topics=/home/rocketmq/store/config/topics.json
topic_exists_on_broker() { docker exec peach-rocketmq-broker sh -c "test -s '$broker_topics' && grep -Fq '\"topicName\":\"$1\"' '$broker_topics'"; }
wait_topic_on_broker() { topic=$1; i=0; while [ "$i" -lt 15 ]; do topic_exists_on_broker "$topic" && return 0; i=$((i + 1)); sleep 1; done; return 1; }
mqadmin clusterList -n "$name_server" >/dev/null
mqadmin topicList -n "$name_server" | sed '/^[[:space:]]*$/d' | sort -u > "$tmp_topics" || true
auto_create=$(docker exec peach-rocketmq-broker sh -c "sed -n 's/^[[:space:]]*autoCreateTopicEnable[[:space:]]*=[[:space:]]*//p' /home/rocketmq/rocketmq-*/conf/broker.conf | tail -n 1" 2>/dev/null || true); [ -n "$auto_create" ] || auto_create=unknown
expected_count=$(node -e 'const f=require(process.argv[1]); console.log((f.topics||[]).length)' "$TOPICS_FILE"); existing_count=$(wc -l < "$tmp_topics" | tr -d ' ')
{
 echo "================ RocketMQ ================="; echo "NameServer               : $name_server"; echo "Cluster                  : ${ROCKETMQ_CLUSTER_NAME:-PeachCluster}"; echo "Broker target            : $local_broker"; echo "Broker autoCreateTopic   : $auto_create"; echo "Peach topic auto-create  : ${PEACH_ROCKET_TOPIC_AUTO_CREATE:-false}"; echo "CI topic provisioning    : $mode"; echo "Expected Topics          : $expected_count"; echo "NameServer Topics        : $existing_count"; echo "";
} > "$REPORT_FILE"
node -e 'const c=require(process.argv[1]); for(const t of c.topics||[]) console.log([t.name,t.readQueueNums??8,t.writeQueueNums??8,t.perm??6].join("\t"));' "$TOPICS_FILE" | while IFS="$(printf '\t')" read -r topic read_queues write_queues perm; do
 [ -n "$topic" ] || continue
 if topic_exists_on_broker "$topic"; then echo "[rocketmq-topic] preserved: $topic" | tee -a "$REPORT_FILE"; continue; fi
 case "$mode" in
  ensure)
    echo "[rocketmq-topic] creating: $topic" | tee -a "$REPORT_FILE"
    mqadmin updateTopic -n "$name_server" -b "$local_broker" -t "$topic" -r "$read_queues" -w "$write_queues" -p "$perm" >/dev/null
    wait_topic_on_broker "$topic" || { echo "[rocketmq-topic] broker metadata not updated: $topic" | tee -a "$REPORT_FILE"; exit 1; }
    ;;
  verify) echo "[rocketmq-topic] missing: $topic" | tee -a "$REPORT_FILE" ;;
  off) echo "[rocketmq-topic] unmanaged/missing: $topic" | tee -a "$REPORT_FILE" ;;
 esac
done
missing_topics=$(node -e 'const f=require(process.argv[1]); for(const t of f.topics||[]) console.log(t.name)' "$TOPICS_FILE" | while IFS= read -r topic; do topic_exists_on_broker "$topic" || echo "$topic"; done)
if [ -n "$missing_topics" ]; then final_missing=$(printf '%s\n' "$missing_topics" | sed '/^[[:space:]]*$/d' | wc -l | tr -d ' '); else final_missing=0; fi
{
 echo ""; echo "Missing Topics after run : $final_missing"; if [ "$final_missing" -eq 0 ]; then echo "RocketMQ provisioning     : SUCCESS"; elif [ "$mode" = "off" ]; then echo "RocketMQ provisioning     : SKIPPED"; else echo "RocketMQ provisioning     : FAILED"; fi; echo "===========================================";
} >> "$REPORT_FILE"
cat "$REPORT_FILE"
if [ "$final_missing" -gt 0 ] && [ "$mode" != "off" ]; then exit 1; fi
