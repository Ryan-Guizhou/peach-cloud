# peach-threadpool

English | [中文](README.md)

## Purpose

`peach-threadpool` provides configurable thread pools and the `@AsyncExecuted` annotation, centralizing async task execution and avoiding ad-hoc thread pool creation in business code.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-threadpool-autoconfigure` | Auto-configuration, thread pool manager, and annotation aspect |
| `peach-threadpool-starter` | Starter exposed to business modules |

## Core Objects

- `ThreadPoolProperties`: binds `peach.threadpool`.
- `ThreadPoolManager`: gets, submits, and executes tasks by `PoolType`.
- `@AsyncExecuted`: method-level async execution annotation.
- `TaskWrapper`: propagates SecurityContext.
- `PoolProperties`: thread pool parameters.

## Configuration Example

```yaml
peach:
  threadpool:
    global:
      enable-security-context: true
    pools:
      - type: IO
        core-size: 16
        max-size: 64
        queue-capacity: 1000
        thread-name-prefix: io-task-
        rejected-policy: CALLER_RUNS
```

## Boundaries

- The current aspect matches method annotations only; class-level annotations do not automatically apply to all methods.
- Normal return values wait on `Future.get()`, so this is not fire-and-forget behavior.
- `CompletableFuture` paths must be checked against the expected executor usage.
- `timeoutMs` limits wait time but does not guarantee reliable cancellation of the underlying task.

## Verification

```bash
mvn -f "peach-component/peach-threadpool/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
