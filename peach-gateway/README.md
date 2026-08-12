# peach-gateway

[English](README.en-US.md) | 中文

最后更新：2026-08-09

`peach-gateway` 是 Peach Cloud 的统一流量入口，基于 Spring Cloud Gateway 提供路由转发、服务发现、网关侧 Sa-Token 认证、Same-Token 传递、基础风控、请求 ID、访问日志、CORS、Knife4j 聚合和统一安全异常响应。

## 模块定位

本模块负责外部 HTTP/WebSocket 流量进入后端业务服务前的网关层处理。

提供能力：

- Spring Cloud Gateway HTTP 与 WebSocket 路由。
- Nacos 服务发现和 Nacos 配置导入。
- 网关侧 Sa-Token 登录态校验。
- 复用 `peach-satoken-autoconfigure` 的 Sa-Token Redis DAO 和 Jackson Session 序列化。
- Same-Token 注入，用于下游服务间认证。
- 基础风控：URI 长度、请求头数量、危险 HTTP 方法、静态黑名单、Redis 动态 IP 黑名单。
- 统一异常映射，避免向客户端返回原始异常细节。
- 请求 ID 传递和访问日志，不记录 query、body、token 或完整 DTO。
- Knife4j/OpenAPI 文档聚合。

不提供能力：

- 用户、角色、菜单、权限数据维护，这些属于 `peach-auth`。
- 业务服务内的 `SecurityContextHolder` 上下文恢复。业务服务通过聚合 `peach-satoken`，使用 `StpUtil.getLoginId()` 和缓存数据恢复。
- 完整 WAF、DDoS 防护、灰度发布或生产网关治理。
- 绕过网关直连业务服务时的服务端防护。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-gateway-core` | 网关过滤器、安全匹配、配置属性和通用支持代码 |
| `peach-gateway-launch` | Spring Boot 启动模块、profile 引导配置和 logback 配置 |

## 核心类

| 能力 | 类 / 配置 | 说明 |
| --- | --- | --- |
| 请求 ID | `GatewayRequestGlobalFilter` | 写入 `X-Request-Id` 到下游请求和响应 |
| 基础风控 | `GatewayRiskControlGlobalFilter`、`GatewayRiskControlProperties` | 拦截异常请求形态和显式黑名单 |
| 登录校验 | `GatewayAuthorizationGlobalFilter`、`GatewaySaTokenProperties` | 非公开端点执行 `StpUtil.checkLogin()` |
| Same-Token | `GatewaySameTokenGlobalFilter` | 为下游请求注入 Sa-Token Same-Token |
| 统一异常 | `GatewayExceptionGlobalFilter` | 将认证、权限、路由、4xx、5xx 异常映射为安全 JSON |
| 访问日志 | `GatewayAccessLogGlobalFilter` | 只记录 method、path、status、duration、requestId、client |
| 公开端点 | `GatewaySecurityEndpointMatcher`、`GatewaySecurityEndpointRule` | 网关独立维护公开端点匹配 |
| Sa-Token 存储 | `PeachSaTokenDao`、`PeachSaSessionForJacksonCustomized` | 与业务服务共享 Redis/session 契约 |
| CORS | `GatewayCorsConfig` | 注册响应式 CORS 配置 |

过滤器顺序：

| 顺序 | 过滤器 | 作用 |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE` | `GatewayExceptionGlobalFilter` | 捕获后续过滤器和路由链异常 |
| `-300` | `GatewayRequestGlobalFilter` | 生成并传递请求 ID |
| `-250` | `GatewayRiskControlGlobalFilter` | 执行基础风控 |
| `-200` | `GatewayAuthorizationGlobalFilter` | 执行 Sa-Token 登录校验 |
| `-150` | `GatewaySameTokenGlobalFilter` | 注入 Same-Token |
| `Ordered.LOWEST_PRECEDENCE` | `GatewayAccessLogGlobalFilter` | 记录访问完成日志 |

## 配置布局

本地 profile 文件只负责启动引导：

- `peach-gateway-launch/src/main/resources/application-dev.yml`
- `peach-gateway-launch/src/main/resources/application-prod.yml`
- `peach-gateway-launch/src/main/resources/application-docker.yml`

网关运行态配置集中放在 Nacos：

- `deploy/nacos/config/peach-gateway.yml`

profile 文件通过以下方式导入 Nacos 配置：

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.${spring.cloud.nacos.config.file-extension}?group=${spring.cloud.nacos.config.group}&namespace=${spring.cloud.nacos.config.namespace}
```

## 路由

业务路由位于 `deploy/nacos/config/peach-gateway.yml`：

| 路由 ID | Path | 下游服务 | 过滤器 |
| --- | --- | --- | --- |
| `peach-auth` | `/api/auth/**` | `lb://peach-auth` | `StripPrefix=1` |
| `peach-monitor` | `/api/monitor/**` | `lb://peach-monitor` | `StripPrefix=1` |
| `peach-fileservice` | `/api/file/**` | `lb://peach-fileservice` | `StripPrefix=1` |
| `peach-setting` | `/api/setting/**` | `lb://peach-setting` | `StripPrefix=1` |
| `peach-message` | `/api/message/**` | `lb://peach-message` | `StripPrefix=1` |
| `peach-message-ws` | `/webSocket/**` | `lb:ws://peach-message` | 无 |
| `peach-generator` | `/api/generator/**` | `lb://peach-generator` | `StripPrefix=1` |

文档路由：

| 路由 ID | Path | 下游服务 | 过滤器 |
| --- | --- | --- | --- |
| `peach-auth-swagger` | `/api/auth/v3/api-docs` | `lb://peach-auth` | `SetPath=/v3/api-docs` |
| `peach-monitor-swagger` | `/api/monitor/v3/api-docs` | `lb://peach-monitor` | `SetPath=/v3/api-docs` |
| `peach-fileservice-swagger` | `/api/file/v3/api-docs` | `lb://peach-fileservice` | `SetPath=/v3/api-docs` |
| `peach-setting-swagger` | `/api/setting/v3/api-docs` | `lb://peach-setting` | `SetPath=/v3/api-docs` |
| `peach-message-swagger` | `/api/message/v3/api-docs` | `lb://peach-message` | `SetPath=/v3/api-docs` |
| `peach-generator-swagger` | `/api/generator/v3/api-docs` | `lb://peach-generator` | `SetPath=/v3/api-docs` |

## 配置项

`peach.gateway.satoken` 对应 `GatewaySaTokenProperties`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 是否启用网关认证和网关 Sa-Token 定制 |
| `inject-same-token` | `true` | 是否为下游请求注入 Same-Token |
| `token-strategy-enabled` | `true` | 是否覆盖 Sa-Token token 生成策略 |
| `log-path` | `true` | 是否记录公开端点放行日志 |
| `public-endpoints` | 源码默认列表 | 跳过认证和风控的公开端点 |

`peach.gateway.risk-control` 对应 `GatewayRiskControlProperties`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | 代码默认 `false`，Nacos 配置为 `true` | 是否启用风控过滤器 |
| `max-uri-length` | `2048` | 原始 URI 最大长度 |
| `max-header-count` | `100` | 请求头最大数量 |
| `blocked-ips` | 空 | 静态客户端 IP 黑名单，英文逗号分隔 |
| `blocked-user-agents` | 空 | 静态 User-Agent 黑名单，英文逗号分隔 |

Redis 动态 IP 黑名单使用 Redis Set：

```text
peach:gateway:risk-control:blocked-ip:
```

网关 Redis 连接使用 Spring Boot 原生配置，对应共享 `PeachSaTokenDao` 和风控动态黑名单：

| 配置项 | 说明 |
| --- | --- |
| `spring.redis.host` | Redis 主机 |
| `spring.redis.port` | Redis 端口 |
| `spring.redis.password` | Redis 密码，建议通过环境变量注入 |
| `spring.redis.database` | Redis DB |
| `spring.redis.timeout` | Redis 命令超时时间 |
| `spring.redis.lettuce.pool.*` | Lettuce 连接池配置 |

## 边界与限制

- 网关与业务服务共享 `peach-satoken-autoconfigure` 的 DAO 和 Session 序列化实现。
- 网关不使用业务侧 `SecurityContextHolder`。
- 网关不依赖 `peach-redis-common`。
- 网关代码日志统一使用英文，且不能记录 request body、token、password 或完整 DTO。
- 公开端点列表由网关独立维护，不会自动同步业务服务配置。
- Redis 动态黑名单查询失败时降级放行，并记录英文 WARN 日志。

## 构建与验证

编译网关：

```bash
mvn -f peach-gateway\pom.xml -pl peach-gateway-launch -am clean compile -Pdevelopment
```

检查编码和 diff：

```bash
node scripts\check-utf8.mjs
git diff --check -- peach-gateway deploy/nacos/config/peach-gateway.yml
```

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 返回 `401` | token、Redis Sa-Token 数据、公开端点配置 | 确认登录服务发放 token，再检查 `peach.gateway.satoken.public-endpoints` |
| 返回 `403` | 权限异常、Same-Token 异常或风控拒绝 | 查看 `Gateway authorization rejected` 或 `Gateway risk-control rejected request` 日志 |
| 返回 `404` | 路由 Path、`StripPrefix`、下游 Controller 路径 | 检查 Nacos 中的 `spring.cloud.gateway.routes` |
| Swagger 打不开 | 网关文档路由和下游 `/v3/api-docs` | 先直连下游文档端点，再检查网关 `SetPath` |
| WebSocket 失败 | `lb:ws://peach-message`、`/webSocket/**`、服务注册 | 检查浏览器地址、Nacos 服务名和 message 端点 |
| 风控未生效 | `peach.gateway.risk-control.enabled` 和公开端点匹配 | 确认 Nacos 已开启风控，且路径未命中公开端点 |
