# peach-initialize-starter 指南

## 模块职责

面向业务应用的单点引入坐标，聚合自动配置模块：

- 仅依赖 `peach-initialize-autoconfigure`（`peach-initialize-starter/pom.xml:18`）
- 不携带 `lombok` 与 `spring-boot-configuration-processor` 等编译期依赖
- 不强行携带 `spring-boot-autoconfigure`，由业务工程的 Spring Boot 提供

## 引入方式

在业务工程 `pom.xml` 添加：

```xml
<dependency>
  <groupId>com.peach</groupId>
  <artifactId>peach-initialize-starter</artifactId>
</dependency>
```

无需额外配置，自动配置类通过 Imports 文件完成注册（`peach-initialize-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:1`）。

## 与 autoconfigure 的关系

- `starter` 不导出编译期依赖，避免传递污染；运行依赖由业务工程提供。
- `autoconfigure` 对编译/配置期依赖标注 `optional`，避免不必要的传递。

## 注意事项

- 如需替换或禁用默认行为，使用 `@ConditionalOnMissingBean` 等条件化方式在业务侧自定义处理器或组件。

