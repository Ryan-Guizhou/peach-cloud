# peach-scheduler-quickstart

用于验证调度 starter 的定义同步、触发、RocketMQ 执行分发、租约 claim、业务 Handler 执行和结果上报链路。

## 接入

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-scheduler-starter</artifactId>
</dependency>
```

启动类为 `com.peach.scheduler.quickstart.PeachSchedulerQuickstartApplication`。`DemoCleanupJob` 是业务 Handler，`DemoSchedulerExecutionConsumer` 是执行端消息接入，`DemoSchedulerIntegrationConfiguration` 提供 quickstart 的本地租约实现。

阻塞业务执行使用 `peach.virtual-thread.groups.scheduler` 配置的虚拟线程组；Quartz 触发线程和 RocketMQ 消费生命周期仍由各自组件管理。

## 运行

```bash
mvn -f peach-component/peach-scheduler/peach-scheduler-quickstart/pom.xml spring-boot:run
```

运行前准备 RocketMQ，并按 `src/main/resources/application.yml` 配置 NameServer、执行组和 scheduler 参数。quickstart 中的内存租约实现只用于本地验证，生产环境应接入持久化租约和可靠消息实现。
