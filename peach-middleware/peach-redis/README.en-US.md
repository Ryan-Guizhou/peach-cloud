# Peach Redis

English | [中文](README.md)

Last updated: 2026-07-03  
Target version: `peach-cloud 1.0.0-SNAPSHOT`, JDK 8, Spring Boot 2.7.x

## 1. Purpose

`peach-redis` is the Redis infrastructure module for Peach. It currently provides four capability groups:

- Base Redis and Redisson configuration: `RedisConfig` creates `JedisConnectionFactory`, `RedisTemplate`, `StringRedisTemplate`, and an optional `RedissonClient`.
- Multi-level cache: `peach-redis-multicache-starter` combines local Caffeine cache with Redis and uses Redis Pub/Sub to invalidate local cache entries across nodes.
- Redis Stream: `peach-redis-stream-starter` provides stream publishing, consumer-group binding, and listener container setup.
- Redis DAO utilities: `peach-redis-tool-starter` provides `RedisDao` for common string, hash, list, set, zset, scan, and pub/sub operations.

This module does not deploy Redis, design business Redis keys, or guarantee business-level cache consistency. Key naming, TTL strategy, invalidation timing, stream idempotency, and compensation remain business responsibilities.

## 2. Module Layout

```text
peach-middleware/peach-redis/
├── pom.xml
├── README.md
├── README.en-US.md
├── peach-redis-common/
│   ├── pom.xml
│   └── src/main/java/com/peach/redis/
│       ├── common/RedisConfig.java
│       └── constant/RedisConstant.java
├── peach-redis-multicache-autoconfigure/
│   ├── pom.xml
│   └── src/main/java/com/peach/redis/
│       ├── autoconfigure/MultiCacheAutoConfiguration.java
│       ├── config/
│       ├── listener/
│       └── manager/
├── peach-redis-multicache-starter/
│   └── pom.xml
├── peach-redis-stream-autoconfigure/
│   ├── pom.xml
│   └── src/main/java/com/peach/redis/stream/
├── peach-redis-stream-starter/
│   └── pom.xml
├── peach-redis-tool-autoconfigure/
│   ├── pom.xml
│   └── src/main/java/com/peach/redis/common/tool/
└── peach-redis-tool-starter/
    └── pom.xml
```

## 3. Maven Usage

Import only the starter you need. Multi-cache and Stream both rely on the base Redis configuration from `peach-redis-common`.

```xml
<!-- Multi-level cache -->
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-multicache-starter</artifactId>
</dependency>

<!-- Redis Stream -->
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-stream-starter</artifactId>
</dependency>

<!-- RedisDao utilities -->
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-tool-starter</artifactId>
</dependency>
```

## 4. Base Redis Configuration

`RedisConfig` reads `peach.redis.*` properties and supports `standalone`, `sentinel`, and `cluster` modes.

### 4.1 Standalone

```yaml
peach:
  redis:
    mode: standalone
    host: 127.0.0.1:6379
    password:
    database: 0
    redisson:
      enabled: true
```

### 4.2 Sentinel

```yaml
peach:
  redis:
    mode: sentinel
    host: 127.0.0.1:26379,127.0.0.1:26380
    sentinelMaster: master
    password:
    database: 0
```

### 4.3 Cluster

```yaml
peach:
  redis:
    mode: cluster
    host: 127.0.0.1:6379,127.0.0.1:6380,127.0.0.1:6381
    password:
```

### 4.4 Redisson Properties

`RedissonClient` is enabled by default. Disable it with `peach.redis.redisson.enabled=false`.

| Property | Default | Description |
| --- | --- | --- |
| `peach.redis.redisson.enabled` | `true` | Whether to create `RedissonClient` |
| `peach.redis.redisson.threads` | `16` | Redisson worker threads |
| `peach.redis.redisson.netty-threads` | `32` | Netty threads |
| `peach.redis.redisson.timeout` | `3000` | Redis command timeout in milliseconds |
| `peach.redis.redisson.connection-pool-size` | `64` | Single-server connection pool size |
| `peach.redis.redisson.connection-minimum-idle-size` | `10` | Single-server minimum idle connections |
| `peach.redis.redisson.subscription-connection-pool-size` | `50` | Subscription connection pool size |
| `peach.redis.redisson.subscription-connection-minimum-idle-size` | `1` | Minimum idle subscription connections |
| `peach.redis.redisson.slave-connection-pool-size` | `64` | Slave connection pool size for sentinel/cluster |
| `peach.redis.redisson.master-connection-pool-size` | `64` | Master connection pool size for sentinel/cluster |
| `peach.redis.redisson.scan-interval` | `1000` | Cluster scan interval in milliseconds |
| `peach.redis.redisson.idle-connection-timeout` | `10000` | Idle connection timeout in milliseconds |
| `peach.redis.redisson.ping-timeout` | `1000` | Ping interval/timeout setting in milliseconds |
| `peach.redis.redisson.connect-timeout` | `10000` | Connect timeout in milliseconds |
| `peach.redis.redisson.retry-attempts` | `3` | Retry attempts |
| `peach.redis.redisson.retry-interval` | `1500` | Retry interval in milliseconds |
| `peach.redis.redisson.subscriptions-per-connection` | `5` | Subscriptions per connection |
| `peach.redis.redisson.ssl-enable` | `false` | Whether to use `rediss://` |

## 5. Multi-Level Cache

`peach-redis-multicache-starter` auto-configures:

- `MultiCacheManager`: a Spring `CacheManager` implementation.
- `RedisMessageListenerContainer`: listens for cache invalidation messages.
- `MultiCacheManagerService`: explicit cache operation helper.

Auto-configuration condition: `peach.multicache.enabled` is enabled by default.

### 5.1 Configuration Example

```yaml
peach:
  multicache:
    enabled: true
    cache-names:
      - user
      - dict
    cache-null-values: false
    cache-prefix: peach
    redis:
      default-expiration: 6h
      random-jitter: 30s
      topic: cache-message-topic
      expires:
        user: 30m
        dict: 12h
    caffeine:
      expire-after-access: 10800000
      expire-after-write: 10800000
      initial-capacity: 500
      maximum-size: 5000
      key-strength: WEAK
      value-strength: STRONG
```

### 5.2 Usage Example

```java
@Resource
private MultiCacheManagerService multiCacheManagerService;

public UserDTO getUser(Long userId) {
    return multiCacheManagerService.getOrElse("user", userId, () -> loadUser(userId));
}

public void evictUser(Long userId) {
    multiCacheManagerService.evict("user", userId);
}
```

You can also use the standard Spring `CacheManager` or cache annotations. When a cache entry changes, Redis publishes an invalidation message so other nodes can clear their local Caffeine entries.

### 5.3 Cache Boundaries

- Redis is the second-level cache; Caffeine is the local first-level cache.
- `clear()` uses `RedisDao.keys(pattern)` and then deletes matched keys; use it carefully in large key spaces.
- `asyncClear()` currently creates a raw `Thread` and does not use `peach-threadpool`.
- If `cache-null-values=false`, writing `null` evicts the key.
- Caffeine `value-strength=STRONG` currently maps to `softValues()`; validate GC behavior before relying on reference semantics.

## 6. Redis Stream

`peach-redis-stream-starter` provides `RedisStreamPushHandler`, `RedisStreamHandler`, and `StreamMessageListenerContainer`.

Auto-configuration condition: `peach.redis.stream.enable=true`. The listener container is created only when a `MessageConsumer` bean exists.

### 6.1 Configuration Example

```yaml
peach:
  redis:
    stream:
      enable: true
      stream-name: user-log
      consumer-group: user-log-group
      consumer-name: user-log-consumer
      consumer-type: group
```

`consumer-type` supports only:

- `group`: consumer group mode using `receiveAutoAck`.
- `broadcast`: broadcast mode reading from the beginning of the stream.

### 6.2 Publish Messages

```java
@Resource
private RedisStreamPushHandler redisStreamPushHandler;

public void publish(String message) {
    redisStreamPushHandler.push(message);
}
```

### 6.3 Consume Messages

```java
@Bean
public MessageConsumer userLogConsumer() {
    return message -> {
        String body = message.getValue();
        // handle message
    };
}
```

### 6.4 Stream Boundaries

- Listener batch size is fixed to `10`; poll timeout is fixed to `5s`.
- Group mode currently uses auto ack. Consumers must implement idempotency and compensation for business failures.
- The stream consumer executor is created inside `RedisStreamAutoConfig`; thread names start with `thread-consumer-stream-task-`.
- Default `streamName`, `consumerGroup`, and `consumerName` are `user-log`, `user-log-group`, and `user-log-consumer`.

## 7. RedisDao Utilities

`peach-redis-tool-starter` auto-configures `RedisDao` when `RedisTemplate` exists.

```java
@Resource
private RedisDao redisDao;

public void writeToken(String token, Object value) {
    redisDao.vSet("token:" + token, value, Duration.ofMinutes(30));
}

public Object readToken(String token) {
    return redisDao.vGet("token:" + token);
}
```

Main capabilities:

- key: `existsKey`, `delete`, `deletePattern`, `keys`, `expire`.
- string: `vSet`, `vGet`, `increaseNum`, `decreaseNum`.
- hash: `hmSet`, `hmSetAll`, `hmGet`, `hmDel`, `hmSetIncrement`.
- list: `lLeftPush`, `lRightPush`, `lLeftPop`, `lRightPop`, `lRange`, `lRemove`.
- set/zset: `sAdd`, `sMembers`, `sRandomMember`, `sPop`, `sRemove`, `zAdd`, `rangeByScore`.
- pub/sub: `convertAndSend`.
- scan: `scan`, plus deprecated `hscan`, `sscan`, and `zscan`.

## 8. Auto-Configuration Entries

| Module | AutoConfiguration imports |
| --- | --- |
| `peach-redis-common` | `com.peach.redis.common.RedisConfig` |
| `peach-redis-multicache-autoconfigure` | `com.peach.redis.autoconfigure.MultiCacheAutoConfiguration` |
| `peach-redis-stream-autoconfigure` | `com.peach.redis.stream.config.RedisStreamAutoConfig` |
| `peach-redis-tool-autoconfigure` | `com.peach.redis.common.tool.RedisDaoAutoConfigure` |

## 9. Build and Verification

Module build:

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" test
mvn -f "peach-middleware/peach-redis/pom.xml" -DskipTests package
```

For documentation-only changes, at least run:

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" -DskipTests package
```

## 10. Troubleshooting

| Symptom | Check | Resolution |
| --- | --- | --- |
| `RedisTemplate` is missing | Whether a Redis starter is imported; whether `peach.redis.mode` and `host` are configured | Import the starter and provide `peach.redis.*` |
| Redis connection fails on startup | Whether `mode` is `standalone`, `sentinel`, or `cluster`; whether `host` is a comma-separated `host:port` list | Fix mode and node list |
| Redisson initialization fails | Password, SSL, connection pool, and cluster node settings | Disable `peach.redis.redisson.enabled` temporarily or fix Redisson settings |
| Multi-cache does not work | Whether `peach.multicache.enabled` is disabled; whether `MultiCacheManagerService` is injected | Keep it enabled or set it explicitly to `true` |
| Local cache is not invalidated across nodes | Whether Redis Pub/Sub topic is consistent; whether listener container starts | Check `peach.multicache.redis.topic` and Redis connectivity |
| `clear()` is slow | The cache key space is too large; `keys/scan` is expensive | Avoid frequent full clears; prefer precise key eviction |
| Stream consumer does not run | Whether `peach.redis.stream.enable=true`; whether a `MessageConsumer` bean exists | Enable Stream and provide a consumer bean |
| Stream messages are duplicated or business failures are lost | Group mode uses auto ack | Implement consumer idempotency, error logging, and compensation |

## 11. Current Limits and Recommendations

- `RedisConfig` uses Jedis and does not provide a Lettuce configuration branch.
- `RedisDao` is a convenience helper and does not replace Lua, transactions, or carefully designed atomic Redis operations.
- Stream batch size, poll timeout, and executor settings are not externally configurable yet.
- Cache consistency is based on invalidation messages and local eviction, not strong consistency.
- In production, define key naming, TTL, and large-key governance rules before using `keys`, `clear`, or batch deletion broadly.
