# peach-redission

English | [中文](README.md)

Last updated: 2026-07-15
artifactId: `peach-redission`
Type: Redisson middleware aggregate module

## Purpose

`peach-redission` uses `RedissonClient` to provide distributed locks, delay queues, a segmented scalable Bloom filter, and repeat-execution protection. Business modules should import the starter for the required capability instead of depending on autoconfigure directly.

The artifactId, packages, and some public types retain historical spellings such as `redission` and `distrbuted`. Use the spelling present in source for compatibility, but do not copy it into new APIs.

## Prerequisite Configuration

All capabilities require a `RedissonClient`, which is normally created by `RedisConfig` in `peach-redis-common`:

```yaml
peach:
  redis:
    mode: standalone
    host: ${PEACH_REDIS_NODES:127.0.0.1:6379}
    password: ${PEACH_REDIS_PASSWORD:}
    database: 0
    redisson:
      enabled: true
```

When an application supplies its own client, make the injectable client unambiguous. Some runtime dependencies in the autoconfigure POMs are optional; verify that the final application includes `peach-redis-common`, AOP, and Redisson dependencies.

## Distributed Lock

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-distributedlock-starter</artifactId>
</dependency>
```

```java
@DistrbutedLock(
        name = "order:pay",
        keys = {"#orderId"},
        lockType = LockType.REENTRANT,
        waitTime = 5,
        timeUnit = TimeUnit.SECONDS
)
public void pay(Long orderId) {
    // business operation
}
```

`keys` is required and supports SpEL. Defaults are `REENTRANT`, `waitTime=10`, `TimeUnit.SECONDS`, and the `FAIL` timeout strategy. The annotation is implemented through Spring AOP, so self-invocation does not pass through the aspect. Although the annotation also allows type targets, the current aspect matches method annotations only.

A lock limits concurrent execution; it does not replace database constraints, state validation, transactions, or business idempotency. `RedissionWriteLocker.getLock()` differs from the other lockers and requires focused tests before use.

## Delay Queue

Import `peach-redission-delayqueue-starter`, publish through `DelayQueueContext.sendMessage(topic, content, delay, unit)`, and implement `ConsumerTask` with a unique topic for consumption.

Prefix: `peach.delay.queue`

| Property | Default | Description |
| --- | --- | --- |
| `core-pool-size` / `maximum-pool-size` | `4` / `4` | Consumer thread count |
| `work-queue-size` | `256` | Consumer executor queue capacity |
| `isolation-region-count` | `5` | Topic partition count; producers and consumers must agree |
| `max-retry-attempts` | `3` | Maximum consumer retries |
| `retry-interval-millis` | `5000` | Retry interval |
| `use-reliable-queue` | `true` | Selects the reliable-consumer implementation path |
| `max-dead-letter-queue-size` | `10000` | Dead-letter cap |
| `dead-letter-message-retention-hours` | `168` | Dead-letter retention period |

Delivery time is not an exact execution time. Queueing, Redis latency, and business duration add delay. Consumers must be idempotent and have monitoring for retries, dead letters, backlog, and execution time. `use-reliable-queue=true` is not an exactly-once or no-loss guarantee: processing-state transfer is non-atomic, recovery across instances can duplicate work, and rejection or process exit requires dedicated validation. The dead-letter cap is JVM-local and is not a global Redis count across instances or restarts.

## Bloom Filter

Import `peach-redission-bloomfilter-starter`. The `BloomFilterService` supports namespace initialization, single and batch writes, cross-segment queries, status, and clearing. It can be extended through JDK SPI implementations of `KeyNamingStrategy`, `CodecProvider`, and `BloomScalePolicy`; the auto-configuration selects the first discovered implementation, so package only one intended implementation of each SPI.

Prefix: `peach.redis.bloom`. Defaults include `enabled=true`, capacity `1000000`, false-positive probability `0.001`, load factor `0.9`, scale factor `2`, and at most `32` segments.

A positive result means only “might exist”; it must not be the final decision for authorization, balance, or uniqueness. `clear(namespace)` is destructive and needs access control. The default local segment-list cache has no cross-node expansion synchronization, so a node can return a false negative from an old list. Segment-count keys are also not segment-isolated, making status and scaling statistics potentially inaccurate.

## Repeat-Execution Protection

Import `peach-redission-repeat-starter` and annotate methods with `@RepeatLimit`. `durationTime` is in seconds: a value greater than zero writes a success marker after a successful method call; `0` only limits concurrent execution and does not retain a marker. It depends on `LocalCacheLock`, `RedissionDataHandle`, and distributed-lock beans.

Repeat keys must contain a stable business identifier and required tenant/user isolation dimensions. Do not use random values or raw sensitive values. The current aspect silently swallows errors when writing the success marker, so a successful request can be followed by silently ineffective repeat protection. Class-level `@RepeatLimit` is not matched by the current aspect.

## Production Boundaries

- Namespace keys by application and business; isolate tenant data.
- Keep lock critical sections short, and handle acquisition failure explicitly.
- Do not assume delay messages are unique, lossless, or millisecond-accurate.
- Do not use a Bloom-filter positive result as a final security or money decision.
- Do not log Redis nodes, passwords, payloads, or full business keys.
- Treat `peach.localLock.durationTime` as a compatibility risk: metadata says seconds while current source uses hours.

## Build and Troubleshooting

```bash
mvn -f "peach-middleware/peach-redission/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

The aggregate module currently has no test sources; a successful build does not verify its distributed semantics.

| Symptom | Check | Resolution |
| --- | --- | --- |
| `RedissonClient` is missing | `peach.redis` configuration, enabled flag, dependencies | Enable the default client or provide one custom bean |
| Lock annotation has no effect | AOP proxy and self-invocation | Invoke from another proxied bean and verify the aspect |
| Lock waits or fails frequently | Key granularity, wait time, operation duration | Narrow the critical section and handle timeouts explicitly |
| Delay queue does not consume | `ConsumerTask`, topic, partitions, Redisson | Check bean registration and connection |
| Delayed task repeatedly fails | Payload, retry count, dead-letter state | Fix the consumer and compensate safely from the dead-letter path |
| Bloom false positives rise | Capacity, FPP, segment count, load | Inspect status, then resize or rebuild the namespace |
| Repeat requests are blocked incorrectly | SpEL, business key, `durationTime` | Correct isolation dimensions and remove invalid markers |
