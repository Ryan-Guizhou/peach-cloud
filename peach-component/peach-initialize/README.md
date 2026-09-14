# peach-initialize

[English](README.en-US.md) | 中文

`peach-initialize` 用于组织应用启动期初始化处理器，例如缓存预热、必要资源检查和内存映射初始化。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-initialize-autoconfigure` | `InitializeHandler` 契约、编排和自动配置 |
| `peach-initialize-starter` | 业务接入依赖入口 |
| `peach-initialize-quickstart` | 最小处理器接入验证 |

业务模块依赖：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-initialize-starter</artifactId>
</dependency>
```

公开入口是业务侧实现的 `InitializeHandler`（或继承对应生命周期抽象类）。自动装配的四个执行器按 `type()` 分组，再按 `executeOrder` 升序调用 `executeInitialize(ConfigurableApplicationContext)`：

- `APP_INITIALIZING_BEAN`：`InitializingBean#afterPropertiesSet`
- `APP_POSTCONSTRUCT`：`@PostConstruct`
- `APP_EVENT_LISTENER`：`ApplicationStartedEvent`
- `APP_COMMAND_LINE_RUNNER`：`ApplicationRunner`

## Quick Start

示例位于 [`peach-initialize-quickstart`](./peach-initialize-quickstart/)，无 Web 端口，只验证 starter 最小接入，不作为生产依赖。

| 项 | 说明 |
| --- | --- |
| 能力样例（注册 `InitializeHandler`） | **自定义 Handler**：继承 `AbstractAppStartedEventHandler` 做缓存预热；**成功标记**：启动后 `warmedUp=true` / `payload=cache-ready`；**执行顺序**：同类型 Handler 按 `executeOrder` 升序（预热 `10` → 资源检查 `20`） |
| Runner | `InitializeDemoRunner`；关闭演示：`quickstart.initialize.demo.enabled=false` |
| 前置 | 无外部依赖 |
| 端口 | 无 REST：`spring.main.web-application-type=none` |

```bash
mvn -pl peach-component/peach-initialize/peach-initialize-quickstart -am spring-boot:run
mvn -pl peach-component/peach-initialize/peach-initialize-quickstart -am test
```

## 边界

- 初始化任务必须有界，不能无限等待或承载不可控大任务。
- 初始化不替代数据库迁移工具。
- 多实例下需要明确重复执行、并发和幂等语义。
- 初始化失败是阻断启动还是降级运行必须由当前处理器契约和配置明确。
