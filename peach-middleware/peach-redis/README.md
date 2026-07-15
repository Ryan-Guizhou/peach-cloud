# peach-redis

[English](README.en-US.md) | 中文

最后更新时间：2026-07-15
artifactId：`peach-redis`
类型：Redis 中间件聚合模块

## 模块定位

`peach-redis` 提供 Jedis/RedisTemplate 基础连接、`RedisDao` 常用操作、多级缓存和 Redis Stream 接入。业务模块应按能力依赖 starter，不直接依赖 autoconfigure。

`peach-redis-common` 同时创建 `RedisTemplate`、`StringRedisTemplate` 和可选 `RedissonClient`，也是 `peach-redission` 各能力的基础依赖。本模块不部署或运维 Redis 服务端。

当前各 autoconfigure 的 Spring Data Redis、Jedis、Redisson 和 common 等依赖多为 optional。业务仅引入 starter 时，必须检查最终依赖树，不能假设所有运行时依赖会自动传递。

## 模块导航

```text
peach-middleware/peach-redis
├── peach-redis-common
│   └── RedisConfig                  # Jedis、RedisTemplate、RedissonClient
├── peach-redis-tool-autoconfigure
│   ├── RedisDao                     # 通用 Redis 操作契约
│   ├── RedisDaoImpl
│   └── RedisDaoAutoConfigure
├── peach-redis-tool-starter
├── peach-redis-multicache-autoconfigure
│   ├── MultiCacheAutoConfiguration
│   ├── MultiCacheManager
│   ├── MultiCacheManagerService
│   └── MultiCacheConfig
├── peach-redis-multicache-starter
├── peach-redis-stream-autoconfigure
│   ├── RedisStreamAutoConfig
│   ├── RedisStreamHandler
│   ├── RedisStreamPushHandler
│   ├── MessageConsumer
│   └── RedisStreamProperties
└── peach-redis-stream-starter
```

## 能力选择

| 需求 | 引入依赖 | 主要入口 |
| --- | --- | --- |
| String、Hash、List、Set、ZSet、扫描等常用操作 | `peach-redis-tool-starter` | `RedisDao` |
| Caffeine + Redis 两级缓存 | `peach-redis-multicache-starter` | `MultiCacheManagerService` |
| Redis Stream 生产与消费 | `peach-redis-stream-starter` | `RedisStreamHandler`、`MessageConsumer` |
| Redisson 锁、延迟队列、布隆过滤器 | 对应 `peach-redission-*-starter` | 见 `../peach-redission/README.md` |

## 基础配置

所有 starter 最终依赖 `RedisConfig`。`mode`、`host`、`password` 当前没有配置类默认值，必须显式提供；无密码环境可传空字符串。

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

- `mode` 支持 `standalone`、`sentinel`、`cluster`。
- `host` 使用逗号分隔的 `host:port`。单机模式只使用解析出的一个节点。
- 哨兵模式通过 `peach.redis.sentinelMaster` 指定 master 名称，当前键名沿用源码拼写。
- Redis 密码、真实集群地址不得提交到仓库。

## Redis 工具

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-tool-starter</artifactId>
</dependency>
```

注入 `RedisDao` 后可使用字符串、Hash、List、Set、ZSet、发布订阅、扫描和计数器操作。`hscan`、`sscan`、`zscan` 已标记为 deprecated 且只支持单机；新增代码应使用明确支持当前部署模式的 API。

当前 `RedisDaoImpl.scan()` 直接返回 `null`，因此依赖它的 `keys()`、`deletePattern()` 以及多级缓存二级清理并不完整。修复和测试前禁止把这些 API 用于生产清理。

## 多级缓存

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-multicache-starter</artifactId>
</dependency>
```

```yaml
peach:
  multicache:
    enabled: true
    cache-names:
      - user
    cache-null-values: false
    cache-prefix: "peach:"
    redis:
      default-expiration: 6h
      random-jitter: 30s
      expires:
        user: 30m
      topic: cache:message:topic
    caffeine:
      expire-after-access: 10800000
      expire-after-write: 10800000
      refresh-after-write: 10800000
      initial-capacity: 500
      maximum-size: 5000
      key-strength: WEAK
      value-strength: STRONG
```

配置前缀为 `peach.multicache`，`enabled` 缺省时自动启用。默认 Redis TTL 为 6 小时，Caffeine 三个时间字段单位均为毫秒。节点间通过 Redis Pub/Sub topic 通知本地缓存失效；Pub/Sub 不是持久消息，断连期间通知可能丢失。

当前实现还存在以下兼容限制：`refresh-after-write` 字段未实际应用；`value-strength=STRONG` 实际使用 soft values；`key-strength=STRONG` 会抛出不支持异常。配置这些字段前必须以实现和测试结果为准。

## Redis Stream

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-stream-starter</artifactId>
</dependency>
```

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

注册消费 Bean：

```java
@Bean
public MessageConsumer orderMessageConsumer() {
    return message -> orderEventService.handle(message.getValue());
}
```

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.redis.stream.enable` | 未配置即不启用 | 必须为 `true` 才装配 Stream |
| `stream-name` | `user-log` | Stream key |
| `consumer-group` | `user-log-group` | 消费组 |
| `consumer-name` | `user-log-consumer` | 消费者名，同组内应唯一 |
| `consumer-type` | `group` | 仅支持 `group`、`broadcast` |

当前 group 模式使用 `receiveAutoAck`，监听器会捕获业务异常而不向容器重新抛出，失败消息可能已 ACK，模块没有内置重试或死信。创建消费组的逻辑也只覆盖 Stream 不存在的情况；Stream 已存在但 group 不存在时可能报 `NOGROUP`。broadcast 模式从 Stream 起点读取，使用前应评估历史消息量和重复消费。

## Redisson 配置

`RedisConfig` 默认创建 `RedissonClient`，可通过 `peach.redis.redisson.enabled=false` 关闭。常用默认值：

| 配置项 | 默认值 |
| --- | --- |
| `threads` / `netty-threads` | `16` / `32` |
| `timeout` / `connect-timeout` | `3000` / `10000` 毫秒 |
| `connection-pool-size` / `connection-minimum-idle-size` | `64` / `10` |
| `retry-attempts` / `retry-interval` | `3` / `1500` 毫秒 |
| `scan-interval` | `1000` 毫秒 |
| `ssl-enable` | `false` |

完整配置以 `RedisConfig` 中 `peach.redis.redisson.*` 字段为准。启用 TLS 时应同时校验证书、Redis 端口和 sentinel/cluster 节点协议。

## 运行机制

```text
peach.redis 配置
    └── RedisConfig
        ├── JedisConnectionFactory
        ├── RedisTemplate / StringRedisTemplate
        └── RedissonClient（可关闭）
              │
              ├── RedisDao
              ├── Caffeine <-> Redis 多级缓存
              ├── Redis Stream
              └── peach-redission 扩展能力
```

## 生产边界

`REQUIRED`：

- key 必须包含应用/业务命名空间；多租户数据必须包含租户隔离维度。
- 所有写缓存路径都要定义 TTL、失效策略和源数据回源方式。
- Stream 消费必须具备业务幂等、失败观测和积压治理。
- Redis 密码、节点和业务数据不得写入日志。
- Stream 当前生产和消费路径可能输出完整消息内容；接入生产前必须移除或脱敏相关 INFO 日志。

`PREFERRED`：

- 大范围删除使用受控 `SCAN`，限制每批数量，禁止在线请求执行无边界 key 扫描。
- 多级缓存 TTL 使用抖动并明确最终一致性窗口。
- 根据部署模式和流量压测连接池、线程数、超时与重试。

`LEGACY_COMPATIBLE`：

- `RedisStreamContant`、`RedisStreamLinstener`、`sentinelMaster` 等拼写按当前 API 兼容；新增命名不继续复制。
- `RedisDao` 是历史宽接口，新增业务优先封装领域缓存仓储，避免 Service 直接散落 key。
- Jedis 池和 Stream 线程池的部分参数当前硬编码；README 配置表不能暗示它们均可外部配置。

`FORBIDDEN`：

- 对不可信 Redis 数据启用任意类型反序列化。当前 `RedisTemplate` 使用 Jackson polymorphic typing，必须确保 Redis 写入边界可信。
- 使用 `KEYS *` 或无边界扫描处理线上请求。
- 将 Pub/Sub 失效通知视为可靠消息。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

当前聚合模块未发现测试源码；构建通过只证明编译与打包成功，不证明缓存一致性、扫描清理或 Stream 失败语义正确。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 启动时报配置占位符缺失 | `mode`、`host`、`password` 是否存在 | 补充 `peach.redis` 基础配置 |
| Redis 连接失败 | mode、节点格式、密码、database、TLS 是否匹配 | 用同网络环境的 Redis CLI 验证 |
| `RedisDao` 未注入 | 是否引入 tool starter；`RedisTemplate` 是否创建 | 检查依赖和自动配置 imports |
| 缓存读到旧值 | Pub/Sub 通知、topic、本地 TTL 是否一致 | 主动失效并缩短一致性窗口 |
| Stream Bean 未创建 | `enable=true` 且是否存在 `MessageConsumer` | 检查配置和业务 Bean |
| Stream 重复或积压 | consumerName、消费组、处理异常和吞吐 | 保证 consumer 唯一、业务幂等并监控 lag |
| RedissonClient 冲突 | 是否同时定义自有客户端 | 关闭默认客户端或统一保留一个 Bean |
