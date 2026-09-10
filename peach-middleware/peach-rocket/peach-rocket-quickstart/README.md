# peach-rocket-quickstart

用于验证 `peach-rocket-starter` 的事件发送、消费者注册、幂等消费、事务消息和 JDBC Outbox 覆盖方式。

## 接入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
```

启动类为 `com.peach.rocket.quickstart.PeachRocketQuickstartApplication`。`OrderCreatedEvent`、`OrderCreatedConsumer` 和 `OrderService` 展示最小发送消费链路；`QuickstartJdbcRocketConfiguration` 展示通过 Bean 覆盖幂等与 Outbox 存储。

按 `src/main/resources/application.yml` 配置 RocketMQ NameServer、生产者和 `peach.rocket` 参数。JDBC 覆盖前先执行 `src/main/resources/schema` 下的建表脚本。

## 运行

```bash
mvn -f peach-middleware/peach-rocket/peach-rocket-quickstart/pom.xml spring-boot:run
```

quickstart 中的配置和表结构用于本地验证；生产环境需要稳定的幂等 key、持久化 Outbox、Topic 治理和消费者副作用幂等。
