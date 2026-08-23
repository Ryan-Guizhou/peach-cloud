# peach-scheduler

[English](README.en-US.md) | 中文

- 最后更新时间：2026-08-14
- artifactId：`peach-scheduler`
- 类型：定时任务执行侧组件（SDK + Provider SPI + RocketMQ Transport）
- 适用版本：Java 21、Spring Boot 3.5.4

## 模块定位

`peach-component/peach-scheduler` 是 Peach Cloud 分布式定时任务的**执行侧组件**。业务微服务通过它注册 `@PeachJob` Handler、消费控制面下发的执行命令、Claim 租约后执行业务逻辑，并回传结果。

**本组件提供：**

- 业务任务 SDK（`@PeachJob`、`JobHandler`、`PeachJobExecutor`）
- 调度 Provider SPI 与 Quartz 默认实现
- RocketMQ 执行命令/结果传输与 JDBC Outbox/消费幂等适配
- `peach-scheduler-starter` 聚合依赖

**本组件不提供：**

- 任务定义、执行历史、页面 API、状态机与人工审计（由根目录 `peach-scheduled` 控制面负责）
- Exactly-Once 语义；生产可靠性为 `At-Least-Once + JDBC Claim + 业务幂等`

与 `peach-scheduled` 的关系：

| 模块 | 角色 |
| --- | --- |
| `peach-scheduler` | 嵌入业务服务的执行 SDK |
| `peach-scheduled` | 独立部署的调度控制面服务 |

## 目录结构

```text
peach-component/peach-scheduler/
├── peach-scheduler-core/              # 契约、注解、SPI 接口
├── peach-scheduler-autoconfigure/     # 自动配置、默认执行器
├── peach-scheduler-provider-quartz/   # Quartz SchedulingProvider
├── peach-scheduler-transport-rocket/  # RocketMQ + JDBC 持久化
├── peach-scheduler-starter/           # 业务接入 starter
├── peach-scheduler-example/           # 本地示例
└── docs/architecture/                 # 架构图源文件
```

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-scheduler-core` | `@PeachJob`、`JobHandler`、`JobRegistry`、Provider/Dispatcher/Transport 契约 |
| `peach-scheduler-autoconfigure` | `PeachSchedulerAutoConfiguration`、Handler 扫描注册、`DefaultPeachJobExecutor` |
| `peach-scheduler-provider-quartz` | `QuartzSchedulingProvider`、Quartz 触发桥接（主要用于控制面） |
| `peach-scheduler-transport-rocket` | 执行结果 Outbox、`SchedulerJdbcMqOutboxStore`、`SchedulerJdbcMqIdempotentStore` |
| `peach-scheduler-starter` | 聚合 `autoconfigure` + `transport-rocket` |
| `peach-scheduler-example` | 示例 Consumer 与本地 Claim 桩 |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `@PeachJob` | 声明 Handler 名称与描述，注册到 `JobRegistry` |
| `JobHandler` | 业务任务入口，接收 `JobContext`，返回 `JobResult` |
| `PeachJobExecutor` | 执行编排：校验 → Claim → 线程池提交 → 结果上报 |
| `ExecutionLeaseClient` | 向控制面抢占 execution 租约（生产由 Feign 实现） |
| `ExecutionResultReporter` | 上报执行结果到 RocketMQ Outbox |
| `SchedulingProvider` | 调度引擎 SPI（schedule/reschedule/pause/resume/delete/trigger） |
| `JobDispatcher` | 控制面将 execution 分发给业务服务的 SPI |

## 快速接入

### Maven 依赖

业务服务生产接入通常需要：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-scheduler-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-threadpool-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-rocket-starter</artifactId>
</dependency>
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-scheduled-openfeign-external</artifactId>
</dependency>
```

`peach-scheduled-openfeign-external` 提供 Claim 与 Handler 注册 Feign 客户端；`peach-threadpool-starter` 提供 `PoolType.SCHEDULED` 执行线程池。

### 配置示例

```yaml
spring:
  application:
    name: order-service

peach:
  scheduler:
    enabled: true
    rocket:
      require-jdbc: true
    executor:
      application-name: ${spring.application.name}
      default-timeout-ms: 1800000
  threadpool:
    pools:
      - type: SCHEDULED
        core-size: 4
        max-size: 16
        queue-capacity: 200
        thread-name-prefix: scheduler-business-
        rejected-policy: ABORT
  rocket:
    enabled: true
    app-name: ${spring.application.name}
    consumer:
      dynamic-register: true
      enable-idempotent: true
    outbox:
      enabled: true
    topic:
      auto-create: false
```

### 实现 Handler

```java
@Indexed
@Component
@PeachJob(value = "orderTimeoutCloseJob", description = "关闭超时未支付订单")
public class OrderTimeoutCloseJob implements JobHandler {

    @Override
    public JobResult execute(JobContext context) {
        // 基于 context.getExecutionId() 或稳定业务键做幂等
        return JobResult.success();
    }
}
```

### 声明 MQ Consumer

`@MqConsumer` 的 topic 必须是静态注解值。Topic 规则：`scheduler-execute-{spring.application.name}`。

```java
@Component
@MqConsumer(topic = "scheduler-execute-order-service",
        tag = "execute", consumerGroup = "order-service-executor", idempotent = true)
public class SchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {

    private final PeachJobExecutor executor;

    public SchedulerExecutionConsumer(PeachJobExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {
        executor.execute(message);
    }
}
```

参考：`peach-scheduler-example` 中的 `DemoSchedulerExecutionConsumer`。

## 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.scheduler.enabled` | `true` | 是否启用执行器自动配置 |
| `peach.scheduler.executor.application-name` | 无 | 必须与控制面任务定义中的 `applicationName` 一致 |
| `peach.scheduler.executor.instance-id` | 运行时生成 | 执行器实例标识，Claim 时上报 |
| `peach.scheduler.executor.default-timeout-ms` | `1800000` | Handler 未指定超时时的默认等待毫秒数 |
| `peach.scheduler.executor.max-error-message-length` | `1000` | 回传错误摘要最大长度 |
| `peach.scheduler.executor.handler-heartbeat-ms` | `60000` | Handler 注册心跳间隔（需引入 `openfeign-external`） |
| `peach.scheduler.rocket.require-jdbc` | `false` | 为 `true` 时强制 JDBC Outbox/幂等，禁止内存实现 |
| `peach.scheduler.quartz.group` | `PEACH_SCHEDULER` | Quartz Provider 的 Job/Trigger 分组 |

## 运行机制

```text
控制面 Outbox → RocketMQ scheduler-execute-{app}
  → 业务 @MqConsumer
  → PeachJobExecutor.execute()
  → ExecutionLeaseClient.claim(executionId)   # 失败则丢弃，不执行 Handler
  → ThreadPoolManager / PoolType.SCHEDULED
  → @PeachJob JobHandler
  → ExecutionResultReporter → scheduler-execution-result
```

架构图源：

- `docs/architecture/architecture.mmd`
- `docs/architecture/execution-flow.mmd`
- `docs/architecture/peach-scheduler-architecture.drawio`

关键原则：

1. **Trigger 与 Execute 分离**：Quartz 只在控制面触发，业务 Handler 在业务服务执行。
2. **Claim 先于执行**：重复 MQ 消息在 Claim 阶段被挡掉。
3. **业务幂等兜底**：Claim 无法覆盖“已执行但结果丢失”等场景，Handler 必须幂等。

## Provider SPI

新增调度引擎时实现 `SchedulingProvider`，`getProviderId()` 使用稳定字符串（非封闭枚举）。Quartz 实现边界：

- `PEACH_SCHEDULER_*` 是事实源，`QRTZ_*` 只是运行时投影。
- Quartz Job 只调用 `ScheduleTriggerHandler`，不直接执行业务 Handler。

推荐独立模块：`peach-scheduler-provider-{name}/`，并通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册。

## SQL

脚本位于仓库根目录 `sql/`，说明见 `sql/README.md`。

| 脚本 | 用途 |
| --- | --- |
| `MQ_OUTBOX_EVENT.sql` | 执行命令/结果可靠发送 |
| `MQ_CONSUME_RECORD.sql` | 业务侧 MQ 消费幂等 |

控制面事实表见 `PEACH_SCHEDULER_*.sql`、`QRTZ_MYSQL.sql`。

## 边界与限制

- Handler 不得依赖 Quartz API，不得动态执行页面传入的 class/method/SpEL/Shell/SQL。
- 禁止自建游离线程池；统一使用 `PoolType.SCHEDULED`。
- 日志只记录 `executionId`、`jobCode`、`handlerName` 等稳定字段，禁止输出完整参数 JSON 或凭据。
- `require-jdbc=true` 时若最终装配内存 Outbox/幂等存储，应用启动 fail-fast。
- 超时仅表示控制面等待超时，不宣称回滚外部副作用。

## 构建与验证

```bash
mvn -f peach-component/peach-scheduler/pom.xml -Dmaven.test.skip=true package -Pdevelopment
node scripts/check-utf8.mjs
git diff --check -- peach-component/peach-scheduler
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `PeachJobExecutor` 未创建 | 是否缺少 `ExecutionLeaseClient` / `ExecutionResultReporter` / `ThreadPoolManager` | 补齐 starter 与 Feign/Rocket 依赖 |
| Claim 始终失败 | 控制面 execution 状态、Same-Token、`applicationName` 是否一致 | 核对 Feign 目标服务 `peach-scheduler` 与内部接口可达 |
| Handler 未出现在控制面白名单 | `peach.scheduler.executor.application-name`、Feign 注册是否成功 | 检查 `openfeign-external` 与 `/internal/scheduler/handlers/register` |
| 重复执行业务副作用 | 仅依赖 Claim 不够 | 在 Handler 内基于 `executionId` 做幂等 |
| 启动 fail-fast（durability） | `require-jdbc=true` 但无 JDBC 表或 Bean | 执行 `MQ_*.sql` 并确认未使用内存 Store |
| MQ 消费不到消息 | Topic 名称是否与 `scheduler-execute-{app}` 完全一致 | 对照 `SchedulerConstants.executionTopic()` 修正 `@MqConsumer` |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
