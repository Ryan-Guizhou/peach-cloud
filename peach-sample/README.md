# peach-sample

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-sample`  
类型：示例应用模块

## 模块定位

`peach-sample` 是组件和中间件能力的本地示例应用，用于验证 starter 接入、配置样例和基础调用方式。它不是业务生产服务。

本模块解决：

- 展示组件和中间件 starter 如何被普通 Spring Boot Web 应用引入。
- 为开发期提供快速验证入口。
- 作为新增 starter 的接入样例承载位置。

本模块不解决：

- 生产业务流程。
- 完整接口权限、安全审计和稳定性治理。
- 所有组件能力的穷尽示例。

## 主要依赖

`peach-sample` 当前引入了多个 starter 和基础依赖，包括：

- `peach-redis-multicache-starter`
- `peach-redis-stream-starter`
- `peach-redis-tool-starter`
- `peach-redission-bloomfilter-starter`
- `peach-redission-delayqueue-starter`
- `peach-redission-distributedlock-starter`
- `peach-redission-repeat-starter`
- `peach-email-starter`
- `peach-initialize-starter`
- `peach-captcha-starter`

## 启动入口

| 类型 | 路径 |
| --- | --- |
| 启动类 | `peach-sample/src/main/java/com/peach/sample/SampleApplication.java` |
| Maven POM | `peach-sample/pom.xml` |

## 使用方式

构建：

```bash
mvn -f "peach-sample/pom.xml" clean package -DskipTests -Pdevelopment
```

从根目录构建并带上依赖：

```bash
mvn -pl peach-sample -am clean package -DskipTests -Pdevelopment
```

本地运行：

```bash
mvn -pl peach-sample -am spring-boot:run
```

## 边界与限制

- 示例配置可以为了演示简化，不能直接当作生产配置。
- 示例中间件依赖需要本地 Redis、Redisson 相关配置或其他外部服务可用。
- 示例代码允许覆盖默认 Bean 展示扩展方式，但生产实现应放在业务模块中。
- 新增示例时应写清楚依赖的 starter、配置前缀和验证路径。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 示例启动失败 | Redis、数据库或其他中间件是否可用 | 启动本地依赖或关闭不需要的示例配置 |
| starter Bean 缺失 | 依赖是否引入；自动配置条件是否满足 | 检查 `pom.xml` 和条件报告 |
| 示例行为与生产不一致 | 是否使用了内存实现或简化配置 | 回到对应 starter README 查看生产边界 |
| 根目录构建失败 | 是否需要 `-am` 构建依赖模块 | 使用 `mvn -pl peach-sample -am ...` |
