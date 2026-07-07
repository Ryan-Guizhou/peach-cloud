# peach-rocket 模块参考

## 模块路径

- 聚合模块：`peach-middleware/peach-rocket`
- 自动配置：`peach-middleware/peach-rocket/peach-rocket-autoconfigure`
- 对外 starter：`peach-middleware/peach-rocket/peach-rocket-starter`
- 示例：`peach-middleware/peach-rocket/peach-rocket-example`

## 核心 API

- `MqPublisher`：统一发送入口，支持同步、异步、单向、顺序、延迟、事务消息。
- `@MqEvent`：事件路由声明，字段包括 `topic`、`tag`、`key`、`version`。
- `@MqConsumer`：动态消费者声明，字段包括 `topic`、`tag`、`consumerGroup`、`consumeMode`、`messageModel`、`maxReconsumeTimes`、`idempotent`。
- `MqMessageHandler<T>`：业务消费者处理接口。
- `MqConsumeContext`：消费上下文，暴露 messageId、topic、tag、key、headers、重试次数等。
- `MqSendOptions`：单次发送覆盖参数，包括 topic、tag、key、timeout、headers、delay 等。

## 配置基线

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-service-producer

peach:
  rocket:
    enabled: true
    namespace: dev
    app-name: order-service
    naming:
      topic-prefix: biz
      topic-separator: "-"
      auto-prefix-env: true
    consumer:
      dynamic-register: true
      enable-idempotent: true
    topic:
      auto-create: false
    outbox:
      enabled: false
```

## 默认实现与可覆盖 SPI

- 路由：`MqRouteResolver`，默认 `AnnotationMqRouteResolver`。
- 编解码：`MqMessageCodec`，默认 Jackson，启用安全能力后使用安全编解码。
- 幂等：`MqIdempotentStore`、`MqIdempotentKeyResolver`，默认内存实现只适合开发/单实例。
- 错误处理：`MqErrorHandler`、`MqExceptionClassifier`。
- 加密：`MqKeyProvider`、`MqPayloadEncryptor`、`MqEncryptionPolicy`。
- Outbox：`MqOutboxStore`、`MqOutboxPublisher`、`MqOutboxReplayService`。

## 示例位置

- 基础配置：`peach-rocket-example/src/main/resources/application.yml`
- 事件示例：`peach-rocket-example/src/main/java/com/peach/rocket/example/event`
- 消费者示例：`peach-rocket-example/src/main/java/com/peach/rocket/example/consumer`
- 发送示例：`peach-rocket-example/src/main/java/com/peach/rocket/example/service/OrderService.java`
- JDBC 幂等/Outbox 覆盖：`peach-rocket-example/src/main/java/com/peach/rocket/example/config`
- 表结构示例：`peach-rocket-example/src/main/resources/schema`

## 构建验证

```bash
mvn -f "peach-middleware/peach-rocket/pom.xml" test
mvn -f "peach-middleware/peach-rocket/pom.xml" -DskipTests package
```
