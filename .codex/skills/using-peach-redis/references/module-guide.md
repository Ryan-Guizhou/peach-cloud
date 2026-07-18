# peach-redis / peach-redission 模块参考

本文只记录当前源码可验证事实。配置、API、自动配置条件或 POM 依赖变化后必须重新核对源码。

## 模块导航

```text
peach-middleware
├── peach-redis
│   ├── peach-redis-common
│   │   └── RedisConfig.java
│   ├── peach-redis-tool-autoconfigure
│   │   ├── RedisDao.java
│   │   └── RedisDaoImpl.java
│   ├── peach-redis-tool-starter
│   ├── peach-redis-multicache-autoconfigure
│   │   ├── MultiCacheAutoConfiguration.java
│   │   ├── MultiCacheManager.java
│   │   └── MultiCacheConfig.java
│   ├── peach-redis-multicache-starter
│   ├── peach-redis-stream-autoconfigure
│   │   ├── RedisStreamAutoConfig.java
│   │   ├── RedisStreamHandler.java
│   │   ├── MessageConsumer.java
│   │   └── RedisStreamProperties.java
│   └── peach-redis-stream-starter
└── peach-redission
    ├── peach-redission-common
    ├── peach-redission-distributedlock-autoconfigure
    │   ├── DistrbutedLock.java
    │   └── DistrbutedLockAspect.java
    ├── peach-redission-distributedlock-starter
    ├── peach-redission-delayqueue-autoconfigure
    │   ├── DelayQueueContext.java
    │   ├── ConsumerTask.java
    │   └── DelayQueueProperties.java
    ├── peach-redission-delayqueue-starter
    ├── peach-redission-bloomfilter-autoconfigure
    │   ├── BloomFilterService.java
    │   ├── SegmentedBloomFilterService.java
    │   └── BloomFilterProperties.java
    ├── peach-redission-bloomfilter-starter
    ├── peach-redission-repeat-autoconfigure
    │   ├── RepeatLimit.java
    │   └── RepeatExecuteLimitAspect.java
    └── peach-redission-repeat-starter
```

导航时忽略 `target/` 和 `.flattened-pom.xml`。

## 基础连接事实

`RedisConfig` 要求 `peach.redis.mode`、`host`、`password`。mode 支持 standalone、sentinel、cluster；host 为逗号分隔 `host:port`。

它默认创建：

- `JedisPoolConfig`、`JedisConnectionFactory`；
- `RedisTemplate<String, Object>`、`StringRedisTemplate`；
- `RedissonClient`（`peach.redis.redisson.enabled` 缺省为 true）。

当前 RedisTemplate 使用 Jackson polymorphic typing。只允许受信写入边界；修改 codec 或类型策略必须做存量数据兼容测试。

## 能力入口与关键语义

### RedisDao

- 提供 String、Hash、List、Set、ZSet、Pub/Sub、scan 和计数器操作。
- `hscan`、`sscan`、`zscan` 已 deprecated 且仅支持单机。
- 批量删除和 scan 必须限制 pattern 与 count，不得暴露为任意用户输入。
- 当前 `RedisDaoImpl.scan()` 返回 null，依赖它的 keys/deletePattern 和多级缓存二级清理不可视为可用。

### 多级缓存

- 前缀 `peach.multicache`，`enabled` 缺省启用。
- Redis 默认 TTL 6 小时；random-jitter 默认 0。
- Caffeine 默认 expire-after-access/write/refresh-after-write 均为 10800000ms，maximum-size 为 5000。
- 节点间本地失效使用 Redis Pub/Sub，不保证断连期间送达。
- `refresh-after-write` 当前未应用；value STRONG 实际为 softValues；key STRONG 当前不支持。

### Redis Stream

- 前缀 `peach.redis.stream`，只有 `enable=true` 才启用。
- 默认 stream/group/consumer 为 `user-log` / `user-log-group` / `user-log-consumer`。
- consumer-type 仅支持 group、broadcast。
- group 当前为 receiveAutoAck；broadcast 从 Stream 起点接收。
- 容器 Bean 只有存在 `MessageConsumer` 时创建。
- listener 吞掉业务异常，失败消息可能已 ACK；没有内置重试和死信。
- Stream 已存在但 group 不存在时，当前创建逻辑可能导致 NOGROUP。

### 分布式锁

- 注解：`@DistrbutedLock`；keys 必填，支持 SpEL。
- 默认锁类型 REENTRANT、waitTime 10 秒、超时策略 FAIL。
- 通过 Spring AOP 生效；类内自调用不生效。
- 虽然注解 Target 包含 TYPE，当前切面仅匹配方法注解。
- `RedissionWriteLocker.getLock()` 与其他 locker 行为不同，使用前需专项验证。

### 延迟队列

- 生产入口：`DelayQueueContext.sendMessage(topic, content, delay, unit)`。
- 消费入口：实现 `ConsumerTask`，topic 必须唯一且与生产者一致。
- 默认 5 个隔离分区、3 次重试、5 秒重试间隔、可靠队列开启。
- 生产与消费的 isolation-region-count 必须一致。
- reliable 路径仍不保证不丢不重；processing 转移非原子，多实例恢复可重复。
- DLQ 数量上限使用 JVM 内计数，多实例/重启后不代表全局准确值。

### 布隆过滤器

- 前缀 `peach.redis.bloom`，enabled 缺省启用。
- 默认容量 1000000、FPP 0.001、load-factor 0.9、scale-factor 2、max-segments 32。
- 使用分段 `RBloomFilter`；查询跨段，写入尾段。
- `KeyNamingStrategy`、`CodecProvider`、`BloomScalePolicy` 通过 JDK SPI 扩展，当前选择发现到的第一个实现。
- 默认本地段缓存无跨节点扩容同步，可能产生假阴性。
- 分段计数 key 当前未包含段名，状态和扩容统计可能失真。

### 防重复执行

- 注解：`@RepeatLimit`；keys 必填，支持 SpEL。
- durationTime 单位秒。大于 0 时成功后写标记；0 仅限制并发执行。
- 依赖本地锁、Redisson 数据操作和分布式锁 Bean。
- 成功标记写 Redis 的异常当前被吞掉，业务成功后防重可能静默失效。
- 注解 Target 包含 TYPE，但当前切面仅匹配方法注解。

## 依赖与兼容风险

- 多个 autoconfigure POM 将运行时依赖标记为 optional；独立 starter 接入时检查最终依赖树和 Bean 条件。
- `redission`、`distrbuted`、`annoation`、`Contant`、`Linstener` 等是存量拼写，不主动破坏兼容，也不继续扩散。
- 修改 key 格式、codec、默认 TTL、ACK 或分区数属于高影响变更，必须提供迁移/兼容方案。
- `peach.localLock.durationTime` metadata 声称单位秒，源码当前按小时使用。
- 当前两个聚合模块都没有测试源码；构建成功不能替代运行语义验证。

## 变更检查表

- 配置字段：配置类、metadata、README、默认值测试同步。
- key 或 codec：兼容旧数据，明确双读/迁移/清理方案。
- Stream：验证消费组、ACK、重放、consumer 唯一性和积压。
- 锁/防重：验证 SpEL、代理调用、超时策略、异常释放和租户隔离。
- 延迟队列：验证分区一致、消费者幂等、重试与死信。
- 布隆：验证容量、FPP、扩容上限、清理权限和权威源回查。

## 验证

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" clean package -DskipTests -Pdevelopment
mvn -f "peach-middleware/peach-redission/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```
