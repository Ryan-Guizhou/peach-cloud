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


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
