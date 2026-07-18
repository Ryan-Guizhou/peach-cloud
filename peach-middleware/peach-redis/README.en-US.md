# peach-redis

English | [中文](README.md)

Last updated: 2026-07-15
artifactId: `peach-redis`
Target version: `peach-cloud 1.0.0-SNAPSHOT`, JDK 8, Spring Boot 2.7.x

## Purpose

`peach-redis` provides base Redis and Redisson configuration, `RedisDao`, Caffeine-plus-Redis multi-level caching, and Redis Stream support. It does not deploy Redis, define business key naming, or guarantee business-level cache consistency. Key naming, TTL, invalidation timing, stream idempotency, and compensation remain application responsibilities.

## Base Configuration

`RedisConfig` requires `peach.redis.mode`, `host`, and `password`; supported modes are `standalone`, `sentinel`, and `cluster`. `host` is a comma-separated `host:port` list. It creates a `JedisConnectionFactory`, `RedisTemplate<String, Object>`, `StringRedisTemplate`, and (unless disabled) a `RedissonClient`.

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

`RedissonClient` is enabled by default and can be disabled with `peach.redis.redisson.enabled=false`. Important defaults are `threads=16`, `netty-threads=32`, command/connect timeouts of `3000`/`10000` ms, pool size/minimum idle of `64`/`10`, retries `3` every `1500` ms, `scan-interval=1000` ms, and `ssl-enable=false`. When enabling TLS, verify certificates, Redis ports, and the protocol of sentinel or cluster nodes together.

`RedisTemplate` currently uses Jackson polymorphic typing. Treat Redis as a trusted-write boundary; do not deserialize untrusted data with arbitrary types.

## Capability Starters

| Capability | Starter | Notes |
| --- | --- | --- |
| RedisDao | `peach-redis-tool-starter` | General string, collection, Pub/Sub, scan, and counter operations |
| Multi-level cache | `peach-redis-multicache-starter` | Caffeine L1 plus Redis L2, invalidated with Redis Pub/Sub |
| Redis Stream | `peach-redis-stream-starter` | Stream publishing and `MessageConsumer` binding |

Some autoconfigure POMs mark runtime dependencies optional. Check the final dependency tree and bean conditions when integrating a starter independently.

## RedisDao and Multi-Level Cache

`RedisDao` provides String, Hash, List, Set, ZSet, Pub/Sub, scan, and counter operations. Its deprecated `hscan`, `sscan`, and `zscan` work only for standalone mode. Bound every scan pattern and count; never expose scan or batch deletion directly to untrusted input.

The multi-cache prefix is `peach.multicache` and it is enabled by default. Redis TTL defaults to six hours; random jitter defaults to zero. Caffeine defaults to `10800000` ms for expire-after-access, expire-after-write, and refresh-after-write, with maximum size `5000`. Node-local invalidation is Redis Pub/Sub and is not guaranteed during disconnection.

Current limitations matter in production: `RedisDaoImpl.scan()` returns `null`, so its dependent key deletion and L2 cache clearing are not reliable; `refresh-after-write` is not applied; `value-strength=STRONG` is actually implemented with soft values; `key-strength=STRONG` is unsupported. Do not describe these paths as strong consistency or guaranteed cleanup.

## Redis Stream

Enable the capability explicitly:

```yaml
peach:
  redis:
    stream:
      enable: true
      stream-name: order-events
      consumer-group: order-service
      consumer-name: order-service-1
      consumer-type: group
```

The listener container is created only when a `MessageConsumer` bean exists. Defaults are `user-log`, `user-log-group`, and `user-log-consumer`; supported consumer types are only `group` and `broadcast`.

Group mode uses `receiveAutoAck`. The listener catches business exceptions rather than rethrowing them, so failed messages may already be acknowledged. There is no built-in retry or dead-letter handling. Group creation only covers a missing Stream; an existing Stream with a missing group can produce `NOGROUP`. Broadcast mode reads from the beginning of the Stream, so assess historic volume and duplicate handling before using it.

## Runtime and Production Boundaries

```text
peach.redis configuration
    -> RedisConfig
        -> JedisConnectionFactory
        -> RedisTemplate / StringRedisTemplate
        -> RedissonClient (optional)
              -> RedisDao
              -> Caffeine <-> Redis multi-level cache
              -> Redis Stream
              -> peach-redission capabilities
```

- Namespace keys by application and business; add tenant isolation where needed. Never place sensitive raw values in a key.
- Define TTL, invalidation, and source-of-truth fallback for every cache write.
- Stream consumers require business idempotency, failure monitoring, and backlog governance.
- Do not log Redis passwords, nodes, or payloads. Current Stream paths may log complete messages at INFO; remove or redact those logs before production use.
- Do not use `KEYS *` or unbounded scans in online requests. Use controlled `SCAN` with bounded batches.
- Pub/Sub invalidation is not a reliable message channel. Cache invalidation is eventually consistent.

## Build and Troubleshooting

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

The aggregate module currently has no test sources. A successful build does not validate cache consistency, scan cleanup, or Stream failure semantics.

| Symptom | Check | Resolution |
| --- | --- | --- |
| Missing configuration placeholder at startup | `mode`, `host`, and `password` | Provide the required `peach.redis` configuration |
| Redis connection failure | Mode, node format, password, database, TLS | Validate with Redis CLI from the same network |
| `RedisDao` is missing | Tool starter and `RedisTemplate` creation | Check dependencies and auto-configuration imports |
| Stale cache entry | Pub/Sub topic and local TTL | Perform explicit eviction and reduce the consistency window |
| Stream bean is missing | `enable=true` and a `MessageConsumer` bean | Check configuration and application bean registration |
| Stream duplicates or backlog | Consumer name, group, exception handling, throughput | Use unique consumer names, idempotent processing, and lag monitoring |
| Redisson client conflict | Multiple client definitions | Disable the default client or retain exactly one intended bean |
