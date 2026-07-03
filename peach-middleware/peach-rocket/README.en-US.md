# Peach RocketMQ Starter

English | [中文](README.md)

## Purpose

`peach-rocket` is a RocketMQ business integration starter. It provides unified publishing, annotation-based routing, dynamic consumer registration, consumer idempotency, error handling, transaction messages, topic management, payload encryption, and Outbox reliable messaging.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-rocket-autoconfigure` | Core APIs, auto-configuration, default implementations, and SPI |
| `peach-rocket-starter` | Starter exposed to business modules |
| `peach-rocket-example` | Example application and JDBC idempotency/Outbox override samples |

## Core Objects

- `MqPublisher`: unified publish entrypoint.
- `@MqEvent`: topic, tag, key, and version route declaration.
- `@MqConsumer`: dynamic consumer declaration.
- `MqMessageHandler<T>`: message handler interface.
- `MqSendOptions`: per-send override options.
- `MqIdempotentStore`: consumer idempotency SPI.
- `MqOutboxStore`: Outbox storage SPI.
- `MqPayloadEncryptor`, `MqEncryptionPolicy`, `MqKeyProvider`: payload encryption SPI.

## Configuration Example

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-service-producer

peach:
  rocket:
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

## Publishing Example

```java
@MqEvent(topic = "order", tag = "created", key = "#orderId")
public class OrderCreatedEvent {
    private String orderId;
}

@Resource
private MqPublisher mqPublisher;

public void publish(OrderCreatedEvent event) {
    mqPublisher.publish(event);
}
```

## Consumer Example

```java
@Component
@MqConsumer(topic = "order", tag = "created", consumerGroup = "order-created-consumer")
public class OrderCreatedConsumer implements MqMessageHandler<OrderCreatedEvent> {
    @Override
    public void handle(OrderCreatedEvent message, MqConsumeContext context) {
        // handle message
    }
}
```

## Production Boundaries

- Default in-memory idempotency and in-memory Outbox are for development or single-instance testing only.
- Production environments should override `MqIdempotentStore`, `MqOutboxStore`, and related SPI with explicit `@Bean`s.
- Topic auto-create is disabled by default; production topics should usually be managed by the platform.
- RocketMQ Broker, NameServer, and console deployment are outside this module.
- Outbox improves reliable delivery but does not replace business final-consistency state machines.

## Verification

```bash
mvn -f "peach-middleware/peach-rocket/pom.xml" -DskipTests package
```
