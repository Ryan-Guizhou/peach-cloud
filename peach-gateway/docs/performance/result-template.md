# Gateway rate-limit performance run

## Run metadata

| Field | Value |
| --- | --- |
| Date/time (timezone) | |
| Git commit | |
| Environment | |
| Gateway version / Java | |
| Gateway instance count | |
| Gateway CPU / memory limits | |
| Redis topology / version | |
| Redis node count / resources | |
| Load-generator host / resources | |
| k6 version | |
| Algorithm / scope | |
| Rule count / matched rules | |
| Configured rate / burst / window | |
| Key distribution | |
| Duration / VUs | |

## Results

| Metric | Result |
| --- | ---: |
| Achieved RPS | |
| HTTP p50 | |
| HTTP p90 | |
| HTTP p95 | |
| HTTP p99 | |
| HTTP max | |
| 2xx responses | |
| 429 responses | |
| 503 responses | |
| Unexpected 4xx / 5xx | |
| Lua execution p95 | |
| Lua execution p99 | |
| Redis CPU peak | |
| Redis memory peak | |
| Redis connected clients peak | |
| Redis command latency p95 / p99 | |
| Gateway CPU peak | |
| Gateway heap peak | |
| GC pause p95 / max | |
| Netty EventLoop lag p95 / p99 | |

## Fault and recovery scenarios

| Scenario | Expected behavior | Observed behavior | Evidence |
| --- | --- | --- | --- |
| Redis connection interruption | FAIL_OPEN/FAIL_CLOSED policy is honored | | |
| Redis recovery | Requests recover without Gateway restart | | |
| Nacos valid refresh under load | New snapshot applies atomically | | |
| Nacos invalid refresh under load | Last-known-good snapshot remains active | | |
| Lua script reload failure | Previous complete script snapshot remains active | | |
| Lua script reload recovery | New script snapshot becomes ready | | |

## Artifacts

- k6 summary JSON:
- k6 raw output:
- Prometheus range export:
- Grafana dashboard snapshot:
- Redis INFO / SLOWLOG:
- Gateway logs:

## Assessment

- Baseline comparison:
- Bottleneck:
- Regressions:
- Capacity recommendation:
- Known unverified areas:

Do not mark the run as passed solely because the test process exited successfully. Compare the measured latency, error rates, resource saturation, and quota accuracy with an agreed deployment baseline.
