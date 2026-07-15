---
name: using-peach-redis
description: 规范 peach-cloud 中 peach-redis 与 peach-redission 的连接配置、RedisDao、多级缓存、Redis Stream、Redisson 分布式锁、延迟队列、布隆过滤器和防重复执行。Use when editing Redis/Redisson code, configuring peach.redis or peach.multicache, adding cache/stream/lock/queue/bloom/repeat logic, or writing related documentation.
---

# Peach Redis And Redisson

## 工作流

1. 先判断目标属于基础连接、RedisDao、多级缓存、Stream、分布式锁、延迟队列、布隆过滤器或防重复执行。
2. 业务模块按能力依赖 starter，不直接依赖 autoconfigure，也不直接散落 Redis key。
3. 涉及模块路径、配置默认值、运行链路、历史拼写或边界时，完整读取 `references/module-guide.md`。
4. 编写业务分层代码时同时使用 `using-peach-code-skeleton`；更新 README 时同时使用 `using-peach-readme-writer`。
5. 改动后执行对应聚合模块构建、UTF-8 检查和 `git diff --check`。

## REQUIRED

- key 包含应用与业务命名空间；多租户/多用户数据包含隔离维度，敏感参数不原文入 key。
- Redis 密码、节点、payload 和完整业务 key 不得进入日志、文档或测试快照。
- 缓存写入定义 TTL、失效、回源和一致性策略。
- Stream 与延迟队列消费者必须业务幂等，并提供失败、积压和死信治理。
- 分布式锁只保护最小临界区，等待时间小于上游超时，失败路径明确。
- 布隆过滤器的“可能存在”结果必须回查权威数据源。
- 使用通配扫描或批量删除前限制匹配范围、批次、权限和执行入口。

## PREFERRED

- 在 Service 之外封装领域缓存仓储，集中管理 key、TTL 和序列化。
- 多级缓存 TTL 加抖动，明确 Pub/Sub 失效通知可能丢失。
- RedissonClient 统一配置和生命周期，避免多个客户端争用连接。
- 锁、防重、队列和缓存均建立成功率、延迟、积压或命中率指标。

## LEGACY_COMPATIBLE

- `redission`、`DistrbutedLock`、`RedisStreamContant` 等拼写仅按现有 API 兼容，不作为新增命名模板。
- `RedisDao` 是历史宽接口；新增复杂业务优先使用领域化适配器。
- 当前 Stream group 模式使用 auto-ack，修改消费语义前必须评估兼容性。
- 当前扫描清理、Caffeine strength、Stream 失败处理、延迟队列可靠性、布隆多节点缓存和防重标记写入均有已知限制，不得包装为生产强保证。

## FORBIDDEN

- 在线请求执行 `KEYS *`、无边界 scan 或大范围同步删除。
- 将 Pub/Sub 当作可靠消息，或假设队列消息绝不重复/丢失。
- 用分布式锁替代数据库唯一约束、事务或状态校验。
- 用布隆过滤器直接做授权、余额、唯一性等最终判断。
- 对不可信 Redis 数据开放任意类型反序列化。

## 代码审查重点

- 部署 mode、连接池、超时、重试和 TLS 是否适合生产拓扑。
- key 是否稳定、隔离且可治理；TTL 是否存在永久数据风险。
- 缓存更新与本地失效顺序是否存在陈旧窗口。
- Stream ACK、consumerName、重试和业务幂等是否匹配。
- 锁切面是否会被类内自调用绕过，锁粒度是否过大。
- 延迟任务 payload、重试、死信和分区数是否安全。
- 防重复 duration 为 0 与大于 0 的语义是否被正确使用。
- starter 的 optional 运行时依赖是否已由最终应用提供。
- 注解是否错误标在类上；当前锁与防重切面只匹配方法注解。

## 验证

```bash
mvn -f "peach-middleware/peach-redis/pom.xml" clean package -DskipTests -Pdevelopment
mvn -f "peach-middleware/peach-redission/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```
