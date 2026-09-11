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

## QuickStart

The example lives in [`peach-initialize-quickstart`](./peach-initialize-quickstart/) and verifies minimal starter integration. Real initialization handlers should be registered as business beans with bounded timeouts, idempotency, and explicit startup-failure semantics.

```bash
mvn -f peach-component/peach-initialize/peach-initialize-quickstart/pom.xml spring-boot:run -Pdevelopment
```

## Boundaries

- Initialization work must be bounded and must not wait forever or perform uncontrolled large jobs.
- Initialization is not a database migration mechanism.
- Multi-instance deployments must define duplicate execution, concurrency and idempotency semantics.
- Whether initialization failure blocks startup or degrades operation must follow the current handler contract and configuration.
