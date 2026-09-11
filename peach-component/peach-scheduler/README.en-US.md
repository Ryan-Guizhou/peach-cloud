# peach-scheduler

English | [中文](README.md)

- Last updated: 2026-09-11
- artifactId: `peach-scheduler`
- Type: distributed scheduler executor SDK (Provider SPI + RocketMQ transport)
- Target stack: Java 21, Spring Boot 3.5.4

## Purpose

`peach-component/peach-scheduler` is the **executor-side** component of Peach Cloud scheduling. Business services use it to register `@PeachJob` handlers, consume execution commands from the control plane, claim leases, run business logic, and report results.

**Provided:**

- Business task SDK (`@PeachJob`, `JobHandler`, `PeachJobExecutor`)
- Scheduling Provider SPI and the default Quartz implementation
- RocketMQ command/result transport with JDBC Outbox and consume-idempotency adapters
- Handler execution isolation through the `peach-virtual-thread` scheduler group
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
| `peach-scheduler-quickstart` | Locally provable Handler / Claim / executor orchestration samples |

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
    <artifactId>peach-virtual-thread-starter</artifactId>
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
  virtual-thread:
    enabled: true
    groups:
      scheduler:
        max-concurrency: 16
        max-pending: 200
        backpressure: REJECT
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

See `DemoSchedulerExecutionConsumer` in `peach-scheduler-quickstart`.

### Quickstart local demo

[`peach-scheduler-quickstart`](./peach-scheduler-quickstart/) (`web-application-type: none`) covers only executor-side capabilities that can be proven locally. It does not depend on the production control plane or a real RocketMQ cluster:

- **JobHandler / `@PeachJob`**: `DemoCleanupJob` executes directly and is registered
- **Claim gate**: in-memory `ExecutionLeaseClient` allows then rejects
- **PeachJobExecutor orchestration**: `command -> claim -> handler -> reporter`; a rejected claim skips the handler and does not report
- In-memory stubs: `DemoExecutionLeaseClient` / `DemoExecutionResultReporter` (production replaces them with Feign + RocketMQ Outbox)
- `DemoSchedulerExecutionConsumer` is a production wiring sample only; default `peach.rocket.enabled=false` leaves it unregistered
- `SchedulerDemoRunner` is off by default; enable with `quickstart.scheduler.demo.enabled=true`
- Tests: no-container `SchedulerSliceTest` plus a small integration `SchedulerCapabilityTest`

```bash
mvn -f peach-component/peach-scheduler/peach-scheduler-quickstart/pom.xml test
```

## Configuration Keys

| Key | Default | Description |
| --- | --- | --- |
| `peach.scheduler.enabled` | `true` | Enable executor auto-configuration |
| `peach.scheduler.executor.application-name` | none | Must match control-plane job `applicationName` |
| `peach.scheduler.executor.default-timeout-ms` | `1800000` | Default handler wait timeout |
| `peach.scheduler.rocket.require-jdbc` | `false` | Fail fast if durable JDBC stores are missing |
| `peach.scheduler.quartz.group` | `PEACH_SCHEDULER` | Quartz job/trigger group |
| `peach.virtual-thread.groups.scheduler.max-concurrency` | `256` | Maximum concurrent scheduler handler executions |
| `peach.virtual-thread.groups.scheduler.max-pending` | `512` | Maximum pending scheduler handler executions |

## Runtime Flow

```text
Control-plane Outbox → RocketMQ scheduler-execute-{app}
  → business @MqConsumer
  → PeachJobExecutor.execute()
  → ExecutionLeaseClient.claim()
  → VirtualExecutorRegistry / scheduler group
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
- Use `peach.virtual-thread.groups.scheduler` for blocking IO handler execution; do not create ad-hoc thread pools.
- Quartz triggers and listener lifecycle threads remain platform-thread concerns unless explicitly migrated and verified.
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
| `PeachJobExecutor` missing | `ExecutionLeaseClient`, `ExecutionResultReporter`, `VirtualExecutorRegistry`, or Scheduler auto-config evaluating before the VT registry | Add required starters and configure the `scheduler` virtual-thread group; the starter now runs after `PeachVirtualThreadAutoConfiguration` |
| Claim always rejected | execution state, Same-Token, application name | Verify Feign target `peach-scheduler` |
| Handler not whitelisted | registration heartbeat, application name | Check `openfeign-external` |
| Duplicate side effects | claim alone is insufficient | Add handler idempotency by `executionId` |
| Durability startup failure | `require-jdbc=true` without JDBC tables | Run `MQ_*.sql` scripts |

## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
