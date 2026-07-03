# peach-redission

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-redission`  
类型：Redisson 中间件聚合模块

## 模块定位

`peach-redission` 聚合基于 Redisson 的分布式锁、延迟队列、布隆过滤器、防重复执行和公共配置能力。业务模块通过 starter 引入对应能力。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-redission-common` | Redisson 公共配置和锁信息处理 |
| `peach-redission-distributedlock-autoconfigure` / `starter` | 分布式锁和 `@DistrbutedLock` |
| `peach-redission-delayqueue-autoconfigure` / `starter` | 延迟队列和 `ConsumerTask` |
| `peach-redission-bloomfilter-autoconfigure` / `starter` | 布隆过滤器和扩展 SPI |
| `peach-redission-repeat-autoconfigure` / `starter` | 防重复执行和 `@RepeatLimit` |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `@DistrbutedLock` | 分布式锁注解，当前包名和类名按源码拼写 |
| `DistributedLocker` | 分布式锁执行接口 |
| `LockType` | 锁类型枚举 |
| `LockTimeOutStrategy` | 锁等待超时策略 |
| `ConsumerTask` | 延迟队列消费任务 |
| `BloomFilterService` | 布隆过滤器服务 |
| `KeyNamingStrategy`、`CodecProvider`、`BloomScalePolicy` | 布隆过滤器扩展 SPI |
| `@RepeatLimit` | 防重复注解 |

## 接入方式

按需引入 starter：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-distributedlock-starter</artifactId>
</dependency>
```

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-delayqueue-starter</artifactId>
</dependency>
```

## 边界与限制

- 本模块不部署 Redis / Redisson 服务端。
- 分布式锁不替代数据库唯一约束和业务幂等。
- 延迟队列任务可能重复执行，消费者必须可幂等。
- 布隆过滤器存在误判概率，不应用于需要绝对准确的授权判断。
- 防重复执行依赖 key 设计，key 不稳定会导致误拦截或漏拦截。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-redission/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-redission -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 锁注解不生效 | 是否经过 Spring AOP；是否引入 starter | 避免自调用，检查依赖和 Bean |
| 锁一直等待或超时 | 锁 key、lease time、wait time 是否合理 | 调整锁参数并检查持锁任务耗时 |
| 延迟队列不消费 | `ConsumerTask` 是否注册；Redis 是否可用 | 检查任务 Bean 和 Redisson 连接 |
| 布隆误判 | 容量和误判率配置是否合理 | 重建过滤器或调整容量参数 |
