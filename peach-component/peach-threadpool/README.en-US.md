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
- `TaskWrapper`: propagates MDC and SecurityContext.
- `PoolProperties`: thread pool parameters.

## Configuration Example

```yaml
peach:
  threadpool:
    global:
      enable-mdc: true
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
