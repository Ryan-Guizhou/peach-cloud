# peach-scheduled

English | [中文](README.md)

- Last updated: 2026-08-14
- artifactId: `peach-scheduled`
- Type: scheduler control-plane service (standalone Spring Boot app)
- Target stack: Java 8, Spring Boot 2.7.13
- Nacos service name: `peach-scheduler`

## Purpose

`peach-scheduled` is the **control plane** for Peach Cloud scheduling. It manages job definitions, execution records, state machines, Quartz cluster triggers, RocketMQ Outbox dispatch, lease claims, result ingestion, manual audit, and recovery loops.

The executor SDK lives in `peach-component/peach-scheduler`. Business handlers run in business microservices, not in this service.

**Provided:**

- Job CRUD and lifecycle operations
- Execution occurrences, claim, retry, and concurrency control
- Handler whitelist registration and heartbeat
- Quartz JDBC cluster triggering
- Admin APIs and internal claim/register APIs

**Not provided:**

- Long-running business handler execution
- Exactly-once semantics
- Reliable rollback for RUNNING executions
- Dynamic code execution from the admin UI
- A separate RBAC system (uses Peach Auth `scheduler:*` permissions)

Reliability model:

```text
At-Least-Once Delivery + JDBC Atomic Claim + Idempotent Business Handler
```

## Submodules

| Module | Responsibility |
| --- | --- |
| `peach-scheduled-common` | States, events, constants, permission codes |
| `peach-scheduled-entity` | DO, DTO, QO, VO |
| `peach-scheduled-service` | Core services, reconciler, trigger, state machines, DAO |
| `peach-scheduled-rest` | `/scheduler/**` admin APIs, `/internal/scheduler/**` internal APIs |
| `peach-scheduled-openfeign-external` | Feign clients for business-side claim and handler registration |
| `peach-scheduled-launch` | Boot entry and runtime configuration |

## Core Services

| Service | Description |
| --- | --- |
| `SchedulerReconciler` | Syncs `PEACH_SCHEDULER_JOB` to Quartz (`QRTZ_*`) |
| `SchedulerTriggerService` | Creates executions and dispatches commands |
| `SchedulerExecutionLifecycleService` | Execution transitions, claim, complete |
| `SchedulerRetryRecoveryService` | Lease recovery, due retries, deferred dispatch |
| `RocketJobDispatcher` | Default `JobDispatcher` via `MqOutboxPublisher` |
| `SchedulerExecutionResultConsumer` | Consumes `scheduler-execution-result` |

## Architecture

Source: `docs/architecture/peach-scheduler-platform.drawio`

```text
Admin UI → peach-scheduled-rest
  → PEACH_SCHEDULER_JOB (JDBC source of truth)
  → SchedulerReconciler → Quartz
  → PEACH_SCHEDULER_EXECUTION
  → RocketMQ scheduler-execute-{app}
  → business peach-scheduler-starter
  → claim + @PeachJob + scheduler-execution-result
```

## State Machines

**Job:** `DRAFT → ENABLED ⇄ PAUSED → DISABLED → DELETED`

**Execution:** `CREATED → QUEUED → RUNNING → SUCCEEDED / RETRY_WAIT / TIMED_OUT / DEAD / CANCELLED / SKIPPED`

State is persisted in JDBC with optimistic locking (`state + version`). Spring StateMachine is rebuilt from JDBC on each transition.

Manual retry is allowed only in `RETRY_WAIT`. Manual cancel is allowed only in `CREATED/QUEUED/RETRY_WAIT`.

## Concurrency Policies

| Policy | Behavior |
| --- | --- |
| `ALLOW` | Dispatch immediately |
| `SKIP_IF_RUNNING` | Create occurrence, then mark `SKIPPED` |
| `DISALLOW` | Keep `CREATED`, dispatch sequentially via recovery loop |

## REST APIs

### Admin APIs (authenticated, `scheduler:*` permissions)

- `/scheduler/jobs` — list, detail, create, update, enable/disable/pause/resume/run, delete, cron preview
- `/scheduler/executions` — list, detail, manual retry/cancel
- `/scheduler/handlers` — registered handler list

Permission codes: `SchedulerPermissions`.

### Internal APIs (Same-Token, not public)

- `POST /internal/scheduler/executions/{id}/claim`
- `POST /internal/scheduler/handlers/register`

## Configuration

```yaml
spring:
  application:
    name: peach-scheduler
  quartz:
    job-store-type: jdbc
    jdbc:
      initialize-schema: never

peach:
  scheduler:
    provider: quartz
    rocket:
      require-jdbc: true
```

| Key | Default | Description |
| --- | --- | --- |
| `peach.scheduler.provider` | `quartz` | Active SchedulingProvider id |
| `peach.scheduler.service.reconcile-delay-ms` | `5000` | Job-to-Quartz sync interval |
| `peach.scheduler.service.recovery-delay-ms` | `5000` | Recovery loop interval |
| `peach.scheduler.service.handler-offline-scan-ms` | `60000` | Handler offline scan interval |

RocketMQ topics:

- `scheduler-execute-{applicationName}` — dispatch commands to business apps
- `scheduler-execution-result` — ingest execution results

## SQL

Scripts are under repository `sql/`. See `sql/README.md`.

Core tables: `PEACH_SCHEDULER_*`, `QRTZ_*`, `MQ_OUTBOX_EVENT`, `MQ_CONSUME_RECORD`.

MyBatis XML:

```text
peach-scheduled-service/src/main/resources/com/peach/scheduler/dao/
```

## Business Integration Checklist

1. Add `peach-scheduler-starter` and `peach-scheduled-openfeign-external` to business services.
2. Implement `@PeachJob` handlers and a fixed-topic `@MqConsumer`.
3. Create jobs in the control plane with matching `applicationName` and whitelisted `handlerName`.
4. Ensure Feign reaches service `peach-scheduler` with Same-Token propagation.

## Security Boundaries

- No dynamic executable expressions from the admin UI.
- No credential fields in job parameter JSON.
- Logs must not contain full DTOs or secrets.
- Internal APIs require Same-Token, not user login.
- All writes use optimistic locking.

## Production Checklist

1. Run scheduler SQL scripts from `sql/` (or `ALL_TABLE_CREATE.sql`).
2. Use MySQL Quartz JDBC JobStore; keep `initialize-schema=never`.
3. Pre-create RocketMQ topics.
4. Keep `/internal/scheduler/**` internal-only.
5. Configure retention for execution/audit/outbox/consume tables.

## Verification

```bash
mvn -f peach-scheduled/pom.xml -Dmaven.test.skip=true package -Pdevelopment
node scripts/check-utf8.mjs
git diff --check -- peach-scheduled sql
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| Job never fires | job state, `SYNC_STATUS` | inspect reconciler logs and `LAST_SYNC_ERROR` |
| Quartz fires but no execution | trigger service, job state | ensure job is `ENABLED` |
| Business receives no MQ | outbox status, topic name | inspect `MQ_OUTBOX_EVENT` |
| Claim rejected | execution state/version | verify `QUEUED` and Same-Token |
| Handler missing in UI | handler registry heartbeat | verify business Feign registration |
| Durability startup failure | JDBC tables/beans | run SQL scripts, disable memory stores |
