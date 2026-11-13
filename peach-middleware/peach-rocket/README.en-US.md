# peach-rocket

English | [中文](README.md)

A RocketMQ integration starter providing unified publishing, annotation routing, dynamic consumers, consumer idempotency, transaction messages, authenticated encrypted envelopes, and Outbox. Production acceptance has not passed. The [24-item remediation status (Chinese)](docs/production-remediation-status.md) separates repository changes from pending external acceptance; the [original audit (Chinese)](docs/production-readiness-audit-2026-11-13.md) remains a historical snapshot.

## Integration

| Module | Responsibility |
| --- | --- |
| peach-rocket-autoconfigure | Public APIs, defaults, auto-configuration, SPI |
| peach-rocket-starter | Business entry point, aggregating RocketMQ Spring and Admin runtime dependencies |
| peach-rocket-jdbc | Explicitly optional durable idempotency/Outbox, requiring a DataSource and DDL |
| peach-rocket-quickstart | Real-Broker and in-process samples/tests |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
```

Minimal development configuration (without production durability/security):

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-service-producer
peach:
  rocket:
    namespace: dev
    app-name: order-service
    topic:
      auto-create: false
```

Pre-create `dev-biz-order` for the current naming settings. Publish through `MqPublisher.publish(event)`; an event can declare `@MqEvent(topic="order", tag="created", key="#orderId")`. Consumers implement `MqMessageHandler<OrderCreatedEvent>` and declare `@MqConsumer(topic="order", tag="created", consumerGroup="order-created-consumer")`. Annotation keys only allow read-only property paths; runtime `MqSendOptions.key` is literal text.

## Production boundaries

- Default in-memory stores are development-only. Explicitly enable `production=true`, durable/fenced/renewable stores, ACL/TLS, and AES_GCM_AAD encryption of all payloads without legacy downgrade. The platform pre-creates Topics.
- Deduplication defaults to stable envelope `messageId` as the event ID; business query keys and Broker message IDs cannot replace it. Business side effects still require unique constraints, state machines, or downstream idempotency.
- Outbox is disabled by default. Ordinary production publish requires a business transaction on the same DataSource; `publishStandalone` explicitly publishes independently. Sends and database ACKs are not atomic: delivery remains at-least-once, timeouts may follow delivery, and one-way sends have no Broker ACK.
- Ordinary orderly sends require a stable shardingKey. Production rejects orderly Outbox and broadcasting. Strict global ordering across instances is Unsupported.
- Bounded encoding, envelope/Header/JSON sizes, publishing/consumption admission, and Consumer drain still require capacity and shutdown drills across all publishing paths. SKIP requires successful durable recording. Replay is a programmatic SPI, without an HTTP management endpoint.
- Consumer Group prefixing defaults on to isolate environments. Decide Group names and initial offsets before the first deployment. Strict mode provides fail-closed checks; external acceptance remains necessary.

## Quickstart and verification

Use Java 21 with reachable NameServer/Broker. The current user-modified workspace `RocketDemoRunner` only runs ordinary publishing: orderly/idempotency demos and waiting for ordinary consumption are commented out. Publishing logs do not prove consumption. Set `quickstart.rocket.demo.enabled=false` to disable the demo. Append `--spring.profiles.active=in-process` when running the executable quickstart JAR for an offline sample.

```bash
mvn -B -ntp -pl peach-middleware/peach-rocket/peach-rocket-jdbc,peach-middleware/peach-rocket/peach-rocket-quickstart -am test -DfailIfNoTests=false
node scripts/verify-changes.mjs
```

`RocketBrokerEndToEndTest` requires explicit `PEACH_ROCKET_BROKER_TEST=true` and a real environment. Unit/Mock tests and skipped tests do not prove production acceptance. Docker engine is currently unavailable; real MySQL/Broker, multi-instance recovery, and final-artifact SCA remain pending.

## Further reading

- [Production configuration reference (Chinese)](docs/production-configuration.md): every property/default/unit/constraint, a full production example, secret sources, and native namespace distinctions.
- [Production operations (Chinese)](docs/production-operations.md): fresh-schema initialization, keys, offsets, leases, replay, cleanup, shutdown, business idempotency, and acceptance evidence.
- [Remediation status (Chinese)](docs/production-remediation-status.md), [implementation plan (Chinese)](docs/production-hardening-plan.md), [historical audit (Chinese)](docs/production-readiness-audit-2026-11-13.md).
- Versions follow the [module POM](pom.xml) and [current BOM](../../peach-dependencies/pom.xml); scan the final artifact to assess security.