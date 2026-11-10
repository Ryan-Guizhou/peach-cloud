# peach-redis

English | [中文](README.md)

`peach-redis` separates Redis capabilities into three independent starters: tools, multi-level cache and Stream. Stable shared contracts live in `peach-redis-common`.

## Purpose and compatibility

**Audience:** Spring Boot services needing Redis data structures, Caffeine L1 + Redis L2 caching, or Redis Stream messages. `peach-redis` is a Maven aggregator, **not** one dependency that enables every feature. Select only the needed starter.

**Current baseline:** Java 21 / Spring Boot 3.5.4. The legacy `peach-redis-common` still creates Jedis/templates **and** RedissonClient for compatibility. New Redisson-only services can now use `peach-redisson-starter` without this legacy common module. Migrating the Redis-side foundation remains a separate stage. This matrix describes what exists on main and the known limitations. See [Issue #18](https://github.com/Ryan-Guizhou/peach-cloud/issues/18) for the redesign.

## Complete capability matrix

| Module / starter | Main API | Capabilities | Use cases | Current limitation |
| --- | --- | --- | --- | --- |
| Connection foundation / `peach-redis-common` (current, scheduled for migration) | `RedisTemplate`, `StringRedisTemplate`, `RedisConnectionFactory` | Standalone, Sentinel, Cluster, Jedis pool, Jackson JSON serialization | Shared Redis access | Also configures Redisson today; nonzero Cluster database is unsupported; preserve stored formats |
| `peach-redis-tool-starter` | `RedisDao` | String, TTL, Hash, List, Set, ZSet, Pub/Sub and batch deletion | KV, counters, tags, session support | `scan()` supports standalone/Sentinel and scans all Cluster masters; `count` is a batch hint, not a result limit; result is memory-backed and not a consistent snapshot; cross-slot deletion is per-key |
| `peach-redis-multicache-starter` | `MultiCacheManager`, `MultiCacheManagerService`, Spring Cache | Caffeine L1 and Redis L2; TTL jitter; manual and annotation APIs; Pub/Sub invalidation | Product detail, configuration and dictionary caching | Best-effort invalidation, not strong consistency; `clear()` uses the restored Redis SCAN but remains memory-intensive and not an atomic snapshot; Caffeine STRONG reference semantics fixed; not strongly consistent across nodes |
| `peach-redis-stream-starter` | `RedisStreamPushHandler`, `RedisStreamHandler`, `MessageConsumer` | Produce Stream records, group/broadcast consumption, consumer callbacks | Lightweight event messages | Group consumers ACK only on success; failures remain in PEL, but automatic reclaim, retries and DLQ are not implemented |

### Which module should I use?

- **Key enumeration and cleanup:** `RedisDao#scan` / `deletePattern` traverse matching keys; Redis `COUNT` is a hint, not a result cap or a consistent snapshot. Use sparingly, preferably off the request path, and avoid huge keyspaces.
- **Redis structures:** Tool, or Spring `RedisTemplate` directly if no wrapper is needed.
- **Caffeine L1 behavior:** Strong references are now the default; explicit `WEAK` changes key reference semantics. `putIfAbsent` uses Redis atomic SET NX; cross-node invalidation still uses best-effort Pub/Sub.
- **Local cache acceleration:** MultiCache, with explicit TTL and tolerance for temporary staleness; never rely solely on L1 for real-time authorization.
- **Stream messaging:** Stream; current implementation requires application idempotency and separate recovery arrangements.
- **Distributed locks / bloom filters / delay queues / duplicate suppression:** use [peach-redission](../peach-redission/README.en-US.md), not the Redis Tool.
- **Gateway:** retain the standalone reactive Lettuce setup; avoid blocking Redis calls on WebFlux event-loop threads.

### Minimal Maven dependency (current release)

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-tool-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

`revision` resolves within the peach-cloud Maven reactor; use an actual published version outside it. **Starter dependency closure has not been fixed yet**: this single declaration does not guarantee a standalone consumer boots; existing Quickstarts declare explicit common/client dependencies. [Issue #18](https://github.com/Ryan-Guizhou/peach-cloud/issues/18) addresses that.

### Minimal configuration (current project convention)

```yaml
peach:
  redis:
    mode: standalone
    host: "${REDIS_HOST:127.0.0.1:6379}"
    password: "${REDIS_PASSWORD:}"
    database: 0
```

Development placeholders only; use a secure secret source in production. Current topology config uses `peach.redis.*`, not `spring.data.redis.*`. MultiCache uses `peach.multicache.*`, and Stream is enabled using **`peach.redis.stream.enable`**, not `enabled`.

### Tool usage example

```java
@Service
@RequiredArgsConstructor
public class ProfileCacheService {
    private final RedisDao redisDao;

    public void cache(String userId, String value) {
        redisDao.vSet("profile:" + userId, value, java.time.Duration.ofMinutes(5));
    }

    public Object find(String userId) {
        return redisDao.vGet("profile:" + userId);
    }
}
```

Uses the existing `RedisDao` API. Production code should standardize key naming, TTL and failure rules and avoid personal data in keys or logs.

### Failure and compatibility policy

| Situation | Recommendation |
| --- | --- |
| Redis unavailable | Ordinary caches may fall back to source; authentication, tokens and CAPTCHA must not be bypassed |
| Multicache subscription disconnected | Local data can become stale; bound L1 TTL; reconnect invalidation and cross-node consistency still need improvement |
| Stream handler fails | Group-mode failures stay pending; automatic PEL reclaim and bounded retry are not available yet. Application idempotency remains required |
| Changing Jackson or client driver | Test persisted JSON and existing keys first; do not silently change stored session format |


## Application dependency migration progress (Issue #41)

- **Sa-Token:** its autoconfiguration uses `RedisConnectionFactory`; the Starter transitively includes `peach-redis-starter` rather than the old `peach-redis-common`. Existing session JSON/key formats remain unchanged.
- **Captcha:** its autoconfiguration declares optional Spring Data Redis and Hutool compile dependencies; in-memory captcha storage remains usable independently.
- **Auth / Setting / Message:** service modules use the independent Redis Starter. The Auth launcher removes redundant direct Jedis, Redisson and Spring Data Redis dependencies provided by the Starters.
- **Still to verify:** existing Redis data/session compatibility, login/logout, gateway reactive Lettuce isolation, Nacos deployment profiles and Sentinel/Cluster/TLS rollout in the intended runtime. This batch does not delete the legacy `peach-redis-common`.

## Structure

| Capability | Autoconfigure | Starter | Quickstart |
| --- | --- | --- | --- |
| Tool | `peach-redis-tool-autoconfigure` | `peach-redis-tool-starter` | `peach-redis-tool-quickstart` |
| MultiCache | `peach-redis-multicache-autoconfigure` | `peach-redis-multicache-starter` | `peach-redis-multicache-quickstart` |
| Stream | `peach-redis-stream-autoconfigure` | `peach-redis-stream-starter` | `peach-redis-stream-quickstart` |

```mermaid
flowchart LR
    App[Business Service] --> Tool[Tool Starter]
    App --> Cache[MultiCache Starter]
    App --> Stream[Stream Starter]
    Tool --> Redis[(Redis)]
    Cache --> Redis
    Stream --> Redis
```

## Selection

- General Redis access: `peach-redis-tool-starter`.
- Local + Redis multi-level caching: `peach-redis-multicache-starter`.
- Redis Stream producer/consumer integration: `peach-redis-stream-starter`.

## Quick Start

Each capability ships a non-web quickstart (`spring.main.web-application-type=none`) that depends on the matching starter plus `peach-redis-common`, and verifies the minimal loop with Testcontainers Redis (`redis:7.2-alpine`). Provide a development Redis for local demos, or run the IT suite directly.

### Tool

- Module: [`peach-redis-tool-quickstart`](./peach-redis-tool-quickstart/)
- Capability samples (inject `RedisDao`):
  - **String + TTL**: session token save / get / exists / delete
  - **Hash**: user profile write-all, field get, field delete
  - **Set**: user tags add / list / remove
- `RedisToolDemoRunner` runs the demos on startup; disable with `quickstart.redis-tool.demo.enabled=false`
- Run: `mvn -pl peach-middleware/peach-redis/peach-redis-tool-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redis/peach-redis-tool-quickstart -am test`

### MultiCache

- Module: [`peach-redis-multicache-quickstart`](./peach-redis-multicache-quickstart/)
- Capability samples (product-detail scenario, two access styles):
  - **CacheManager**: inject the auto-configured `CacheManager`, then explicit `put/get`, miss → load, `evict` then reload
  - **Layered proof**: clear L1 (Caffeine) only, still hit L2 (Redis), refill local, and skip the store
  - **Annotation**: `@EnableCaching` + `@Cacheable` / `@CacheEvict` on the same `CacheManager`
- `MulticacheDemoRunner` runs the demos on startup; tests use `InMemoryProductStore#loadCount` and `clearLocal` to prove the hit path
- Minimal config: `peach.multicache.enabled=true`, `peach.multicache.cache-names` (`product-manager` / `product-annotation`), plus `peach.redis.*`
- Run: `mvn -pl peach-middleware/peach-redis/peach-redis-multicache-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redis/peach-redis-multicache-quickstart -am test`
- Disable startup demo: `quickstart.multicache.demo.enabled=false`

### Stream

### Pending recovery and DLQ (Issue #37)

- **Opt-in**: set `peach.redis.stream.recovery.enabled=true`. Only consumer groups support recovery. The business consumer must implement durable idempotency keyed by the event ID or business key before enabling this feature; broadcast mode is unsupported.
- **Algorithm**: a single scheduled worker pages through `XPENDING` in bounded batches, uses `XCLAIM` to take idle records, increases retry idle time exponentially from `min-idle` to `max-retry-delay`, and XACKs only after a successful business callback.
- **Dead letters**: upon `max-attempts`, append the original ID, source, delivery count and payload to an independent DLQ stream, then XACK the source only after successful persistence. The default is `<source-stream>:dlq`. DLQ XADD and source XACK are **not cross-key atomic**; crashes can duplicate DLQ records. De-duplicate by `sourceId`.
- **Acceptance boundary**: at-least-once only. Crash injection, Redis failover/Cluster topologies, manual replay and observability remain required before asserting production-grade guarantees. Application restarts retain PEL state.

```yaml
peach:
  redis:
    stream:
      enable: true
      consumer-type: group
      recovery:
        enabled: true
        batch-size: 32
        max-attempts: 5
        min-idle: 60s
        max-retry-delay: 15m
        interval: 30s
        # dead-letter-stream: order-status:dlq
```


**Acknowledgment and recovery boundary:** consumer groups XACK only after the business callback succeeds; failures remain pending in the PEL. Each service instance uses a unique consumer name and Spring manages the executor. Automatic `XPENDING`/`XCLAIM` recovery, backoff and DLQ are not yet implemented; durable idempotency is still required.


- Module: [`peach-redis-stream-quickstart`](./peach-redis-stream-quickstart/)
- Capability samples (order-status stream):
  - **Push + Consume**: `RedisStreamPushHandler#push` returns `RecordId`; implement `MessageConsumer` to confirm consume
  - **Idempotency**: replay the same `RecordId` and skip duplicates in business code (accepted=1 / duplicate=1)
- `RedisStreamDemoRunner` runs on startup; disable with `quickstart.stream.demo.enabled=false`
- Minimal config: `peach.redis.stream.enable=true` (`consumer-type=group`), plus `peach.redis.*`
- Run: `mvn -pl peach-middleware/peach-redis/peach-redis-stream-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-middleware/peach-redis/peach-redis-stream-quickstart -am test`

`peach.redis.host` uses `host:port`. Quickstarts share defaults: `password` is empty by default (override via `PEACH_REDIS_PASSWORD`), `database=1`.

## Redis-only foundation (new)

The repository now includes `peach-redis-autoconfigure`, `peach-redis-starter`, and `peach-redis-quickstart`. This foundation uses Spring Data Redis + Jedis and does **not** create `RedissonClient`. Tool, MultiCache and Stream feature Starters now include it. The historical `peach-redis-common` is kept for legacy services until migration acceptance is complete.

| Capability | Entry point | Important contract |
| --- | --- | --- |
| Redis-only / `peach-redis-starter` | `RedisConnectionFactory` / `RedisTemplate` / `StringRedisTemplate` | Jedis default; custom connection factories win; JSON values retain legacy Jackson2JsonRedisSerializer format |
| Tool / `peach-redis-tool-starter` | `RedisDao` | Depends on the new foundation; avoid SCAN on hot or huge keyspaces |
| MultiCache / `peach-redis-multicache-starter` | `MultiCacheManagerService` | Includes Tool, Caffeine and Hutool; invalidation remains best-effort |
| Stream / `peach-redis-stream-starter` | `RedisStreamHandler` | Uses generic `RedisConnectionFactory`, ACKs on success and leaves failures pending; automated recovery remains future work |

**Minimal dependency** (version is managed by the project parent POM):

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-starter</artifactId>
    <version>${revision}</version>
</dependency>
```

**Minimal configuration:** retain `peach.redis.mode / host / password / database`. Set `peach.redis.enabled=false` to disable the new auto-configuration, `peach.redis.ssl-enabled=true` for TLS and `peach.redis.jedis.max-total` for the connection budget. Existing `RedisConnectionFactory` wins; default pool `max-total=128`.

**Shortest path:** from the repository root run `mvn -pl peach-middleware/peach-redis/peach-redis-quickstart -am spring-boot:run`. Run `-am test` on the same module for real Redis integration tests (requires Docker for Testcontainers). The example does not require Redisson. The Sa-Token Starter and Auth/Setting/Message modules now use the independent Redis Starter. Legacy `RedisConfig` remains available for phased rollout; production login and session compatibility have not been fully accepted.

## Boundaries

- Keys need stable namespaces and required isolation dimensions; sensitive values must not be placed directly in keys/logs.
- Caches define TTL, invalidation, fallback and consistency semantics.
- Stream consumers require business idempotency plus explicit ACK, retry, backlog and failure governance.
- Pub/Sub, Stream and caches must not be documented as strong consistency or Exactly-Once guarantees.
