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
