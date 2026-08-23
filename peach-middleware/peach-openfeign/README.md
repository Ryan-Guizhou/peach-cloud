# peach-openfeign

[English](README.en-US.md) | 中文

最后更新时间：2026-08-12

artifactId：`peach-openfeign`

适用版本：Java 21、Spring Boot `3.5.4`、Spring Cloud `2025.0.0`、Spring Cloud Alibaba `2025.0.0.0`

## 模块定位

`peach-openfeign` 是 Peach Cloud 的服务间 HTTP 调用治理模块，提供 OpenFeign starter 和自动配置。

它负责：

- Feign 发起端自动携带 Same-Token 与 RequestId。
- OkHttp、默认超时和按客户端超时覆盖。
- 幂等/显式白名单重试。
- Sentinel 限流、熔断和 Nacos 动态规则接入。
- Feign 异常统一分类和安全响应。
- `fallbackFactory` 启动期检查与业务降级辅助。

它不负责：

- 业务接口定义；接口仍由各业务 `*-openfeign-external` 模块维护。
- 消费端 Same-Token 白名单与入站校验；业务服务引入 `peach-satoken-starter` 后由 `peach-satoken` 统一校验。
- 通用业务 Header 透传；当前只传播 Same-Token 和 RequestId。
- 替业务决定降级数据语义；starter 只提供异常识别和通用失败响应工具。
- 大文件中转治理；超过 `peach.openfeign.upload-max-bytes` 的文件应优先走对象存储直传。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-openfeign-autoconfigure` | 自动配置、配置类、拦截器、重试、异常、Sentinel 基线规则、fallback 校验 |
| `peach-openfeign-starter` | 业务接入入口，聚合 autoconfigure、OkHttp、Sentinel |

## 接入方式

业务模块优先依赖对应业务域的 `*-openfeign-external`。需要直接接入 starter 时使用：

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-openfeign-starter</artifactId>
</dependency>
```

Feign 客户端必须声明稳定的 `contextId`，并优先使用 `fallbackFactory`：

```java
@FeignClient(
        contextId = "messageFeignClient",
        name = ServiceNameConstant.MESSAGE_SERVICE,
        path = ServicePathConstant.MESSAGE_PATH_SERVICE,
        fallbackFactory = MessageFeignClientFallbackFactory.class
)
public interface MessageFeignClient {
}
```

## 核心配置

公共 Nacos 配置位于 `deploy/nacos/config/peach-openfeign.yml`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `peach.openfeign.enabled` | `true` | 启用模块自动配置 |
| `peach.openfeign.same-token-enabled` | `true` | 注入 Sa-Token Same-Token |
| `peach.openfeign.same-token-fail-fast` | `false` | Same-Token 缺失时是否拒绝调用；公共 Nacos 配置建议生产设为 `true` |
| `peach.openfeign.request-id-enabled` | `true` | 注入 RequestId |
| `peach.openfeign.upload-max-bytes` | `10485760` | Feign 上传上限，默认 10MB |
| `peach.openfeign.client.connect-timeout-millis` | `3000` | 默认连接超时 |
| `peach.openfeign.client.read-timeout-millis` | `10000` | 默认读超时 |
| `peach.openfeign.client.named.*.read-timeout-millis` | 当前配置 | 按 `contextId` 覆盖读超时 |
| `peach.openfeign.retry.enabled` | `true` | 启用有限重试 |
| `peach.openfeign.retry.max-attempts` | `2` | 最大尝试次数，包含首次调用 |
| `peach.openfeign.retry.methods` | `GET,HEAD` | 允许重试的方法；写请求必须显式加入 |
| `peach.openfeign.retry.statuses` | `429,503,504` | 允许重试的 HTTP 状态码 |
| `peach.openfeign.retry.exceptions` | 连接/超时/Feign 临时异常 | 允许归类为临时失败的异常类名 |
| `peach.openfeign.sentinel.enabled` | `true` | 启用 Sentinel 自动配置 |
| `peach.openfeign.sentinel.flow-data-id` | `peach-openfeign-sentinel-flow-rules` | 限流规则 dataId |
| `peach.openfeign.sentinel.degrade-data-id` | `peach-openfeign-sentinel-degrade-rules` | 熔断规则 dataId |
| `peach.openfeign.exception.expose-remote-message` | `false` | 是否向前端暴露下游异常消息 |
| `peach.openfeign.fallback.validate-on-startup` | `true` | 启动时检查 fallback/fallbackFactory |
| `peach.openfeign.fallback.fail-fast-if-missing` | `true` | 非生产环境缺失 fallback 时启动失败 |
| `feign.sentinel.enabled` | `true` | Spring Cloud Alibaba Sentinel Feign 集成 |
| `feign.circuitbreaker.enabled` | `true` | Spring Cloud OpenFeign 熔断集成开关 |
| `feign.sentinel.rules` | 当前配置 | Feign 客户端级/方法级 Sentinel 熔断规则 |

## Sentinel 规则

本模块以 Sentinel 为唯一默认治理 Provider。Nacos 规则文件：

| 文件 | rule-type | 说明 |
| --- | --- | --- |
| `deploy/nacos/config/peach-openfeign-sentinel-flow-rules.json` | `flow` | Feign 客户端限流规则 |
| `deploy/nacos/config/peach-openfeign-sentinel-degrade-rules.json` | `degrade` | Feign 客户端熔断/慢调用规则 |

规则分两层：

- `feign.sentinel.rules`：Feign 客户端级/方法级熔断规则，随 `peach-openfeign.yml` 下发。
- `spring.cloud.sentinel.datasource`：从 Nacos 加载 flow/degrade JSON。生产环境以 Sentinel 控制台实际资源名为准校准规则。

当前规则由 `peach-openfeign.yml` 中的 `spring.cloud.sentinel.datasource` 接入 Nacos；生产阈值以 Nacos 动态规则为准。

## 运行机制

```text
业务 Service
  -> @FeignClient 代理
  -> PeachOpenfeignRequestInterceptor：Same-Token / RequestId
  -> OkHttp / Feign Client：连接与读取超时
  -> Sentinel：限流 / 熔断 / 降级
  -> PeachOpenFeignErrorDecoder：HTTP 错误统一异常
  -> PeachOpenfeignRetryer：有限重试
  -> fallbackFactory：业务降级
  -> PeachOpenfeignExceptionHandler：统一安全响应
```

### Same-Token 与 RequestId

- Same-Token 优先从当前 HTTP 请求头读取；当前请求头不存在时，使用 `SaSameUtil.getToken()` 获取服务间 token。
- RequestId 只从当前 HTTP 请求头读取并透传，不创建新 RequestId，不依赖 MDC，也不维护自定义上下文。
- 非 Servlet 请求线程、异步任务或定时任务内发起 Feign 时，通常没有原始 RequestId；如果开启 `same-token-fail-fast=true` 且无法获取 Same-Token，会在调用发起前失败。
- 入站 Same-Token 校验由消费方 `peach-satoken-starter` 负责；对外公开接口不要加入 Same-Token 内部校验白名单。

## 降级要求

- 所有新增 `@FeignClient` 必须配置 `fallbackFactory`。
- 非生产环境缺少 fallback 时默认启动失败；`prod`、`production`、`docker` profile 下只告警。
- fallback 不应记录完整 DTO、token、文件内容、签名 URL 或下游响应体。
- 日志不记录 Same-Token 原文，也不记录包含查询参数的完整 URL；需要定位时使用 client、method、path、cause。
- 可注入 `PeachFeignFallbackSupport` 识别限流、熔断降级、超时、重试耗尽等原因。

## 边界与限制

- 默认只允许 `GET/HEAD` 重试；写请求必须显式加入 `peach.openfeign.retry.methods`。
- 重试状态码会先转换为可重试异常，重试耗尽后保留原始分类语义，例如 429 仍按限流处理，503/504 仍按服务不可用处理。
- Sentinel 限流/熔断不参与重试，避免和治理规则对抗。
- Resilience4j 不再作为默认配置样例，避免双熔断体系并存。
- 不提供通用 Header 透传，避免服务间调用隐式携带用户凭据或代理链路信息。
- `settingFeginClient` 是历史 `contextId` 拼写，当前保持兼容；新客户端命名应使用 `Feign`。

## 构建与验证

推荐验证命令：

```bash
node scripts/check-utf8.mjs
mvn -pl peach-middleware/peach-openfeign/peach-openfeign-autoconfigure -am -DskipTests compile -Pdevelopment
mvn -pl peach-setting/peach-setting-openfeign-external,peach-monitor/peach-monitor-openfeign-external -am -DskipTests compile -Pdevelopment
git diff --check
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 启动失败提示缺少 fallback | `@FeignClient` 是否配置 `fallbackFactory` | 为客户端补齐 fallbackFactory；生产可只告警 |
| 调用被限流 | `peach-openfeign-sentinel-flow-rules.json`、Sentinel 控制台 | 调整 QPS 或拆分热点接口 |
| 调用被熔断 | `peach-openfeign-sentinel-degrade-rules.json`、下游错误率/慢调用 | 降低下游错误率，适当调整熔断窗口 |
| 启动时报 `Failed to introspect ... NacosDataSourceFactoryBean` | 运行包是否包含 `sentinel-datasource-nacos` | `peach-openfeign-starter` 已显式依赖 `com.alibaba.csp:sentinel-datasource-nacos`；重新 Maven 打包并重建业务镜像 |
| 写请求未重试 | `peach.openfeign.retry.methods` | 确认接口幂等后显式加入方法白名单 |
| 文件调用超时 | `fileFeignClient` 超时配置、文件大小 | 小文件可调读超时，大文件改对象存储直传 |
| 下游错误暴露给前端 | `peach.openfeign.exception.expose-remote-message` | 生产保持 `false` |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
