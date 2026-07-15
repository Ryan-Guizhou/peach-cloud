# peach-rocket 模块参考

本文记录当前 RocketMQ starter 的可验证入口和生产边界；事件路由、SPI 和默认值必须以当前注解、配置类、自动配置和测试为准。

## 模块导航

```text
peach-middleware/peach-rocket/
├── pom.xml                                # 聚合模块
├── README.md
├── peach-rocket-autoconfigure/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/peach/rocket/
│       │   ├── autoconfigure/
│       │   │   ├── PeachRocketAutoConfigure.java
│       │   │   ├── PeachRocketOutboxAutoConfigure.java
│       │   │   └── PeachRocketProperties.java
│       │   ├── annotation/                # MqEvent/MqConsumer/MqTransaction
│       │   ├── core/                      # MqPublisher/MqMessageHandler 等契约
│       │   ├── producer/                  # RocketMqPublisher 实现
│       │   ├── consumer/                  # 动态消费者注册与调用
│       │   ├── outbox/                    # Outbox 能力
│       │   └── security/                  # payload 安全能力
│       └── resources/META-INF/
├── peach-rocket-starter/
│   └── pom.xml                            # 业务接入依赖
└── peach-rocket-example/
    ├── pom.xml
    └── src/main/                          # 事件、消费者、发送与 SPI 示例
```

导航时忽略 `target/` 和 `.flattened-pom.xml`。

## 可验证入口

- `MqPublisher`：统一发送契约；具体发送语义以接口和 `RocketMqPublisher` 为准。
- `@MqEvent`：事件默认 topic/tag/key/version 路由声明。
- `@MqConsumer` + `MqMessageHandler<T>`：动态消费声明和处理契约。
- `MqConsumeContext`：消息元数据与重试上下文。
- `MqSendOptions`：单次发送覆盖参数。
- `PeachRocketProperties`：`peach.rocket` 配置事实来源。

## REQUIRED

- 业务模块依赖 `peach-rocket-starter`，不混用 RocketMQ Spring 原生消费注解绕过统一治理。
- topic、tag、consumerGroup 和 shardingKey 必须稳定且无环境硬编码冲突。
- 消费者按至少一次投递设计；幂等 key 必须与业务唯一性一致。
- 重试前评估重复扣减、重复调用外部系统和重复落库等副作用。
- payload、密钥、token 和完整敏感报文不得写入日志。
- 可靠投递根据一致性需求选择事务消息或 Outbox；同步发送成功不等于业务最终一致。

## PREFERRED

- 事件通过 `@MqEvent` 声明默认路由，仅在明确场景使用 `MqSendOptions` 覆盖。
- 生产环境显式提供持久化 `MqIdempotentStore`/`MqOutboxStore`，不依赖单机内存实现。
- Topic 自动创建只在开发测试或平台授权环境开启。
- 加密通过统一 annotation/strategy/SPI 完成，不在业务代码手写加解密。

## LEGACY_COMPATIBLE

- 当前动态消费者、内存幂等和内存 Outbox 可用于兼容开发/单实例场景，不代表生产默认安全方案。
- 配置示例只用于说明结构；默认值以 `PeachRocketProperties` 和自动配置条件为准。

## 配置示意

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: local-producer
peach:
  rocket:
    enabled: true
    namespace: dev
    app-name: local-service
    topic:
      auto-create: false
    outbox:
      enabled: false
```

## 验证

```bash
mvn -f "peach-middleware/peach-rocket/pom.xml" test
mvn -f "peach-middleware/peach-rocket/pom.xml" -DskipTests package
node scripts/check-utf8.mjs
```
