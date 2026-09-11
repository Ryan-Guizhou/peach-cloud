# peach-initialize

English | [中文](README.md)

`peach-initialize` organizes application-startup initialization handlers such as cache warm-up, required-resource checks and in-memory mapping initialization.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-initialize-autoconfigure` | `InitializeHandler` contract, orchestration and auto-configuration |
| `peach-initialize-starter` | Business integration dependency entry point |
| `peach-initialize-quickstart` | Minimal handler-integration verification |

Business dependency:

```xml
<dependency><groupId>com.peach</groupId><artifactId>peach-initialize-starter</artifactId></dependency>
```

Quickstart: [`peach-initialize-quickstart`](peach-initialize-quickstart/README.en-US.md).

## Boundaries

- Initialization work must be bounded and must not wait forever or perform uncontrolled large jobs.
- Initialization is not a database migration mechanism.
- Multi-instance deployments must define duplicate execution, concurrency and idempotency semantics.
- Whether initialization failure blocks startup or degrades operation must follow the current handler contract and configuration.
