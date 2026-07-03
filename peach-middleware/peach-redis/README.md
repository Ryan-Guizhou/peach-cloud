# peach-redis

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-redis`  
类型：Redis 中间件聚合模块

## 模块定位

`peach-redis` 聚合 Redis 通用配置、多级缓存、Redis Stream 和 Redis 工具类能力。业务模块通过对应 starter 接入 Redis 能力，不重复编写基础 DAO、缓存和 Stream 消费样板代码。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-redis-common` | Redis 基础配置和常量 |
| `peach-redis-tool-autoconfigure` | `RedisDao` 工具自动配置 |
| `peach-redis-tool-starter` | Redis 工具 starter |
| `peach-redis-multicache-autoconfigure` | 多级缓存自动配置，配置前缀 `peach.multicache` |
| `peach-redis-multicache-starter` | 多级缓存 starter |
| `peach-redis-stream-autoconfigure` | Redis Stream 自动配置 |
| `peach-redis-stream-starter` | Redis Stream starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `RedisDao` | Redis 常用操作接口 |
| `MultiCacheConfig` | 多级缓存配置 |
| `RedisStreamProperties` | Redis Stream 配置 |
| `MessageConsumer` | Stream 消费接口 |

## 接入方式

按需引入对应 starter：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-tool-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-multicache-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redis-stream-starter</artifactId>
</dependency>
```

## 边界与限制

- 本模块不部署 Redis 服务端。
- 多级缓存需要明确本地缓存和 Redis 缓存的一致性边界。
- Redis Stream 消费需要业务侧处理重复投递、ACK、死信和积压。
- 生产 Redis 密码、集群地址、哨兵地址不能写死在仓库。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-redis -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| Redis 连接失败 | 地址、端口、密码、数据库编号是否正确 | 先用 Redis CLI 验证连接 |
| `RedisDao` 未注入 | 是否引入 tool starter | 检查依赖和自动配置条件 |
| 缓存读到旧值 | 本地缓存淘汰和 Redis 更新是否同步 | 缩短 TTL 或增加主动失效 |
| Stream 消息重复 | ACK、消费组、幂等处理是否正确 | 为消息定义业务幂等键 |
