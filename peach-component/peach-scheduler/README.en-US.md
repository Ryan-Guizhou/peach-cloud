# peach-scheduler

English | [中文](README.md)

- Last updated: 2026-08-14
- artifactId: `peach-scheduler`
- Type: distributed scheduler executor SDK (Provider SPI + RocketMQ transport)
- Target stack: Java 8, Spring Boot 2.7.13

## Purpose

`peach-component/peach-scheduler` is the **executor-side** component of Peach Cloud scheduling. Business services use it to register `@PeachJob` handlers, consume execution commands from the control plane, claim leases, run business logic, and report results.

**Provided:**

- Business task SDK (`@PeachJob`, `JobHandler`, `PeachJobExecutor`)
- Scheduling Provider SPI and the default Quartz implementation
- RocketMQ command/result transport with JDBC Outbox and consume-idempotency adapters
- `peach-scheduler-starter` aggregation dependency

**Not provided:**

- Job definitions, execution history, admin APIs, state machines, or manual audit (handled by `peach-scheduled`)
- Exactly-once semantics; production reliability is `At-Least-Once + JDBC Claim + idempotent handlers`

| Module | Role |
| --- | --- |
| `peach-scheduler` | Executor SDK embedded in business services |
| `peach-scheduled` | Standalone scheduler control-plane service |

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-scheduler-core` | Annotations, contracts, Provider/Dispatcher/Transport SPI |
| `peach-scheduler-autoconfigure` | Auto-configuration, handler registration, default executor |
| `peach-scheduler-provider-quartz` | Quartz `SchedulingProvider` and trigger bridge |
| `peach-scheduler-transport-rocket` | RocketMQ transport and JDBC durability stores |
| `peach-scheduler-starter` | Business integration starter |
| `peach-scheduler-example` | Local integration example |

## Core Objects

| Object | Description |
| --- | --- |
| `@PeachJob` | Declares handler name and description |
| `JobHandler` | Business entry point |
| `PeachJobExecutor` | Orchestrates claim, thread-pool execution, and result reporting |
| `ExecutionLeaseClient` | Claims execution lease from the control plane |
| `ExecutionResultReporter` | Publishes execution results via RocketMQ Outbox |
| `SchedulingProvider` | Scheduling engine SPI |

## Quick Start

### Maven

Typical production dependencies:

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

### Configuration

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
        rejected-policy: ABORT
  rocket:
    enabled: true
    consumer:
      enable-idempotent: true
    outbox:
      enabled: true
```

### Handler

```java
@Component
@PeachJob(value = "orderTimeoutCloseJob", description = "Close unpaid orders")
public class OrderTimeoutCloseJob implements JobHandler {
    @Override
    public JobResult execute(JobContext context) {
        return JobResult.success();
    }
}
```

### MQ Consumer

Topic pattern: `scheduler-execute-{spring.application.name}`.

```java
@MqConsumer(topic = "scheduler-execute-order-service",
        tag = "execute", consumerGroup = "order-service-executor", idempotent = true)
public class SchedulerExecutionConsumer implements MqMessageHandler<JobExecutionCommand> {
    private final PeachJobExecutor executor;
    @Override
    public void handle(JobExecutionCommand message, MqConsumeContext context) {
        executor.execute(message);
    }
}
```

See `DemoSchedulerExecutionConsumer` in `peach-scheduler-example`.

## Configuration Keys

| Key | Default | Description |
| --- | --- | --- |
| `peach.scheduler.enabled` | `true` | Enable executor auto-configuration |
| `peach.scheduler.executor.application-name` | none | Must match control-plane job `applicationName` |
| `peach.scheduler.executor.default-timeout-ms` | `1800000` | Default handler wait timeout |
| `peach.scheduler.rocket.require-jdbc` | `false` | Fail fast if durable JDBC stores are missing |
| `peach.scheduler.quartz.group` | `PEACH_SCHEDULER` | Quartz job/trigger group |

## Runtime Flow

```text
Control-plane Outbox → RocketMQ scheduler-execute-{app}
  → business @MqConsumer
  → PeachJobExecutor.execute()
  → ExecutionLeaseClient.claim()
  → ThreadPoolManager / PoolType.SCHEDULED
  → @PeachJob handler
  → scheduler-execution-result
```

Architecture sources: `docs/architecture/`.

## SQL

Scripts live under repository `sql/`. See `sql/README.md`.

| Script | Purpose |
| --- | --- |
| `MQ_OUTBOX_EVENT.sql` | Reliable send Outbox |
| `MQ_CONSUME_RECORD.sql` | Consume idempotency |

Control-plane tables: `PEACH_SCHEDULER_*.sql`, `QRTZ_MYSQL.sql`.

## Boundaries

- Handlers must not depend on Quartz APIs or execute dynamic code from the admin UI.
- Use `PoolType.SCHEDULED` only; do not create ad-hoc thread pools.
- Do not log full payloads or credentials.
- Timeout means control-plane wait timeout, not guaranteed rollback of external side effects.

## Verification

```bash
mvn -f peach-component/peach-scheduler/pom.xml -Dmaven.test.skip=true package -Pdevelopment
node scripts/check-utf8.mjs
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| `PeachJobExecutor` missing | `ExecutionLeaseClient`, `ThreadPoolManager`, Rocket beans | Add required starters |
| Claim always rejected | execution state, Same-Token, application name | Verify Feign target `peach-scheduler` |
| Handler not whitelisted | registration heartbeat, application name | Check `openfeign-external` |
| Duplicate side effects | claim alone is insufficient | Add handler idempotency by `executionId` |
| Durability startup failure | `require-jdbc=true` without JDBC tables | Run `MQ_*.sql` scripts |
