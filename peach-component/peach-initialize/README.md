# Peach Initialize 使用手册

## 项目概述

`peach-initialize` 提供一个标准化的应用初始化机制与可组合执行组件容器，面向 Spring Boot 应用的多阶段生命周期。你可以在以下时机注入并按顺序执行自定义初始化逻辑：

- `InitializingBean` 阶段（属性填充完成后）
- `@PostConstruct` 阶段（Bean 构造完成后）
- `ApplicationRunner` 命令行阶段（应用启动后）
- `ApplicationStartedEvent` 事件阶段（应用已启动）

核心接口是 `InitializeHandler`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/base/InitializeHandler.java:1`），执行器基类为 `AbstractAppExecute`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/base/AbstractAppExecute.java:20`），其按 `type()` 过滤并按 `executeOrder()` 排序执行（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/base/AbstractAppExecute.java:26`）。

此外，项目内置可配置的“组合组件”容器 `CompositeContainer`，用于按层级与顺序构建执行树并批量执行（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/impl/composite/CompositeContainer.java:35`）。

## 模块架构

- `peach-initialize`：聚合父工程，声明模块（`peach-initialize/pom.xml:18`）。
- `peach-initialize-autoconfigure`：自动配置模块，注册自动配置类（`peach-initialize-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1`）。
- `peach-initialize-starter`：Starter 模块，供业务应用直接依赖，聚合所需依赖。

自动配置类：

- `InitializeAutoConfig` 提供四类生命周期执行器 Bean（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/config/InitializeAutoConfig.java:15`）。
- `CompositeAutoConfig` 提供组合容器与其初始化处理器（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/config/CompositeAutoConfig.java:13`）。

生命周期执行器：

- `AppInitializingBeanExecute`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/AppInitializingBeanExecute.java:1`）
- `AppPostConstructExecute`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/AppPostConstructExecute.java:1`）
- `AppCommandLineExecute`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/AppCommandLineExecute.java:1`）
- `AppStartEventExecute`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/execute/AppStartEventExecute.java:1`）

## 依赖与 `<optional>true</optional>` 说明

- 在自动配置模块 `peach-initialize-autoconfigure/pom.xml` 中，`spring-boot-autoconfigure`、`spring-boot-configuration-processor`、`lombok` 等依赖被标记为可选（如 `peach-initialize-autoconfigure/pom.xml:18`、`:23`、`:28`），目的是避免这些编译期/配置期依赖被作为传递依赖强行带入业务应用。
- 在 Starter 模块 `peach-initialize-starter/pom.xml` 中，仅依赖 `peach-initialize-autoconfigure`（`peach-initialize-starter/pom.xml:18`），不携带 `lombok` 与 `spring-boot-configuration-processor` 等编译期依赖，`spring-boot-autoconfigure` 由业务工程的 Spring Boot 提供。
- 规则：
  - 放在 `autoconfigure` 中且仅用于自动配置或编译辅助的依赖 → 标注为 `optional`，避免不必要的传递。
  - 业务方运行必需的第三方库（确需由 Starter 带入）→ 放在 `starter` 中聚合；若业务工程本就包含（如 Spring Boot 本身），则不重复携带。

## 快速使用

1. 在业务项目 `pom.xml` 引入：

```xml
<dependency>
  <groupId>com.peach</groupId>
  <artifactId>peach-initialize-starter</artifactId>
</dependency>
```

2. 编写初始化处理器：选择一个抽象基类实现，并声明执行顺序：

```java
public class FooInit extends com.peach.initialize.base.AbstructAppInitializingBeanHandler {
  public Integer executeOrder() { return 1; }
  public void executeInitialize(org.springframework.context.ConfigurableApplicationContext ctx) { /* ... */ }
}
```

可选基类：`AbstructAppInitializingBeanHandler`（初始化阶段）、`AbstractAppPostConstructHandler`（构造后）、`AbstractAppCommandLineHandler`（命令行）、`AbstractAppStartedEventHandler`（启动事件）。类型常量见 `InitializeHandlerType`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/constant/InitializeHandlerType.java:8`）。

3. 使用组合组件（可选）：实现 `AbstractComposite<T>`，设置类型、层级、顺序与父顺序，容器会在启动事件时构建并执行（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/impl/composite/AbstractComposite.java:51`）。

## 扩展指南

- 新增生命周期：添加新的 `InitializeHandler.type()` 常量，提供对应执行器实现，并在自动配置中注册。
- 条件化装配：对业务可选的 Bean 使用 `@ConditionalOnMissingBean`/`@ConditionalOnClass` 等进行保护，避免强绑定。
- 组装顺序：通过 `executeOrder()` 控制同类型处理器的执行先后；组合组件通过 `executeTier()` 与 `executeParentOrder()` 控制层级与父子关系。

## Starter 制作规则

- 模块拆分：`autoconfigure` 专注自动配置与 SPI 注册，`starter` 负责聚合依赖与对外发版。
- 依赖策略：`autoconfigure` 中对编译期/配置期/可选依赖一律 `<optional>true</optional>`；`starter` 引入运行必需依赖且不使用 `optional`。
- 自动配置注册：使用 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册（`peach-initialize-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1`）。
- 抽象与扩展点：以接口/抽象基类暴露扩展点，禁止在自动配置中硬编码业务逻辑。
- 命名与可读性：类名、常量名与模块名清晰表达职责，示例与文档完善。
- 兼容与安全：避免在自动配置中产生重副作用；对外部依赖进行条件化与版本约束；不暴露敏感信息。

## 参考

- Spring Boot AutoConfiguration：`@AutoConfiguration` 与 Imports 文件机制。
- Spring Boot Configuration Processor 与元数据生成。

