# peach-component

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-component`  
类型：通用组件聚合模块

## 模块定位

`peach-component` 聚合与业务域无关、可被多个服务复用的组件 starter。组件通过 `autoconfigure` 提供核心 API、配置绑定、默认实现和扩展点，通过 `starter` 对业务模块暴露依赖入口。

本模块解决：

- 验证码、邮件、初始化、存储、线程池等通用能力复用。
- starter 和 autoconfigure 的统一组织。
- 示例模块与业务接入说明的归口。

本模块不解决：

- 具体业务域接口。
- 中间件协议封装，例如 Redis、RocketMQ、Sa-Token，这些位于 `peach-middleware`。
- 生产外部服务部署，例如 SMTP、对象存储、文件服务器等。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-captcha` | 验证码生成、缓存、校验和频控扩展 |
| `peach-email` | 邮件发送、模板、路由、重试和幂等 |
| `peach-storage` | 统一存储模板、provider SPI、对象存储和本地存储接入 |
| `peach-initialize` | 应用初始化处理器和编排 |
| `peach-threadpool` | 配置化线程池、异步注解和上下文传递 |

## 通用接入方式

业务模块一般只引入对应 `*-starter`：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-threadpool-starter</artifactId>
</dependency>
```

不要在业务模块中直接依赖 `*-autoconfigure`，除非正在扩展组件内部能力。

## 组件分层约定

| 层级 | 说明 |
| --- | --- |
| `*-autoconfigure` | 核心 API、配置类、自动配置、默认实现、SPI |
| `*-starter` | 对外依赖聚合，业务接入优先使用 |
| `*-example` | 可运行示例、覆盖默认 Bean 示例、配置样例 |

## 运行机制

1. 业务模块引入组件 starter。
2. Spring Boot 读取 starter 中的自动配置声明。
3. autoconfigure 根据配置项和 Bean 条件注册默认实现。
4. 业务可以通过自定义 `@Bean` 或 SPI 覆盖默认行为。
5. 组件运行依赖的外部服务由业务环境提供。

## 边界与限制

- 组件默认实现通常偏向开发和基础场景，生产环境需要确认外部依赖、幂等、超时、重试、资源释放等策略。
- starter 不应隐藏高风险行为，例如批量删除、自动创建资源、无限队列、明文密钥日志。
- 每个组件 README 应写明真实配置项、默认值、扩展方式和排障表。

## 构建与验证

```bash
mvn -f "peach-component/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component -am clean package -DskipTests -Pdevelopment
```

单组件验证示例：

```bash
mvn -pl peach-component/peach-storage -am clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-threadpool -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| starter Bean 未注入 | 是否引入 `*-starter`；自动配置条件是否满足 | 检查依赖树和 Spring Boot 条件报告 |
| 配置未生效 | 配置前缀、profile、Nacos 配置是否正确 | 对照组件 README 和配置类字段 |
| 默认实现不满足生产 | 是否提供自定义 `@Bean` 或 SPI | 用业务实现覆盖默认 Bean |
| 构建失败 | 是否从根目录构建；是否需要 `-am` | 使用聚合模块命令构建 |
