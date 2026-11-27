# peach-setting

English | [中文](README.md)

## Purpose

`peach-setting` is the system-setting domain. It manages dictionaries, value sets, notices, localized messages, and exposes OpenFeign APIs for other services.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-setting-service` | Dictionary, value-set, notice, and localized-message services |
| `peach-setting-rest` | Internal REST management APIs |
| `peach-setting-launch` | Runtime application module |
| `peach-setting-entity` | DO/DTO/QO/VO models |
| `peach-setting-common` | Shared setting-domain objects |
| `peach-setting-openfeign-external` | External OpenFeign client |

## Key Entrypoints

- Application: `peach-setting-launch/src/main/java/com/peach/setting/launch/PeachSettingApplication.java`
- REST controllers: `peach-setting-rest/src/main/java/com/peach/setting/rest/internal`
- Service interfaces: `peach-setting-service/src/main/java/com/peach/setting/service`
- Feign client: `peach-setting-openfeign-external/src/main/java/com/peach/setting/openfeign`

## Boundaries

- This module manages configuration data; it does not define all real-time push protocols for business configuration.
- Cache refresh strategies for dictionaries and value sets must be designed by each consumer scenario.

## Troubleshooting

| Symptom | Checks | Resolution |
| --- | --- | --- |
| Dictionary query returns no data | Verify initialization scripts, tenant scope, and query conditions | Check database data and request parameters |

## Verification

```bash
mvn -f "peach-setting/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.


## Scheduler integration demo

Launch the existing application entry point in IDEA with the normal `dev` (or `docker`) profile. Local application YAML retains startup and Nacos imports; Nacos manages scheduler settings, the managed `scheduler` virtual-thread group, RocketMQ, Outbox and task defaults. The service registers `settingSchedulerDemo`, sends heartbeats and consumes execution commands through the managed executor and Claim gate. The demo returns success without changing business data. Set `PEACH_SCHEDULER_DEMO_ENABLED=false` to disable the demo Handler and consumer.

Task registration follows service startup automatically; inspect its definition and initial state in task management. Services and the control plane require the same reachable Nacos, Redis, MySQL Outbox and RocketMQ environment. Verify execution against live services.


## Automatic task registration

Services declare stable `jobCode` and `jobName` in `@PeachJob` and supply complete defaults through Nacos `peach.scheduler.executor.jobs[handlerName]`: schedule, time zone, misfire and concurrency policies, timeout, retries, parameters and initial state. The control plane validates the definition on first registration and creates the task. Missing metadata permits Handler registration and heartbeats only, without creating a placeholder draft. Later heartbeats preserve schedules, parameters and lifecycle or deleted states maintained by administrators. Additional tasks may use the same Handler. Initial state is managed centrally.
