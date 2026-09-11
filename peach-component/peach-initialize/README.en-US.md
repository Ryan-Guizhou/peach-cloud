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
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-initialize-starter</artifactId>
</dependency>
```

The public entry is a business `InitializeHandler` (or a lifecycle abstract class). Four auto-configured executors group handlers by `type()` and invoke `executeInitialize(ConfigurableApplicationContext)` in ascending `executeOrder`:

- `APP_INITIALIZING_BEAN`: `InitializingBean#afterPropertiesSet`
- `APP_POSTCONSTRUCT`: `@PostConstruct`
- `APP_EVENT_LISTENER`: `ApplicationStartedEvent`
- `APP_COMMAND_LINE_RUNNER`: `ApplicationRunner`

## Quick Start

The example lives in [`peach-initialize-quickstart`](./peach-initialize-quickstart/). It is non-web and only verifies minimal starter integration. Production modules must not depend on it.

| Item | Detail |
| --- | --- |
| Capability samples (register `InitializeHandler`) | **Custom handler**: extend `AbstractAppStartedEventHandler` for cache warm-up; **Success marker**: after startup `warmedUp=true` / `payload=cache-ready`; **Order**: same-type handlers run by ascending `executeOrder` (warm-up `10` → resource check `20`) |
| Runner | `InitializeDemoRunner`; disable with `quickstart.initialize.demo.enabled=false` |
| Prerequisites | No external dependencies |
| Port | No REST: `spring.main.web-application-type=none` |

```bash
mvn -f peach-component/peach-initialize/peach-initialize-quickstart/pom.xml spring-boot:run
mvn -f peach-component/peach-initialize/peach-initialize-quickstart/pom.xml test
```

## Boundaries

- Initialization work must be bounded and must not wait forever or perform uncontrolled large jobs.
- Initialization is not a database migration mechanism.
- Multi-instance deployments must define duplicate execution, concurrency and idempotency semantics.
- Whether initialization failure blocks startup or degrades operation must follow the current handler contract and configuration.
