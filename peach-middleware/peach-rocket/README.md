# peach-rocket

[English](README.en-US.md) | 中文

RocketMQ 业务接入 Starter，提供统一发送、注解路由、动态消费者、消费幂等、事务消息、认证加密信封和 Outbox。生产验收尚未通过；[24 项整改状态](docs/production-remediation-status.md)记录代码整改与外部待验收项，[原审计](docs/production-readiness-audit-2026-11-13.md)保留历史快照。

## 接入

| 模块 | 职责 |
| --- | --- |
| peach-rocket-autoconfigure | 公共 API、默认实现、自动配置、SPI |
| peach-rocket-starter | 业务入口，聚合 RocketMQ Spring 和 Admin 运行依赖 |
| peach-rocket-jdbc | 显式可选的持久化幂等/Outbox，需要数据源与 DDL |
| peach-rocket-quickstart | 真实 Broker 和 in-process 样例/测试 |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
```

开发最小配置（不具备生产持久化与安全基线）：

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

预建当前命名对应的 `dev-biz-order`。用 `MqPublisher.publish(event)` 发布，事件可声明 `@MqEvent(topic="order", tag="created", key="#orderId")`；消费者实现 `MqMessageHandler<OrderCreatedEvent>` 并声明 `@MqConsumer(topic="order", tag="created", consumerGroup="order-created-consumer")`。注解 Key 只允许只读属性路径，运行时 `MqSendOptions.key` 是字面值。

## 生产边界

- 默认内存 Store 仅限开发；生产显式启用 `production=true`、持久化/fencing/续租 Store、ACL/TLS 和 AES_GCM_AAD 全量加密，禁止历史降级。Topic 由平台预建。
- 默认以稳定信封 `messageId` 为事件去重 ID；业务查询 Key 与 Broker 消息 ID 不替代事件 ID。业务副作用仍需唯一约束、状态机或下游幂等。
- Outbox 默认关闭；生产普通 publish 要求同 DataSource 业务事务，`publishStandalone` 显式独立发布。发送/数据库 ACK 不原子，始终 at-least-once；超时可能已经投递，单向发送无 Broker ACK。
- 普通顺序发送需稳定 shardingKey；生产拒绝顺序 Outbox 与广播。跨实例严格全局顺序 Unsupported。
- 有界编码、报文/Header/JSON、发送/消费准入与 Consumer 排空不替代实际容量和全发送路径停机演练。可靠记录成功才允许 SKIP；Replay 是程序化 SPI，未提供 HTTP 管理入口。
- Consumer Group 前缀默认关闭以保护旧位点，严格生产要求开启；必须先迁移位点/去重域。严格模式是失败关闭保护，不等于外部环境验收通过。

## Quickstart 与验证

使用 Java 21 与可达 NameServer/Broker。当前工作区用户修改后的 `RocketDemoRunner` 只执行普通发布，顺序/幂等演示和普通消费等待已注释；发布日志不证明消费完成。设置 `quickstart.rocket.demo.enabled=false` 关闭演示。运行 quickstart 可执行 JAR 时追加 `--spring.profiles.active=in-process` 使用离线样例。

```bash
mvn -B -ntp -pl peach-middleware/peach-rocket/peach-rocket-jdbc,peach-middleware/peach-rocket/peach-rocket-quickstart -am test -DfailIfNoTests=false
node scripts/verify-changes.mjs
```

`RocketBrokerEndToEndTest` 需显式启用 `PEACH_ROCKET_BROKER_TEST=true` 与真实环境。单测/Mock 或 skipped 不证明生产验收通过。当前 Docker engine 不可用，真实 MySQL/Broker、多实例故障与最终产物 SCA 仍待完成。

## 深入阅读

- [生产配置 Reference](docs/production-configuration.md)：所有属性默认值/单位/约束、完整生产示例、secret 来源与原生 namespace 区别。
- [生产运维与迁移](docs/production-operations.md)：新库/存量库、旧协议、密钥、位点、租约、重放、清理、停机、业务幂等及验收证据。
- [整改状态](docs/production-remediation-status.md)、[实施计划](docs/production-hardening-plan.md)、[历史审计](docs/production-readiness-audit-2026-11-13.md)。
- 依赖版本以[模块 POM](pom.xml)和[当前 BOM](../../peach-dependencies/pom.xml)为准，最终产物安全性仍需扫描。