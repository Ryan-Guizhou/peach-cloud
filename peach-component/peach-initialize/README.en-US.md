# peach-initialize

English | [中文](README.md)

## Purpose

`peach-initialize` is an application startup initialization component. It organizes initialization handlers during Spring Boot startup and supports normal and composite initialization scenarios.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-initialize-autoconfigure` | Initialization handlers, composite handlers, and auto-configuration |
| `peach-initialize-starter` | Starter exposed to business modules |

## Core Objects

- `InitializeHandler`: initialization handler interface.
- `InitializeHandlerType`: handler type constants.
- `InitializeAutoConfig`: registers default initialization beans.
- `CompositeAutoConfig`: registers composite initialization support.

## Usage Notes

- Initialization logic must be idempotent to avoid dirty data after restart.
- Long-running initialization tasks need explicit timeout, failure handling, and logging.
- If a task depends on database, Redis, or external services, ensure dependencies are ready first.

## Verification

```bash
mvn -f "peach-component/peach-initialize/pom.xml" -DskipTests package
```
