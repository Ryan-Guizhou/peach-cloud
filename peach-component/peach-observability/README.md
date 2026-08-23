# Peach Observability

`peach-observability` 是 peach-cloud 的统一可观测性组件，面向 Java 21、Spring Boot 3.5.4，提供 Metrics、Trace 和请求关联标识的标准接入。

## 模块定位

该组件解决以下问题：

- 统一引入 Spring Boot Actuator、Prometheus Registry、Micrometer Tracing 和 OpenTelemetry OTLP Exporter。
- 为 Servlet 服务统一解析或生成 `X-Request-Id`，并写入响应头、请求属性和 MDC。
- 保持 `requestId`、`traceId`、`spanId` 三类标识职责清晰。
- 通过条件化自动配置支持关闭能力或覆盖默认 Bean。

该组件不负责部署 Prometheus、Grafana、Loki、Tempo 或 Collector，也不默认公开敏感 Actuator 端点。部署与网络访问控制由独立部署配置完成。

## 目录结构

```text
peach-observability/
├── pom.xml
├── README.md
├── peach-observability-autoconfigure/
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/peach/observability/
│       │   ├── autoconfigure/
│       │   ├── config/
│       │   ├── core/
│       │   └── web/
│       ├── main/resources/META-INF/
│       └── test/java/
└── peach-observability-starter/
    └── pom.xml
```

## 快速接入

业务运行模块只依赖 starter：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-observability-starter</artifactId>
</dependency>
```

最小生产配置示例：

```yaml
peach:
  observability:
    enabled: true
    request-id:
      enabled: true
      header-name: X-Request-Id
      trust-incoming: true

management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  tracing:
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

`prometheus` 端点只能在内网管理端口或受认证保护的网络中开放。不要把 `env`、`beans`、`configprops`、`heapdump` 等端点直接暴露到公网。

响应式网关还需要：

```yaml
spring:
  reactor:
    context-propagation: auto
```

## 核心对象

| 对象 | 职责 |
| --- | --- |
| `PeachObservabilityProperties` | 绑定 `peach.observability` 自定义配置 |
| `RequestIdGenerator` | 可覆盖的请求 ID 生成契约 |
| `UuidRequestIdGenerator` | 默认 UUID 请求 ID 实现 |
| `RequestIdResolver` | 上游信任和安全格式校验 |
| `RequestIdServletFilter` | Servlet 请求属性、响应头和 MDC 生命周期管理 |
| `PeachObservabilityAutoConfiguration` | 核心 Bean 自动配置 |
| `PeachServletObservabilityAutoConfiguration` | Servlet 条件自动配置 |

## 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.observability.enabled` | `true` | 是否启用自定义自动配置 |
| `peach.observability.request-id.enabled` | `true` | 是否注册 Servlet 请求 ID 过滤器 |
| `peach.observability.request-id.header-name` | `X-Request-Id` | 请求头与响应头名称 |
| `peach.observability.request-id.trust-incoming` | `true` | 是否接受格式合法的上游值；公网网关应关闭 |
| `peach.observability.request-id.min-length` | `8` | 最小长度 |
| `peach.observability.request-id.max-length` | `64` | 最大长度 |

指标、Trace 和 OTLP 使用 Spring Boot 标准 `management.*` 配置，不提供含义重复的 Peach 配置别名。

## 扩展方式

可以覆盖默认请求 ID 生成器：

```java
@Bean
RequestIdGenerator requestIdGenerator() {
    return customGenerator::nextId;
}
```

生成值必须只包含 ASCII 字母、数字、下划线和连字符，且满足长度限制；禁止包含用户信息、租户信息、Token 或其他敏感内容。

## 运行机制

1. Gateway 在公网信任边界生成可信 `X-Request-Id`。
2. 下游 Servlet 服务接受合法的上游 RequestId，否则重新生成。
3. 过滤器把 RequestId 写入响应头、Servlet 请求属性和 MDC。
4. Micrometer/OpenTelemetry 自动创建和传播 traceId、spanId。
5. Feign 使用框架观测能力传播标准 Trace Context，现有拦截器继续传播 `X-Request-Id`。
6. `peach-threadpool` 在 `enable-mdc=true` 时传播 Micrometer ThreadLocal 和 MDC。
7. `peach-rocket` 通过中立 SPI 注入和提取 Trace Context，消费端创建 Consumer Span。

各运行模块的 `ALL_FILE` 使用 Spring Boot 3.5 内置 Logstash JSON Encoder，MDC 中的 requestId、traceId、spanId 会作为结构化字段写入；控制台和按级别文件保留便于人工阅读的键值格式。

### traceId 和 spanId 的注入位置

`traceId`、`spanId` 不由 `RequestIdServletFilter` 生成，也没有业务代码手工写入 MDC：

1. Spring MVC、WebFlux、Feign 等受观测组件创建 Micrometer `Observation`。
2. `micrometer-tracing-bridge-otel` 把 Observation 转换为 OpenTelemetry Span。
3. Span 进入当前作用域时，Micrometer Tracing 的日志关联上下文自动写入 MDC 的 `traceId` 和 `spanId`。
4. Span 退出作用域时自动清理；OTLP Exporter 异步把已采样 Span 发送到 Collector。

因此，只有当前线程或 Reactor 上下文中存在活动 Span 时，这两个字段才有值。`traceId` 表示整条调用链，`spanId` 表示当前 HTTP、Feign、MQ 或异步步骤。它们默认不作为响应头返回，可通过结构化日志或 Tempo 查询。

部署环境至少需要：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  tracing:
    sampling:
      probability: 0.1
  otlp:
    tracing:
      endpoint: http://otel-collector:4318/v1/traces
```

`deploy-pipline/docker-compose.deploy.yml` 已通过等价环境变量提供这些配置。排查时可临时把采样率调整为 `1.0`，生产环境不建议长期全量采样。

## 边界与安全

- RequestId 不替代 traceId；不得手工拼装 `traceparent` 或 `tracestate`。
- RequestId 不作为 Prometheus 标签，避免高基数时序爆炸。
- 默认只采用 Spring Boot 的 Actuator 安全暴露策略，不自动开放 Prometheus。
- 日志只记录稳定且非敏感的标识、结果和耗时，不记录完整 DTO、请求体、Token、Cookie 或签名 URL。
- Collector 不可用时不能阻断业务请求；导出超时、队列和采样率通过 Spring Boot/OpenTelemetry 配置控制。
- RocketMQ 只传播标准 Trace Context，不把消息 ID、业务 key 等高基数字段作为指标标签。

## 构建与验证

```bash
mvn -pl :peach-observability-autoconfigure,:peach-observability-starter -am test -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `/actuator/prometheus` 返回 404 | 是否在 exposure 中包含 `prometheus` | 只在受保护管理网络中开放该端点 |
| 没有 Trace 数据 | OTLP endpoint、采样率、Collector 连通性 | 检查 `management.otlp.tracing.*` |
| Servlet 日志缺少 requestId | starter、过滤器开关、日志 MDC 格式 | 检查自动配置条件和 `%X{requestId}` |
| 下游 requestId 变化 | 请求头名称或上游值格式 | 全链路统一 `X-Request-Id` 并检查长度限制 |
| 出现两个 RequestId 过滤器 | 仍有业务模块手工注册历史 `RequestIdFilter` | 删除手工注册；Sa-Token 已不再自动创建该过滤器 |
