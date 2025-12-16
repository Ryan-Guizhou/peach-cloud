# peach-initialize-autoconfigure 指南

## 模块职责

提供自动配置与生命周期执行器的注册：

- 自动配置类 `InitializeAutoConfig` 与 `CompositeAutoConfig`（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/config/InitializeAutoConfig.java:1`、`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/config/CompositeAutoConfig.java:1`）
- 通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 注册（`peach-initialize-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1`）

## 依赖策略

在 `pom.xml` 中将编译/配置期依赖标记为可选，以避免传递污染：

- `spring-boot-autoconfigure`（`peach-initialize-autoconfigure/pom.xml:18`）
- `spring-boot-configuration-processor`（`peach-initialize-autoconfigure/pom.xml:23`）
- `lombok`（`peach-initialize-autoconfigure/pom.xml:28`）

## 扩展点

- 实现 `InitializeHandler` 的各抽象基类即可在对应生命周期执行：
  - `AbstructAppInitializingBeanHandler`
  - `AbstractAppPostConstructHandler`
  - `AbstractAppCommandLineHandler`
  - `AbstractAppStartedEventHandler`
- 组合组件：实现 `AbstractComposite<T>` 并设置类型/层级/顺序，容器会在启动事件中完成树构建与执行（`peach-initialize-autoconfigure/src/main/java/com/peach/initialize/impl/composite/CompositeContainer.java:35`）。

## 验证

引入 `peach-initialize-starter` 后，应用启动时可在日志或断点处观察各执行器按顺序触发，或在业务处理器中编排组合组件以验证执行顺序。

