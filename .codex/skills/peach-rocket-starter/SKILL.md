---
name: peach-rocket-starter
description: 规范 peach-cloud 项目中 peach-rocket-starter / peach-rocket-autoconfigure / peach-rocket-example 的 RocketMQ 接入、事件建模、生产消费、事务消息、Outbox、幂等、加密、Topic 治理和排障。Use when editing or reviewing RocketMQ code, adding @MqEvent/@MqConsumer/MqPublisher usage, extending peach-rocket SPI, or writing README for peach-middleware/peach-rocket.
---

# Peach Rocket Starter

## 工作流

1. 先读取当前任务涉及的源码，不直接照搬旧 README；旧 README 可能存在编码显示问题。
2. 修改业务接入时，优先使用 `peach-rocket-starter` 对外暴露的 API，不直接散落使用 RocketMQ 原生注解和客户端。
3. 修改 starter 能力时，保持三段式结构：`peach-rocket-autoconfigure` 放核心 API/自动配置/默认实现，`peach-rocket-starter` 只做依赖聚合，`peach-rocket-example` 放可运行示例和业务覆盖示例。
4. 需要详细模块边界、配置项、SPI、示例路径时，读取 `references/module-guide.md`。
5. 完成后至少运行与改动范围匹配的 Maven 校验；无法运行时说明原因和残余风险。

## 使用规则

- 引入依赖时使用 `com.peach:peach-rocket-starter`，不要让业务模块直接依赖 `peach-rocket-autoconfigure`。
- 发送消息时注入 `MqPublisher`，优先让事件类通过 `@MqEvent(topic, tag, key, version)` 声明默认路由。
- 消费消息时让 Spring Bean 实现 `MqMessageHandler<T>` 并标注 `@MqConsumer`；不要同时混用 RocketMQ Spring 原生消费注解。
- 顺序消息必须提供稳定的 `shardingKey`，通常使用订单号、聚合根 ID 或业务实体 ID。
- 需要可靠投递时优先评估事务消息或 Outbox，不要只依赖同步发送成功作为业务最终一致性保障。
- 生产环境不要依赖默认内存幂等或内存 Outbox；通过显式 `@Bean` 覆盖 `MqIdempotentStore`、`MqOutboxStore` 等 SPI。
- Topic 自动创建默认应保持关闭；只在开发、测试或平台明确授权环境开启。
- 加密 payload 时统一通过 `peach.rocket.security.*`、`@MqEncrypted` 或加密策略 SPI，不在业务代码手写加解密。

## 代码审查重点

- 检查 `topic`、`tag`、`consumerGroup` 是否稳定、可读、无环境硬编码冲突。
- 检查消费者是否具备幂等语义；即使框架启用幂等，也要确认幂等 key 与业务唯一性一致。
- 检查失败重试是否会重复调用外部系统、重复扣减、重复落库。
- 检查 `MqSendOptions` 覆盖路由时是否破坏事件注解约定。
- 检查 `outbox.enabled=true` 时是否存在生产级 `MqOutboxStore`。
- 检查事务消息是否存在匹配的 `@MqTransaction` / `MqTransactionHandler<T>`。

## README 提醒

编辑 `peach-middleware/peach-rocket` 或子模块后，使用 `$peach-readme-writer` 刷新 README。README 必须写清楚能力边界、接入示例、有效配置、SPI 覆盖方式、构建验证和排障表。
