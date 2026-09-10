---
name: using-peach-redis
description: 修改或审查 peach-redis / peach-redission 的 RedisDao、多级缓存、Redis Stream、Redisson 分布式锁、延迟队列、布隆过滤器、防重复执行、key/TTL/序列化和连接配置时使用。
---

# Peach Redis And Redisson

## 核心不变量

- key 有稳定应用/业务命名空间；多租户/多用户数据包含隔离维度，敏感参数不原文入 key。
- 缓存明确 TTL、失效、回源和一致性策略；多级缓存承认本地失效传播存在窗口。
- Redis Stream、延迟队列等消费者必须业务幂等，并明确 ACK、重试、积压和死信/补偿治理。
- 分布式锁只覆盖最小临界区，等待小于上游超时；锁不能替代数据库唯一约束、事务或业务状态校验。
- 布隆过滤器“可能存在”必须回查权威数据源，不能直接用于授权、余额、唯一性最终判断。
- 禁止在线请求 `KEYS *`、无边界 scan 或大范围同步删除。
- Pub/Sub 不作为可靠消息保证；队列消息不能假设绝不重复或丢失。
- RedissonClient/连接资源统一生命周期，不为单个业务创建独立客户端。

历史 `redission`、`DistrbutedLock` 等拼写只做兼容，不作为新命名模板。具体配置和已知限制按需读取现有 `references/module-guide.md`。

需要文档时进入 `project-doc-engineer`。
