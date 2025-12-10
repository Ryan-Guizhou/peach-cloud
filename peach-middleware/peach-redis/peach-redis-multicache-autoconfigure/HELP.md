# peach-redis-multicache 模块说明

## 模块概述

- 自动配置入口通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 暴露 `MultiCacheAutoConfiguration`，在属性 `peach.multicache.enabled=true` 时生效。
- 自动注册两类 Bean：
  - `MultiCacheManager`，实现 Spring `CacheManager` 接口，用于创建与管理多级缓存区域：`src/main/java/com/peach/redis/autoconfigure/MultiCacheAutoConfiguration.java:21`
  - `RedisMessageListenerContainer`，用于订阅缓存同步消息频道并清理其他节点本地缓存：`src/main/java/com/peach/redis/autoconfigure/MultiCacheAutoConfiguration.java:31`

## 架构与原理

- 两级缓存链路：
  - 读取优先走 Caffeine，本地未命中再读取 Redis，并将命中结果回灌到 Caffeine：`src/main/java/com/peach/redis/manager/MultiCache.java:70`
  - 写入同时落地 Redis 与 Caffeine，并向频道发布清理消息：`src/main/java/com/peach/redis/manager/MultiCache.java:207`
- 键命名规则：
  - 无前缀：`<cacheName>:<key>`
  - 有前缀：`<cachePrefix>:<cacheName>:<key>`
  - 构建位置：`src/main/java/com/peach/redis/manager/MultiCache.java:186`
- 过期策略：
  - 全局过期时间 `defaultExpiration`：`src/main/java/com/peach/redis/config/RedisCacheConfig.java:14`
  - 按 `cacheName` 细分的过期时间 `expires`，优先级更高：`src/main/java/com/peach/redis/manager/MultiCache.java:197`
- 多节点本地缓存同步：
  - 发布消息：`src/main/java/com/peach/redis/manager/MultiCache.java:176`
  - 监听与清理：`src/main/java/com/peach/redis/listener/CacheMessageListener.java:24`
- 区域清理：
  - 根据键命名规则构造模式并批量删除 Redis 键，再清理 Caffeine：`src/main/java/com/peach/redis/manager/MultiCache.java:160`

## 主要配置示例

```yaml
peach:
  multicache:
    enabled: true
    cache-null-values: false
    cache-prefix: demo
    redis:
      default-expiration: 6h
      expires:
        user: 10m
        order: 5m
      topic: peach:multicache:topic
    caffeine:
      expire-after-access: 10800000
      expire-after-write: 10800000
      refresh-after-write: 10800000
      initial-capacity: 500
      maximum-size: 5000
      key-strength: WEAK
      value-strength: STRONG

```

## 注解使用总结

- 启用缓存：在配置类或启动类上添加 `@EnableCaching`。
- `@Cacheable`
  - 作用：方法返回值会被缓存；相同键的后续调用直接命中缓存。
  - 常用属性：`cacheNames/value`、`key`、`unless`、`sync`、`cacheResolver`。
  - 示例：
    ```java
    @Cacheable(cacheNames = "user", key = "#id", unless = "#result == null")
    public User findById(String id) { return loadUser(id); }
    ```
  - 键在 Redis 中的最终形态遵循键命名规则，示例为 `user:<id>` 或 `demo:user:<id>`。
- `@CachePut`
  - 作用：方法每次执行后都会更新缓存条目，适合写操作后的缓存刷新。
  - 示例：
    ```java
    @CachePut(cacheNames = "user", key = "#user.id")
    public User update(User user) { return persist(user); }
    ```
- `@CacheEvict`
  - 作用：移除缓存条目或清空缓存区域。
  - 示例：
    ```java
    @CacheEvict(cacheNames = "user", key = "#id")
    public void delete(String id) { remove(id); }

    @CacheEvict(cacheNames = "user", allEntries = true)
    public void clearAll() {}
    ```
- 自定义 `CacheResolver`
  - 可根据入参动态选择缓存区域，例如 `DynamicCacheResolver` 将 `userCache` 动态改为 `userCache-<id>`。
  - 动态缓存名会影响区域维度，Redis 键仍按规则生成为 `<cacheName>:<key>` 或 `<prefix>:<cacheName>:<key>`。

## 验证与排查

- 打开调试日志以观测命中来源：
  - `logging.level.com.peach.redis.manager=DEBUG`
- 观察读写来源：
  - Caffeine 命中与日志：`src/main/java/com/peach/redis/manager/MultiCache.java:74`
  - Redis 命中与回灌：`src/main/java/com/peach/redis/manager/MultiCache.java:79`
- 使用 Redis 客户端核对键与 TTL：
  - 键示例：`demo:user:u:1`
  - TTL 取值由 `defaultExpiration` 或 `expires[user]` 决定。

## 多节点同步测试

- 启动两个指向同一 Redis 的应用实例，配置相同的 `topic`。
- 在实例 A 上执行 `evict` 或 `clear`，会发布 `CacheMessage`：`src/main/java/com/peach/redis/manager/MultiCache.java:176`
- 实例 B 通过 `RedisMessageListenerContainer` 接收并清理本地 Caffeine：`src/main/java/com/peach/redis/listener/CacheMessageListener.java:24`
