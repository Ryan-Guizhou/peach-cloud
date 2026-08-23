# peach-monitor

English | [中文](README.md)

## Purpose

`peach-monitor` is the monitoring and runtime-information domain. It provides runtime status queries, monitor REST APIs, and monitoring models. The current implementation centers on `MonitorRuntimeServiceImpl` and `MonitorController`.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-monitor-service` | Runtime monitoring service |
| `peach-monitor-rest` | Monitor REST APIs |
| `peach-monitor-launch` | Runtime application module |
| `peach-monitor-entity` | Monitoring models |
| `peach-monitor-common` | Shared monitoring objects |
| `peach-monitor-openfeign-external` | Monitor OpenFeign client |

## Key Entrypoints

- Application: `peach-monitor-launch/src/main/java/com/peach/monitor/launch/PeachMonitorApplication.java`
- REST controller: `peach-monitor-rest/src/main/java/com/peach/monitor/rest/MonitorController.java`
- Service interface: `peach-monitor-service/src/main/java/com/peach/monitor/service/IMonitorRuntimeService.java`

## Boundaries

- This module is not a full APM, tracing, or alerting platform.
- Production observability still requires logs, metrics, tracing, and alert rules.

## Verification

```bash
mvn -f "peach-monitor/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
