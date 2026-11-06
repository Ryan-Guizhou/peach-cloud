# Gateway rate-limit performance and operations verification

This directory contains repeatable load-test tooling, Prometheus alert rules, and a Grafana dashboard for the Gateway rate-limit engine.

## What CI verifies

| Area | CI verification |
| --- | --- |
| Six Lua algorithms | Testcontainers Redis 7.4 Standalone integration tests |
| Independent Redis clients | Two Lettuce clients share one Redis quota |
| 10,000 in-flight Redis commands | Two Lettuce clients issue 10,000 concurrent Lua checks; CI logs achieved RPS and p95/p99 client latency |
| Gateway enforcement instances | Two separately constructed enforcement services share one Redis quota |
| Nacos delivery | Testcontainers Nacos Server receives and pushes a configuration update |
| Redis Cluster / Sentinel / TLS | Opt-in topology smoke test; not run in default CI |
| 1k / 5k / 10k VU load and soak | Manual, because throughput and tail latency depend on the deployment hardware and network |

Do not report Cluster, Sentinel, TLS, or high-concurrency performance as verified unless the corresponding opt-in test or load profile has actually run.

## k6 profiles

Prerequisites:

- A deployed Gateway with the intended rate-limit rule enabled.
- A separate load-generator host/container. Do not run the generator on the Gateway host for capacity measurements.
- k6 installed.
- For authenticated tenant scenarios, a JSON file containing valid Sa-Token values for distinct tenants. Keep that file outside Git.

The script is `rate-limit-k6.js`. It records 2xx responses, 429 rejections, 503 unavailable responses, and unexpected responses as separate custom counters. Built-in k6 HTTP metrics provide throughput and latency percentiles.

### Single hot key

All virtual users call the same path. This is useful for GLOBAL and single-tenant hot-key contention.

```bash
k6 run \
  --vus 1000 --duration 5m \
  --summary-export results/global-hot-1k.json \
  -e BASE_URL=https://gateway.example.com \
  -e PATH_TEMPLATE=/api/orders \
  -e SCENARIO=global-hot-1k \
  rate-limit-k6.js
```

Repeat with `--vus 5000` and `--vus 10000`.

### Distributed URI keys

Use a path template with `{vu}` to create distinct URI keys. The rule must use a URI-aware scope such as URI or IP_URI.

```bash
k6 run \
  --vus 5000 --duration 5m \
  --summary-export results/uri-distributed-5k.json \
  -e BASE_URL=https://gateway.example.com \
  -e PATH_TEMPLATE='/api/orders/{vu}' \
  -e SCENARIO=uri-distributed-5k \
  rate-limit-k6.js
```

### Tenant token pool

Create a local JSON file containing valid tokens belonging to distinct test tenants, for example:

```json
[
  "token-for-tenant-a",
  "token-for-tenant-b"
]
```

Then run:

```bash
k6 run \
  --vus 5000 --duration 5m \
  --summary-export results/tenant-distributed-5k.json \
  -e BASE_URL=https://gateway.example.com \
  -e PATH_TEMPLATE=/api/orders \
  -e AUTH_TOKENS_FILE=./secrets/test-tenant-tokens.json \
  -e AUTH_HEADER=Authorization \
  -e SCENARIO=tenant-distributed-5k \
  rate-limit-k6.js
```

Use the token header expected by the deployed Sa-Token configuration. Never commit token files or place production credentials in load-test artifacts.

### Sustained soak

Run the target concurrency for at least 30 minutes after a warm-up period:

```bash
k6 run \
  --vus 10000 --duration 30m \
  --summary-export results/soak-10k-30m.json \
  -e BASE_URL=https://gateway.example.com \
  -e PATH_TEMPLATE=/api/orders \
  -e SCENARIO=soak-10k-30m \
  rate-limit-k6.js
```

A 10,000 VU profile is not equivalent to 10,000 RPS. Record actual achieved RPS and latency from the result.

## Required measurements

For each run, record:

- Gateway instance count, CPU/memory limits, JVM, Java version, and container/runtime version.
- Redis topology, node count, CPU/memory, persistence settings, and network placement.
- Load-generator host CPU/memory/network.
- Algorithm, scope, rule count, hot/distributed key model, and configured limit.
- Achieved RPS, p50/p90/p95/p99/max HTTP latency.
- 2xx, 429, 503, and unexpected response counts.
- Redis CPU, memory, connected clients, command latency, and slowlog.
- Gateway CPU, heap, GC pauses, Netty EventLoop latency, and process restarts.
- Rate-limit Lua execution p95/p99 and rule-matching count.

Use [result-template.md](result-template.md) for every recorded run. Do not invent a pass threshold before collecting a baseline on the target deployment environment.

## Prometheus queries

The Micrometer timer publishes percentile histogram buckets. Prometheus names are normalized from the dotted Micrometer names.

**Decision rate by outcome**

```promql
sum by (scope, algorithm, outcome) (
  rate(peach_gateway_rate_limit_requests_total[5m])
)
```

**Execution latency p95 / p99**

```promql
histogram_quantile(
  0.95,
  sum by (le, scope, algorithm) (
    rate(peach_gateway_rate_limit_execution_seconds_bucket[5m])
  )
)
```

Change `0.95` to `0.99` for p99.

**Script readiness**

```promql
peach_gateway_rate_limit_scripts_ready
```

**Script reload failures**

```promql
increase(peach_gateway_rate_limit_scripts_reloads_total{outcome="failure"}[5m])
```

**Redis execution errors**

```promql
sum by (scope, algorithm) (
  rate(peach_gateway_rate_limit_requests_total{outcome="error"}[5m])
)
```

Import [grafana-gateway-rate-limit.json](grafana-gateway-rate-limit.json) into Grafana and select the Prometheus datasource. Load [prometheus-gateway-rate-limit-rules.yml](prometheus-gateway-rate-limit-rules.yml) into the Prometheus rule directory. The supplied alerts cover script readiness, reload failures, and Redis execution errors; rejection-rate thresholds remain deployment-specific.

## Redis topology smoke test

The opt-in JUnit test `RedisTopologySmokeTest` executes all six Lua scripts against an externally supplied Redis topology. It is skipped when `PEACH_REDIS_MODE` is absent.

Run it from the gateway Maven reactor:

```bash
mvn -pl peach-gateway/peach-gateway-core -am \
  -Dtest=RedisTopologySmokeTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Environment variables:

| Variable | Purpose |
| --- | --- |
| `PEACH_REDIS_MODE` | `standalone`, `cluster`, or `sentinel` |
| `PEACH_REDIS_HOST` / `PEACH_REDIS_PORT` | Standalone endpoint; defaults to 127.0.0.1:6379 |
| `PEACH_REDIS_NODES` | Comma-separated Cluster seed nodes |
| `PEACH_REDIS_SENTINEL_MASTER` | Sentinel master name |
| `PEACH_REDIS_SENTINELS` | Comma-separated Sentinel host:port values |
| `PEACH_REDIS_PASSWORD` | Redis password, if configured |
| `PEACH_REDIS_SENTINEL_PASSWORD` | Sentinel password, if configured |
| `PEACH_REDIS_SSL` | Set to `true` to enable TLS |

For TLS, configure the JVM truststore to trust the Redis server certificate before running the test. The test confirms connection and Lua behavior; it does not replace failover, certificate rotation, or production load testing.

### Reproducible local Redis topologies

The compose fixtures are in [redis-topologies/](redis-topologies/). They are manual validation environments, not part of the default CI matrix.

Start one topology at a time:

```bash
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile cluster up -d
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile sentinel up -d
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile tls up -d
```

Run the Maven smoke test from a container attached to the matching network so Redis Cluster redirects and Sentinel-discovered addresses resolve correctly.

**Cluster**

```bash
docker run --rm --network peach-gateway-rate-limit-cluster \
  -v "$PWD":/workspace -w /workspace \
  -e PEACH_REDIS_MODE=cluster \
  -e PEACH_REDIS_NODES=redis-cluster-1:6379,redis-cluster-2:6379,redis-cluster-3:6379 \
  maven:3.9.11-eclipse-temurin-21 \
  mvn -pl peach-gateway/peach-gateway-core -am -Dtest=RedisTopologySmokeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**Sentinel**

```bash
docker run --rm --network peach-gateway-rate-limit-sentinel \
  -v "$PWD":/workspace -w /workspace \
  -e PEACH_REDIS_MODE=sentinel \
  -e PEACH_REDIS_SENTINEL_MASTER=peach-master \
  -e PEACH_REDIS_SENTINELS=redis-sentinel-1:26379,redis-sentinel-2:26379,redis-sentinel-3:26379 \
  maven:3.9.11-eclipse-temurin-21 \
  mvn -pl peach-gateway/peach-gateway-core -am -Dtest=RedisTopologySmokeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

**TLS**

After starting the TLS profile, export the generated test certificate and create a JVM truststore:

```bash
TLS_CONTAINER=$(docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile tls ps -q redis-tls)
docker cp "$TLS_CONTAINER":/tls/redis.crt ./redis.crt
keytool -importcert -noprompt -alias peach-redis -file redis.crt \
  -keystore redis-truststore.p12 -storetype PKCS12 -storepass changeit
```

Run the smoke test on the TLS network with the truststore mounted:

```bash
docker run --rm --network peach-gateway-rate-limit-tls \
  -v "$PWD":/workspace -w /workspace \
  -v "$PWD/redis-truststore.p12":/tmp/redis-truststore.p12:ro \
  -e JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/tmp/redis-truststore.p12 -Djavax.net.ssl.trustStorePassword=changeit' \
  -e PEACH_REDIS_MODE=standalone -e PEACH_REDIS_HOST=redis-tls -e PEACH_REDIS_PORT=6379 \
  -e PEACH_REDIS_SSL=true \
  maven:3.9.11-eclipse-temurin-21 \
  mvn -pl peach-gateway/peach-gateway-core -am -Dtest=RedisTopologySmokeTest -Dsurefire.failIfNoSpecifiedTests=false test
```

These fixtures use unauthenticated test-only Redis nodes and self-signed TLS. Do not reuse them as production security configuration. Clean up only after collecting logs and results:

```bash
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile cluster down -v
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile sentinel down -v
docker compose -f peach-gateway/docs/performance/redis-topologies/docker-compose.yml --profile tls down -v
```
