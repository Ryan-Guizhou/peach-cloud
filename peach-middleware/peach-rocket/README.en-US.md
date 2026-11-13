# Peach RocketMQ Starter

English | [中文](README.md)

`peach-rocket` is a RocketMQ business integration starter. It provides unified publishing, annotation-based routing, dynamic consumer registration, consumer idempotency, error handling, transaction messages, topic management, payload encryption, and Outbox reliable messaging.

`peach-rocket-starter` aggregates RocketMQ Spring Boot and Topic Admin runtime dependencies. Business modules should not redeclare `rocketmq-spring-boot-starter` or `rocketmq-tools`. Depending on autoconfigure alone is not the supported business entry point; missing RocketMQ client or admin classes will disable the related auto-configuration.

## Structure

| Submodule | Responsibility |
| --- | --- |
| `peach-rocket-autoconfigure` | Core APIs, auto-configuration, default implementations, and SPI |
| `peach-rocket-starter` | Starter exposed to business modules |
| `peach-rocket-quickstart` | Order publish-consume, orderly publish, and in-memory idempotency samples |

```mermaid
flowchart LR
    App[Business Service] --> Starter[peach-rocket-starter]
    Starter --> Auto[peach-rocket-autoconfigure]
    Quick[peach-rocket-quickstart] --> Starter
    Auto --> Pub[MqPublisher]
    Auto --> Consumer["@MqConsumer"]
```

## Core Objects

| Object | Description |
| --- | --- |
| `MqPublisher` | Unified publish entrypoint |
| `@MqEvent` | Topic, tag, key, and version route declaration |
| `@MqConsumer` | Dynamic consumer declaration |
| `MqMessageHandler<T>` | Message handler interface |
| `MqConsumeContext` | Consume context |
| `MqSendOptions` | Per-send override options |
| `MqTransactionHandler<T>` / `@MqTransaction` | Transaction message handling |
| `MqIdempotentStore` | Consumer idempotency SPI |
| `MqOutboxStore` | Outbox storage SPI |
| `MqPayloadEncryptor`, `MqEncryptionPolicy`, `MqKeyProvider` | Payload encryption SPI |
| `MqTraceContextPropagator` | Optional MQ trace-context propagation SPI |

## Integration

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
```

Baseline configuration:

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

## Publishing and Consuming

```java
@MqEvent(topic = "order", tag = "created", key = "#orderId")
public class OrderCreatedEvent {
    private String orderId;
}

@Service
@RequiredArgsConstructor
public class OrderEventPublisher {

    private final MqPublisher mqPublisher;

    public void publish(OrderCreatedEvent event) {
        mqPublisher.publish(event);
    }
}
```

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

**Outbox reliability:** `MqOutboxStore.claimPending(batchSize)` returns `MqOutboxClaim` with a store-specific claimant token; the dispatcher now uses claim-aware `markSent` / `markFailed`. Existing store implementations can fall back to the default methods, but durable JDBC stores must enforce token fencing. Broker sends and JDBC acknowledgement are not one atomic transaction, so at-least-once delivery and consumer idempotency remain mandatory.

**Development-only in-memory Outbox:** the default `InMemoryMqOutboxStore` caps stored events at 1,024 and payload bytes at 65,536 per event. Terminal records are cleaned opportunistically after 24h, claims have a 5-minute lease, and no background cleaning threads are created. Settings: `peach.rocket.outbox.in-memory-max-entries`, `in-memory-max-payload-bytes`, `in-memory-retention`, and `in-memory-lease`. Concurrent dispatchers atomically claim and acknowledge with owner tokens, and retry remains bounded. Capacity exhaustion rejects new writes without losing unsent messages. **In-memory Outbox loses messages on restart.** Production should enable `peach.rocket.outbox.require-durable=true` and register a transactional JDBC/other durable store; startup fails otherwise. This is still at-least-once, not exactly-once.

## Consumer Backpressure and Graceful Shutdown

Dynamic consumers use RocketMQ SDK-managed worker threads without adding another fixed executor or unbounded message queue. All dynamic consumers in one JVM share a bounded handler execution budget. When `max-inflight` is saturated beyond `inflight-acquire-timeout`, concurrent listeners return `RECONSUME_LATER`, and orderly listeners return `SUSPEND_CURRENT_QUEUE_A_MOMENT`. **Unprocessed messages are not acknowledged.** Keep idempotency enabled and configure retry/DLQ policy for the expected load.

| Property (`peach.rocket.consumer.*`) | Default | Meaning |
| --- | --- | --- |
| `consume-thread-min` / `consume-thread-max` | `1` / `20` | SDK workers per individual consumer |
| `max-dynamic-consumers` | `64` | Maximum dynamically registered consumers in one JVM; fail startup if exceeded |
| `max-inflight` | `128` | Total concurrent business handlers across dynamic consumers in one JVM |
| `inflight-acquire-timeout` | `200ms` | Maximum time waiting for a handler slot; `0ms` means immediate rejection |
| `shutdown-drain-timeout` | `30s` | Maximum time to drain active handlers on shutdown |

A partial startup failure closes the failed consumer and rolls back earlier clients. Shutdown stops admission, waits up to the configured timeout, and closes all clients even when one shutdown fails. A timeout **does not guarantee business completion**; durable idempotency and retries remain necessary. These bounds apply to **one JVM only**, not a distributed or Broker-wide quota. RocketMQ SDK threads remain SDK-managed; use the existing project's virtual-thread component for separate application asynchronous work when appropriate.

**Optional parallel Outbox I/O:** Scanning and publishing remain serial by default. To parallelize ordinary messages, set `peach.rocket.outbox.parallel-enabled=true` and point `executor-bean-name` at a pre-registered bounded `peach-virtual-thread` `ExecutorService` bean (default: `rocket-outboxVirtualExecutor`). For example:

```yaml
peach:
  virtual-thread:
    groups:
      rocket-outbox:
        max-concurrency: 8
        max-pending: 32
        backpressure: REJECT
  rocket:
    outbox:
      enabled: true
      parallel-enabled: true
      executor-bean-name: rocket-outboxVirtualExecutor
```

The dispatcher awaits the batch's Future results. Rejected submissions are marked failed using the claim token and retried normally; they are not acknowledged or silently dropped. A batch containing sharding-key or delay options uses the serial path to avoid additional parallel reordering. **The current transport path does not yet honor Outbox ordered/delay options; this is addressed in phase four. Do not rely on the current Outbox implementation for ordered or delayed messages in production.** Bound executor pending/concurrency according to Broker throughput, DB pool capacity, and send latency.

## SPI Overrides

- Routing: `MqRouteResolver`, annotation-based by default.
- Codec: `MqMessageCodec`.
- Idempotency: `MqIdempotentStore`, `MqIdempotentKeyResolver`.
- Error handling: `MqErrorHandler`, `MqExceptionClassifier`.
- Encryption: `MqKeyProvider`, `MqPayloadEncryptor`, `MqEncryptionPolicy`.
- Outbox: `MqOutboxStore`, `MqOutboxPublisher`, `MqOutboxReplayService`.
- Trace context: `MqTraceContextPropagator`; a no-op is used when Tracing is not present.

Prefer explicit `@Bean` overrides for in-memory idempotency and in-memory Outbox in production.

## Quick Start

[`peach-rocket-quickstart`](./peach-rocket-quickstart/) connects to the local NameServer at `127.0.0.1:9876` and its Broker by default (`web-application-type: none`), using the starter's real `MqPublisher`. Use Java 21 and ensure the Broker is reachable from the client. Topic auto-creation is disabled; prepare `dev-biz-order`, as resolved by the current naming configuration, beforehand.

- Capability samples:
  - **Publish-consume**: `MqPublisher.publish` an order-created event; the Broker delivers it asynchronously by topic+tag
  - **Orderly publish**: `publishOrderly` uses a stable `orderId` as `shardingKey`; the paid consumer uses `ORDERLY` mode
  - **Consume idempotency**: starter default `InMemoryMqIdempotentStore`; a second `tryStart` after `tryStart + markSuccess` is rejected
- `RocketDemoRunner` runs these on startup, waiting up to 60 seconds after each publish to confirm consumption of that order; a timeout fails startup. Disable with `quickstart.rocket.demo.enabled=false`
- Transaction messages, Outbox, and ordering guarantees across multiple messages are outside this sample's verification scope
- Build: `mvn -pl peach-middleware/peach-rocket/peach-rocket-quickstart -am package -DskipTests`
- Run: `java -jar peach-middleware/peach-rocket/peach-rocket-quickstart/target/peach-rocket-quickstart-1.0.0-SNAPSHOT.jar`
- Test: `mvn -pl peach-middleware/peach-rocket/peach-rocket-quickstart -am test`

Append `--spring.profiles.active=in-process` to the run command for an offline demo. This profile disables Peach Rocket auto-configuration and excludes `RocketMQAutoConfiguration`; only this mode installs the in-process publisher and in-memory idempotency store without contacting a Broker. Capability tests always use this profile. A separate wiring test verifies that real mode selects `RocketMqPublisher`, with a transport test double.

## Production Boundaries

- Default in-memory idempotency and in-memory Outbox are for development or single-instance testing only.
- Production environments should override `MqIdempotentStore`, `MqOutboxStore`, and related SPI with explicit `@Bean`s.
- Topic auto-create is disabled by default; production topics should usually be managed by the platform.
- Orderly messages require a stable `shardingKey`.
- Outbox improves reliable delivery but does not replace business final-consistency state machines.
- Retries may invoke external systems more than once; consumers must be idempotent.
- RocketMQ Broker, NameServer, and console deployment are outside this module.
- After introducing `peach-observability-starter`, the standard Trace Context is written to message headers and restored on consume; do not set `traceparent` in business code.

## Verification

```bash
mvn -f "peach-middleware/peach-rocket/pom.xml" test
mvn -f "peach-middleware/peach-rocket/pom.xml" clean package -DskipTests -Pdevelopment
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| `MqPublisher` is not injected | `peach-rocket-starter` is present; `peach.rocket.enabled` is on | Check the dependency and auto-configuration conditions |
| Consumer is not registered | `@MqConsumer` beans are scanned; `dynamic-register` is on | Check package scan and configuration |
| Duplicate consume | Idempotency key is stable; store is production-ready | Override `MqIdempotentStore` |
| Transaction message is not checked back | `@MqTransaction` matches `MqTransactionHandler` | Check transaction handler registration |
| Outbox backlog | Dispatcher is running; store status can be updated | Check `MqOutboxStore` and scheduler logs |
| Startup reports missing `RocketMQTemplate` | Autoconfigure-only dependency; final artifact includes RocketMQ Spring | Use `peach-rocket-starter` and rebuild |
| Topic auto-create misses `DefaultMQAdminExt` | Final artifact includes `rocketmq-tools` | Use the latest `peach-rocket-starter` and rebuild |

## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as target/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
