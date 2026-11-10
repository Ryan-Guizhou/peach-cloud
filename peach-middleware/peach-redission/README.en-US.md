# peach-redission

English | [中文](README.md)

`peach-redission` (historical artifact/directory spelling retained for compatibility) provides four independent Redisson-based starters: distributed lock, delay queue, bloom filter and repeat guard. Shared Redisson foundations live in `peach-redission-common`.

## Purpose and compatibility

**Redisson is a Redis client with higher-level distributed data structures and coordination primitives.** The project's `peach-redission` is a Maven aggregator for locks, duplicate submission protection, delayed queues and segmented bloom filters. Choose the feature Starter you need. The historical directory/artifact and some Java package spellings use `redission`; **this is not a separate client product**.

**Current baseline:** Java 21 / Spring Boot 3.5.4 / Redisson 3.50.0. New `peach-redisson-autoconfigure` and `peach-redisson-starter` allow Redisson-only services without `peach-redis-common` or Jedis. When legacy `RedisConfig` is present, the new configuration backs off from its existing `RedissonClient`; migrating the old mixed configuration is a follow-up stage. See [Issue #18](https://github.com/Ryan-Guizhou/peach-cloud/issues/18).

## Sentinel ACL and Cluster safeguards (Issue #49)

The independent `peach-redisson-starter` retains `peach.redis.mode / host / username / password` and adds optional `peach.redis.sentinel-username` and `peach.redis.sentinel-password` for Sentinel-specific credentials. Leaving these blank preserves existing behavior. Redisson TLS continues to use `peach.redis.redisson.ssl-enable`, independently of Jedis `peach.redis.ssl-enabled`. A nonzero database in Cluster mode fails at startup rather than silently accessing database zero. Production ACL, certificates and failover still require separate acceptance. The two Nacos templates share the same names and must never embed real credentials.

## Complete capability matrix

| Capability / starter | Public API | Current functionality | Example use | Current limitation |
| --- | --- | --- | --- | --- |
| Redisson client foundation / `peach-redisson-starter` | `RedissonClient` / `PeachRedissonAutoConfiguration` | Independent single/Sentinel/Cluster, SSL, pools, custom-bean backoff | Redisson-only services | Preserves `peach.redis.*` and JsonJacksonCodec; production TLS/topology integration validation is still required |
| Shared support / `peach-redission-common` | `RedissionDataHandle`, `LockInfoHandleFactory`, `LocalCacheLock` | Redisson KV wrapper, lock-key resolution, local mutex support | Internal shared lock/repeat support | Does not create clients; TimeUnit precision and held-lock lifetime fixed; 256 local lock stripes may contend across keys |
| `peach-redission-distributedlock-starter` | `@DistrbutedLock`, `DistributedLockTemplate`, `DistributedLocker` | Reentrant, fair, read/write lock strategies; annotation or programmatic locking; wait timeout | Inventory, account state, scheduler coordination | Historic annotation spelling; distinguish watchdog and explicit lease; retain database guards for critical operations |
| `peach-redission-repeat-starter` | `@RepeatLimit` | Restrict concurrent submissions per business key, or reject repeats for a post-success window | Checkout, form submit, duplicate execution | Failure to persist a success marker raises an uncertain-result error; verify durable business state before retry |
| `peach-redission-delayqueue-starter` | `DelayQueueContext`, `ConsumerTask`, `RedisDelayQueue` | One reliable Lua-backed queue: atomic claims, heartbeats, fenced ACK, bounded retries, DLQ and replay; isolated topic partitions | Order expiry and delayed business jobs | Legacy Redisson queue messages must be drained before switching; at-least-once, not exactly-once |
| `peach-redission-bloomfilter-starter` | `BloomFilterService`, `BloomStatus`, `KeyNamingStrategy` / `BloomScalePolicy` / `CodecProvider` SPIs | Membership pre-check, batch add/query, segmented scaling, status/clear, extension SPI | SKU and content prefilters | Counters are per segment; old counters are lazily approximated from filter bits; segment lists are loaded from Redis to avoid stale cross-node negatives; authoritative lookup remains required |

### Which module should I use?

- **Lock:** use `DistributedLockTemplate` or annotation for resources shared across service instances; business side effects still need idempotency or version checks.
- **Duplicate submit:** `@RepeatLimit` rejects repeated requests in a window; payment/order creation also requires uniqueness and durable idempotency.
- **Delayed tasks:** `DelayQueueContext` uses the single reliable queue. Use the stable message-ID overload to reject duplicates; drain the historical Redisson queue before a deployment cutover.
- **Segment metadata migration:** New counters include a segment identifier; the old shared counter remains for rollback. On first access an old segment's count is estimated with `count()` (approximate), and segment names are no longer cached as an uninvalidated local snapshot. Validate mixed-version rollout and rollback.
- **Membership prefilter:** Bloom filters reduce unnecessary lookups, but positive results require authoritative lookup; errors reading Redis metadata must not mean “definitely absent”.
- **RedisTemplate / Stream / Caffeine + Redis caching:** use [peach-redis](../peach-redis/README.en-US.md).

### Minimal Maven dependency (current repository)

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-distributedlock-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

Lock, repeat guard, bloom, and delay queue Starters now transitively include `peach-redisson-starter`, without requiring explicit `peach-redis-common` or Jedis. The new auto-configuration backs off when the user supplies `RedissonClient`. Existing applications may retain the legacy `RedisConfig` compatibility path. Use an actual published version outside the Maven reactor.

### Minimal development configuration

```yaml
peach:
  redis:
    mode: standalone
    host: "${REDIS_HOST:127.0.0.1:6379}"
    password: "${REDIS_PASSWORD:}"
    database: 0
    redisson:
      enabled: true
```

The new Redisson-only configuration and legacy RedisConfig retain the same `peach.redis.*` topology fields and the default JsonJacksonCodec. Keep passwords out of logs and independently validate TLS/Sentinel/Cluster in the intended Redis deployment.

### Minimal programmatic lock example

```java
@Service
@RequiredArgsConstructor
public class InventoryService {
    private final DistributedLockTemplate lockTemplate;

    public void updateStock(String sku, Runnable action) {
        lockTemplate.run("stock", sku, action);
    }
}
```

Lock names follow current business naming rules; do not change keys during rolling upgrades or old/new instances may no longer coordinate. This example does not replace database transactions or durable consistency.

### Reliability and data migration boundaries

| Situation | Current interpretation |
| --- | --- |
| Redis disconnect / lock acquisition failure | Stop critical unprotected actions; do not silently downgrade to JVM-local locking |
| Failure to persist duplicate guard | Business work may have succeeded without the guard; investigate and enforce business idempotency |
| Delayed-queue worker crash | The current Lua queue supports lease-based recovery and fenced ACK; historic Redisson queue data must be drained before cutover |
| Bloom segment metadata unavailable | Do not use a negative response for irreversible business decisions; query the source of truth |
| Changing lock keys, codec or message payloads | Validate stored data, queue state, mixed-version rollout and rollback before deployment |


## Redisson dependency-closure cleanup (Issue #43)

`peach-redission-common`, Bloom, DistributedLock and DelayQueue autoconfigure modules no longer declare optional `peach-redis-common`. Feature Starters use `peach-redisson-starter`. Legacy `RedisConfig` stays available for existing installations, but is no longer a new Starter dependency.

## Structure

**Dependency model:** each feature Starter → `peach-redisson-starter` → `peach-redisson-autoconfigure` → RedissonClient. `peach-redission-common` remains the historical shared lock naming / utility module, not an independent client factory.


| Capability | Autoconfigure | Starter | Quickstart |
| --- | --- | --- | --- |
| Distributed Lock | `peach-redission-distributedlock-autoconfigure` | `peach-redission-distributedlock-starter` | `peach-redission-distributedlock-quickstart` |
| Delay Queue | `peach-redission-delayqueue-autoconfigure` | `peach-redission-delayqueue-starter` | `peach-redission-delayqueue-quickstart` |
| Bloom Filter | `peach-redission-bloomfilter-autoconfigure` | `peach-redission-bloomfilter-starter` | `peach-redission-bloomfilter-quickstart` |
| Repeat Guard | `peach-redission-repeat-autoconfigure` | `peach-redission-repeat-starter` | `peach-redission-repeat-quickstart` |

```mermaid
flowchart LR
    App[Business Service] --> Lock[Lock Starter]
    App --> Queue[DelayQueue Starter]
    App --> Bloom[BloomFilter Starter]
    App --> Repeat[Repeat Starter]
    Lock --> Redisson[(Redisson)]
    Queue --> Redisson
    Bloom --> Redisson
    Repeat --> Redisson
```

## Quick Start

Each capability ships a non-web quickstart (`spring.main.web-application-type=none`). The four capability Quickstarts no longer explicitly depend on `peach-redis-common`; their feature Starters now pull in the Redisson client foundation. Provide a development Redis for local demos, or run the Testcontainers IT suite (`redis:7.2-alpine`).

### Distributed Lock

- Module: [`peach-redission-distributedlock-quickstart`](./peach-redission-distributedlock-quickstart/)
- Dependency: `peach-redission-distributedlock-starter` (transitively brings `peach-redisson-starter`, `peach-redission-common`, AOP and Caffeine; older Quickstart duplicates may be cleaned separately)
- Capability samples (inventory deduct):
  - **Annotation**: `@DistrbutedLock` protects stock deduct
  - **Template**: inject `DistributedLockTemplate` for programmatic deduct
  - **Concurrency proof**: two threads compete with short `waitTime` (1 success / 1 failure)
- `DistributedLockDemoRunner` runs on startup; disable with `quickstart.distributedlock.demo.enabled=false`
- Run: `mvn -pl peach-middleware/peach-redission/peach-redission-distributedlock-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redission/peach-redission-distributedlock-quickstart -am test`

### Reliable delay queue (current implementation)

The current `RedisDelayQueue` runtime preserves the historical `peach:dq2:{topic}:*` key namespace so every key for a topic shares one Redis Cluster hash slot. Nine Lua scripts live in `src/main/resources/lua/delayqueue/`, not embedded in Java.

**State machine:** `scheduled → processing (lease + fencing token) → completed`; failures return to `scheduled` for retry or enter `DLQ`. Expired leases are atomically reclaimed. Enqueue requires a stable `messageId`; claims allocate monotonically increasing fencing tokens and only the current token can ACK or renew a lease. **At-least-once, not exactly-once**: business systems must enforce durable idempotency and fencing independently. Active callbacks get heartbeat lease extensions. Scheduled polls and business callbacks run on separate bounded executors; no message is claimed without an available execution slot. Failures may still cause duplicate execution.

```yaml
peach:
  delay:
    queue:
      enabled: true
      isolation-region-count: 5
      worker-count: 4
      batch-size: 16
      max-attempts: 5
      poll-interval: 500ms
      lease: 2m
      retry-initial: 5s
      retry-max: 5m
      completed-retention: 7d
      max-payload-bytes: 65536
```

```java
@Service
@RequiredArgsConstructor
public class OrderExpiryService {
    private final DelayQueueContext delayQueue;

    public boolean schedule(String orderId) {
        return delayQueue.sendMessage("order-expiry", orderId, orderId, 30, TimeUnit.MINUTES);
    }
}
```

Declare a Spring `ConsumerTask` bean with `topic() == "order-expiry"` to auto-subscribe all configured partitions. The stable ID is used for duplicate suppression; the original four-argument `sendMessage` remains available, but assigns a random ID for each call. Direct `RedisDelayQueue` calls operate on a physical partition/topic and require explicit `start` or `pollOnce`.

**Migration and acceptance:** Call `RedisDelayQueue.legacyBacklog(physicalTopic)` to inspect delayed, ready, processing and dead-letter counts without deleting records. Direct `offer`, `claim` and `start` now perform a fail-closed check on first use of each physical topic (and `start` rechecks); this is not a live write fence. Startup checks the old delayed/ready/processing/dead-letter data for registered consumer partitions and refuses to switch when pending records exist. Producers also check all partitions of a business topic on first send. This is a read-only fail-closed guard, **not** an atomic fence against old producers; stop all old producers before deploying. Before switching deployments, stop historical queue producers, drain delayed/ready/processing items, then stop historical consumers and start the unified runtime. The old Redisson delayed-queue protocol is not automatically converted and must not run alongside this runtime. Existing Lua state keys stay unchanged to preserve already queued reliable messages. Validate ACK, retry, DLQ, delayed messages and rollback in isolation. Business idempotency remains mandatory.


- Module: [`peach-redission-delayqueue-quickstart`](./peach-redission-delayqueue-quickstart/)
- Capability samples (order timeout):
  - **Single message**: `DelayQueueContext#sendMessage` with short delay + implement `ConsumerTask` to confirm consume
  - **Multi message**: under reliable-queue config, send several messages and confirm all are consumed
- `DelayQueueDemoRunner` runs on startup; disable with `quickstart.delayqueue.demo.enabled=false`
- Minimal config: `peach.delay.queue.*` (example uses `isolation-region-count=1`, `enabled=true`) plus `peach.redis.*`; consumer bootstrap needs `peach-initialize-starter`
- Run: `mvn -pl peach-middleware/peach-redission/peach-redission-delayqueue-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redission/peach-redission-delayqueue-quickstart -am test`

### Bloom Filter

- Module: [`peach-redission-bloomfilter-quickstart`](./peach-redission-bloomfilter-quickstart/)
- Capability samples (inject `BloomFilterService`, product SKU pre-filter):
  - **Membership**: `initNamespace` + `add` + `mightContain` (present / absent)
  - **Batch**: `addAll` / `mightContainAll`
  - **Status and clear**: `status` / `segments` / `clear`
- `BloomFilterDemoRunner` runs on startup; disable with `quickstart.bloomfilter.demo.enabled=false`
- Minimal config: `peach.redis.bloom.enabled=true` (enabled by default) plus capacity/FPP settings, and `peach.redis.*`
- Run: `mvn -pl peach-middleware/peach-redission/peach-redission-bloomfilter-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redission/peach-redission-bloomfilter-quickstart -am test`

### Repeat Guard

- Module: [`peach-redission-repeat-quickstart`](./peach-redission-repeat-quickstart/)
- Dependency: `peach-redission-repeat-starter` (transitively brings distributed locks and Redisson foundation; `peach-redis-common` is not required)
- Capability samples (order submit, public API is `@RepeatLimit`):
  - **Same-key reject**: second submit with the same requestId throws `IllegalStateException`
  - **Different-key isolation**: different requestIds do not interfere
- `RepeatDemoRunner` runs on startup; disable with `quickstart.repeat.demo.enabled=false`
- Run: `mvn -pl peach-middleware/peach-redission/peach-redission-repeat-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redission/peach-redission-repeat-quickstart -am test`

`peach.redis.host` uses `host:port`. Quickstarts share defaults: `password` is empty by default (override via `PEACH_REDIS_PASSWORD`), `database=1`.

## Repeat guard and lock lifecycle fixes

- `RedissionDataHandle#getDuration(long, TimeUnit)` now preserves `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS` and other units instead of silently treating subsecond durations as seconds.
- `LocalCacheLock` retains `getLock(key, fair)` but uses 256 bounded fair lock stripes, avoiding expiration of held lock objects. Unrelated keys can contend on the same stripe.
- `@RepeatLimit` now uses only the distributed Redisson lock for cross-instance exclusion. If the success marker cannot be persisted **after the business action succeeds**, the caller receives an explicit uncertain-result exception rather than false success. **Do not retry blindly**; query the durable order/idempotency state first.

## Real Redis topology integration (Issue #47)

The dedicated Redis 7.2 CI matrix exercises the independent `peach-redisson-starter` against Sentinel, Cluster (three masters, three replicas) and TLS. It verifies RedissonClient bean isolation, real Bucket writes/reads, TTL and deletion alongside Jedis and Redis Tool. See the [Redis topology integration instructions](../peach-redis/README.en-US.md#real-redis-topology-integration-issue-47) for commands, enforced non-skipped Surefire checks and limitations. This smoke test does **not** prove distributed-lock mutual exclusion during failover, exactly-once for delay queue reliable queue, or cross-instance Bloom behavior.

## Bloom segment test with two independent clients (Issue #55)

`BloomTwoClientServerIntegrationTest` uses real Redis 7.2 and two independent `RedissonClient` instances. Node B reads the old segment directory before node A expands; after expansion, B must discover the new segment, find the inserted elements and report independent per-segment counts. Bloom filters still have false positives and provide only a pre-filter. This test does not establish failover safety or strong consistency under very high concurrency.

## Independent worker recovery and bounded concurrency (Issue #59)

`DelayQueueServerIntegrationTest` adds real Redis 7.2 tests with two distinct RedissonClient / `RedisDelayQueue` instances. Worker A claims but stops without ACK; after the lease expires, Worker B reclaims it, obtains a greater fencing token, rejects the old token and acknowledges as the current owner. Two independent workers also consume 64 messages in parallel, checking stable-ID deduplication, completion counts and no remaining processing records. Dedicated CI verifies that all eight test cases actually execute via Surefire XML.

**Scope:** One earlier scenario uses graceful stop to simulate lease loss; a separate test force-kills an independent JVM and verifies reclaim/fencing. Redis master failover, cross-store transaction fencing, 10k-concurrency soak, legacy queue drain and mixed-version upgrades remain separate acceptance work. The current queue defaults to `peach.delay.queue.enabled=true` when the queue Starter is installed; disable it explicitly until historical tasks are drained. Lua/Redis keys and business-side durable idempotency requirements remain unchanged.

## Rollout, acceptance and rollback

For migration steps, Nacos/key/codec/session compatibility checks, CI evidence, failure boundaries and rollback gates, see the [Redis / Redisson migration acceptance runbook](../../docs/redis-redisson/acceptance-and-rollout.en-US.md). Real connectivity smoke tests do not establish production failover safety.

## Long-lived Sentinel client failover (Issue #69)

Dedicated CI exercises the **same JedisConnectionFactory and RedissonClient instances** before and after actually stopping the isolated Redis Sentinel master container. Without recreating those clients it retries bounded SET/GET/TTL, then checks all three Sentinels agree on the promoted master and reruns the new-client suites. This only uses Redis 7.2 CI containers, not production systems. Passing it validates basic client recovery, **not** in-flight business transaction outcomes, Stream/DelayQueue idempotency, historic Sessions or Redis Cluster failure.

## Redis Cluster master failure injection (Issue #73)

Against an isolated three-master/three-replica Redis 7.2 Cluster, CI stops master port 7001 after healthy topology tests, requires promotion of its original replica, three live masters and `cluster_state:ok`, then performs real redirected SET/GET/TTL and reruns all four independent Jedis, Redisson, Redis Tool and Sa-Token new-client suites. This validates **new connections after promotion**, not recovery of long-lived Cluster clients, in-flight messages, cross-AZ failover or load testing. See [Issue #73](https://github.com/Ryan-Guizhou/peach-cloud/issues/73) for CI evidence.

## Real Redis ACL positive/negative tests (Issue #75)

A dedicated Redis 7.2 CI standalone topology disables the default user and uses an ephemeral randomly generated password for a named ACL account. Independent Jedis, Redisson, Redis Tool and Sa-Token must perform real writes/reads/TTL successfully; anonymous access and an invalid password must be rejected. The temporary password is generated only inside GitHub Actions, never stored in source or Nacos. This proves **single-node data ACL authentication**, not live Sentinel ACL credential rotation, production secret management, mTLS or minimum-privilege policy review.

## Real independent-JVM crash recovery (Issue #77)

A Redis 7.2 integration test injects a real worker-process failure: a separate JVM Redisson worker claims a message and writes a ready marker, then the parent calls `Process.destroyForcibly()` so that **the process cannot gracefully ACK or close the queue**. After the lease expires, another RedissonClient reclaims the same message ID/payload with a greater fencing token, rejects the stale token and acknowledges the recovered message without processing residue. Delivery remains **at least once**; durable business-side idempotency and external-store fencing are still mandatory. This is not a Redis master failure or cross-store atomicity test.

## Boundaries

- Distributed locks protect the smallest critical section and do not replace database uniqueness, transactions or state validation.
- Delay-queue consumers handle duplicates, retry and business idempotency.
- A bloom-filter "might exist" result requires authoritative lookup and cannot decide authorization, balance or uniqueness by itself.
- Repeat-guard keys come from stable business semantics and do not replace real idempotency design.
- Historical `redission` spelling is compatibility-only and is not a naming template for new modules.

### Lock and repeat annotation scope

`@DistrbutedLock` and `@RepeatLimit` support both class-level and method-level declarations. Method-level configuration takes precedence and each proxied invocation runs one advice. Class-level annotations now affect public methods invoked through Spring AOP proxies; audit existing class annotations before rolling out to avoid unexpected serialization or rejection. Self-invocation still bypasses Spring AOP. `DistributedLocker#getLock` returns a lock handle without acquiring it; use `lock` or `tryLock` explicitly. Interrupted lock attempts return `false` while preserving the thread's interruption flag.
