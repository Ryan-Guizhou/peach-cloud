---
name: using-peach-rocket
description: 修改或审查 peach-middleware/peach-rocket 的 @MqEvent、@MqConsumer、MqPublisher、顺序消息、事务消息、Outbox、消费幂等、加密、Topic 治理、Store SPI 和 RocketMQ 可靠性边界时使用。
---

# Peach Rocket

## 核心不变量

- 业务依赖 starter 并使用 Peach 对外契约，不把 RocketMQ 原生 API/注解散落到业务层。
- 事件路由保持稳定；顺序消息使用稳定 shardingKey。
- At-Least-Once 是默认可靠性心智模型；消费者必须业务幂等，不能因为框架存在幂等 Store 就假设 Exactly-Once。
- 需要可靠投递时明确选择事务消息或 Outbox；“发送返回成功”不等于业务最终一致。
- 生产环境不把内存 `MqIdempotentStore` / `MqOutboxStore` 描述成持久化或集群级保证。
- Topic 自动创建默认只用于受控开发环境；生产 Topic 由治理流程管理。
- payload 加密通过现有安全配置/注解/SPI，不在业务代码手写另一套协议。
- 重试必须评估外部副作用、重复扣减、重复回调和状态更新失败。

事务/Outbox 变化验证业务事务与消息持久化的真实提交顺序、重启恢复、并发领取、重放和重复消费。具体 API、配置、SPI 按需读取现有 `references/module-guide.md`。

需要文档时进入 `project-doc-engineer`。
