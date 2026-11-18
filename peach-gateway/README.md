# peach-gateway

[English](README.en-US.md) | 中文

最后更新：2026-11-05

`peach-gateway` 是 Peach Cloud 的统一流量入口，基于 Spring Cloud Gateway 提供路由转发、服务发现、网关侧 Sa-Token 登录校验、Same-Token 注入、基础风控、请求 ID、访问日志、CORS、Knife4j 聚合和统一安全异常响应。

## 模块定位

本模块负责外部 HTTP/WebSocket 流量进入后端业务服务前的网关层处理。

提供能力：

- Spring Cloud Gateway HTTP 与 WebSocket 路由。
- 多维度、多算法分布式限流：Global、IP、URI、IP_URI、Tenant、Tenant_URI；支持固定窗口、滑动窗口日志、滑动窗口计数器、令牌桶、漏桶和 GCRA。
- Nacos 限流规则热刷新、可信代理 IP 解析、Token Session 租户隔离、限流白名单/黑名单和 Redis 故障策略。
- 429 标准限流响应头、低基数 Micrometer 指标和真实 Redis 并发集成测试。
- OSS Lua 脚本精确主机/端口白名单、DNS 解析地址校验与连接固定、SHA-256、受限单 Key 脚本契约和 readiness 指标。
- Nacos 服务发现和 Nacos 配置导入。
- 网关侧 Sa-Token 登录态校验。
- 网关独立维护 Reactor 场景下的 Sa-Token Redis DAO、Session 序列化和 token 策略。
- Same-Token 注入，用于已登录请求进入下游业务服务后的服务间校验。
- 基础风控：URI 长度、请求头数量、危险 HTTP 方法、静态黑名单、Redis 动态 IP 黑名单。
- 统一异常映射，避免向客户端返回原始异常细节。
- 请求 ID 传递和访问日志，不记录 query、body、token、password 或完整 DTO。
- Knife4j/OpenAPI 文档聚合。

不提供能力：

- 用户、角色、菜单、权限数据维护，这些属于 `peach-auth`。
- 业务服务内 `SecurityContextHolder` 上下文恢复；业务服务通过引入 `peach-satoken-starter` 恢复当前用户上下文。
- 完整 WAF、DDoS 防护、灰度发布或生产网关治理。
- 绕过网关直连业务服务时的入口防护。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-gateway-core` | 网关过滤器、安全端点匹配、网关侧 Sa-Token 配置、配置属性和通用支持代码 |
| `peach-gateway-launch` | Spring Boot 启动模块、profile 引导配置和 logback 配置 |

## 网关侧 Sa-Token 配置

网关不依赖 `peach-satoken`，但会在 `peach-gateway-core` 中维护自己的 Sa-Token 能力。该模块属于网关服务内部业务模块，不是 starter，也不通过 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 对外声明自动配置。

这些配置类由 `peach-gateway-launch` 启动类的 `@ComponentScan("com.peach")` 扫描加载：

| 类 | 职责 |
| --- | --- |
| `GatewayCorePropertiesConfiguration` | 启用 `GatewaySaTokenProperties` 和 `GatewayRiskControlProperties` |
| `GatewaySaTokenDaoConfiguration` | 注册网关侧 `SaTokenDao`，使用 `PeachSaTokenDao` 读写共享 Redis |
| `GatewaySaTokenSessionStrategyConfiguration` | 覆盖 Sa-Token Session 创建策略，保持 Jackson 序列化契约 |
| `GatewaySaTokenStrategyConfiguration` | 按配置覆盖 Sa-Token token 生成策略 |

命名上避免继续使用 `AutoConfiguration` 后缀，防止与业务服务侧 `peach-satoken` 的 starter 自动配置类混淆。

## 核心类

| 能力 | 类 / 配置 | 说明 |
| --- | --- | --- |
| 请求 ID | `GatewayRequestGlobalFilter` | 写入 `X-Request-Id` 到下游请求和响应 |
| 基础风控 | `GatewayRiskControlGlobalFilter`、`GatewayRiskControlProperties` | 拦截异常请求形态和显式黑名单 |
| `GatewayRateLimitGlobalFilter`、`RateLimitEnforcementService` | 鉴权后执行多规则限流，按优先级短路 |
| `RateLimitRuleRegistry`、`RateLimitRuleMatcher` | 校验 Nacos 规则并原子刷新规则快照 |
| `RedisRateLimitExecutor`、`RateLimitScriptRepository` | 响应式 Redis Lua 执行、远程 OSS / 本地脚本加载 |
| 登录校验 | `GatewayAuthorizationGlobalFilter`、`GatewaySaTokenProperties` | 非公开端点执行 `StpUtil.checkLogin()` |
| Same-Token | `GatewaySameTokenGlobalFilter` | 非公开端点向下游请求注入 Sa-Token Same-Token |
| 统一异常 | `GatewayExceptionGlobalFilter` | 将认证、权限、路由、4xx、5xx 异常映射为安全 JSON |
| 访问日志 | `GatewayAccessLogGlobalFilter` | 只记录 method、path、status、durationMs、requestId、client |
| 公开端点 | `GatewaySecurityEndpointMatcher`、`GatewaySecurityEndpointRule` | 网关独立维护公开端点匹配 |
| Sa-Token 存储 | `PeachSaTokenDao`、`PeachSaSessionForJacksonCustomized` | 网关独立实现，与业务服务保持 Redis/session 契约一致 |
| CORS | `GatewayCorsConfig` | 注册响应式 CORS 配置 |

## 过滤器执行顺序

| 顺序 | 过滤器 | 作用 |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE` | `GatewayExceptionGlobalFilter` | 捕获后续过滤器和路由链异常 |
| `-300` | `GatewayRequestGlobalFilter` | 生成并传递请求 ID |
| `-250` | `GatewayRiskControlGlobalFilter` | 执行基础风控 |
| `-200` | `GatewayAuthorizationGlobalFilter` | 执行 Sa-Token 登录校验 |
| `-175` | `GatewayRateLimitGlobalFilter` | 使用可信 IP 和 Token Session 租户上下文执行限流 |
| `-150` | `GatewaySameTokenGlobalFilter` | 注入 Same-Token |
| `Ordered.LOWEST_PRECEDENCE` | `GatewayAccessLogGlobalFilter` | 请求结束后记录访问日志 |

公开端点命中 `peach.gateway.satoken.public-endpoints` 后，会跳过风控、登录校验和 Same-Token 注入。

## 日志格式

网关安全相关日志统一使用英文、参数化日志，并保持关键字段顺序：

```text
requestId={}, method={}, path={}, status={}, reason={}
requestId={}, method={}, path={}, client={}, reason={}
requestId={}, method={}, path={}, status={}, durationMs={}, client={}
```

日志禁止记录 request body、query、token、password、完整 DTO 和 Redis 密码。可记录的业务标识限定为 requestId、method、path、status、durationMs、client、reason。

## 配置布局

本地 profile 文件只负责启动引导：

- `peach-gateway-launch/src/main/resources/application-dev.yml`
- `peach-gateway-launch/src/main/resources/application-prod.yml`
- `peach-gateway-launch/src/main/resources/application-docker.yml`

网关运行态配置集中在 Nacos：

- `deploy/nacos/config/peach-gateway.yml`
- `deploy-pipline/init/nacos/config/peach-gateway.yml`

限流配置、算法选型、响应头、指标和故障策略详见 [多算法限流使用与运维指南](docs/rate-limit.md)。生产 OSS 地址白名单、Lua 安全契约和故障恢复详见该指南；压测、Redis Cluster/Sentinel/TLS 手动验证、Grafana 面板和 Prometheus 告警详见 [限流性能与运维验证](docs/performance/README.md)。

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
| `inject-same-token` | `true` | 是否为非公开端点的下游请求注入 Same-Token |
| `token-strategy-enabled` | `true` | 是否覆盖 Sa-Token token 生成策略 |
| `log-path` | `true` | 是否记录公开端点放行日志 |
| `public-endpoints` | 源码默认列表 / Nacos 可覆盖 | 跳过风控、登录校验和 Same-Token 注入的公开端点 |

`peach.gateway.risk-control` 对应 `GatewayRiskControlProperties`。

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `enabled` | 代码默认 `false`，Nacos 示例为 `true` | 是否启用风控过滤器 |
| `max-uri-length` | `2048` | 原始 URI 最大长度 |
| `max-header-count` | `100` | 请求头最大数量 |
| `blocked-ips` | 空 | 静态客户端 IP 黑名单，英文逗号分隔 |
| `blocked-user-agents` | 空 | 静态 User-Agent 黑名单，英文逗号分隔 |

Redis 动态 IP 黑名单使用 Redis Set：

```text
peach:gateway:risk-control:blocked-ip:
```

网关 Redis 连接使用 `peach-redis.yml` 中的 `peach.redis.*` 配置，必须与业务服务连接同一套 Sa-Token Redis 数据。网关进程只创建一条 Lettuce 连接：限流使用响应式 API，Sa-Token 复用这条连接的阻塞 API，并在 `boundedElastic` 执行。网关不加载 Jedis 和 Redisson，业务服务仍使用 Jedis。

## 边界与限制

- 网关不依赖 `peach-satoken`，不加载业务侧 Servlet 过滤器，也不使用业务侧 `SecurityContextHolder`。
- 网关必须与业务服务保持 Sa-Token Redis key、`sa-token.token-name` 和 Session 序列化契约一致。
- 公开端点列表由网关独立维护，不会自动同步业务服务配置。
- Redis 动态黑名单查询失败时降级放行，并记录英文 WARN 日志。
- 限流默认关闭；启用后 Redis 不可用默认 FAIL_CLOSED 返回 503，可通过 Nacos 切换 FAIL_OPEN。
- 租户限流只读取当前 Sa-Token Token Session 中的 tenantId，不信任请求头或按 userId 共享的上下文缓存；认证登录会为每次登录签发独立 token，避免设备间上下文覆盖。
- 限流白名单绕过配额、黑名单直接拒绝。动态启用后，在六种 Lua 脚本全部加载成功前，配额检查返回不可用，并按 failure-mode 处理：FAIL_CLOSED 返回 503，FAIL_OPEN 放行。IP/租户白名单和黑名单仍然执行。公开登录接口不应用租户规则，避免无 Token 请求被返回 401。
- Same-Token 只对非公开端点注入；未登录公开请求不会依赖服务间凭证。

## 构建与验证

编译并验证网关与认证服务：

```bash
mvn -pl peach-gateway/peach-gateway-core,peach-auth/peach-auth-service -am verify -Pdevelopment
```

限流 Redis 集成测试使用 Testcontainers 启动真实 Redis；本地执行需要 Docker。

检查编码和 diff：

```bash
node scripts/check-utf8.mjs
git diff --check -- peach-gateway
```

## 排障

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 返回 `401` | token 请求头、Sa-Token Redis 数据、公开端点配置 | 确认登录服务发放 token，再检查 `peach.gateway.satoken.public-endpoints` |
| 返回 `403` | 权限异常、Same-Token 异常或风控拒绝 | 查看 `Gateway authorization rejected`、`Gateway same-token relay failed` 或 `Gateway risk-control rejected request` 日志 |
| 返回 `404` | 路由 Path、`StripPrefix`、下游 Controller 路径 | 检查 Nacos 中的 `spring.cloud.gateway.routes` |
| Swagger 打不开 | 网关文档路由和下游 `/v3/api-docs` | 先直连下游文档端点，再检查网关 `SetPath` |
| WebSocket 失败 | `lb:ws://peach-message`、`/webSocket/**`、服务注册 | 检查浏览器地址、Nacos 服务名和 message 端点 |
| 风控未生效 | `peach.gateway.risk-control.enabled` 和公开端点匹配 | 确认 Nacos 已开启风控，且路径未命中公开端点 |
| 返回 `429` | 命中的规则、算法、限额和 `Retry-After` | 检查 Nacos `peach.gateway.rate-limit.rules` 的 scope、paths、methods、priority 和算法参数 |
| 返回 `503` | Redis 连接、Lua 脚本加载、`failure-mode` | 检查 `peach.redis.*`、Lettuce 响应式连接、OSS SHA-256；确认是否预期使用 FAIL_CLOSED |
| 租户限流返回 `401` | 当前 token 的 Token Session 是否包含 `tenantId` | 重新登录或调用租户切换接口刷新 Token Session；不要通过请求头传租户 ID |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。

## SaaS 网关鉴权安全基线（Issue #208，开发中）

当前开发分支使用 `SaTokenConstant.TOKEN_SESSION_SAAS_CONTEXT` 的单一 Token Session 快照，解析 `userRefId/tenantRefId/memberRefId/appRefId/year/memberVersion/authVersion`；工作机构可空，不再读取历史 `tenantId/orgId/fiscal` 组合。新缓存键支持物理租户、成员、应用、自然年度和两个版本隔离，但**当前网关不会仅凭 Redis 缓存放行受保护 API**：必须注入能核验最新 IAM 权限事实源的 `GatewayIamDecisionVerifier`，没有验证器就默认拒绝；绝不回退读取旧用户级缓存。缺失快照、无 Redis、权限检查关闭或系统配置意外停用时，受保护 API 全部拒绝，不能通过开关进入无授权链路。默认匿名白名单只保留登录、验证码、公钥、只读登录初始化 `POST /auth/init` 以及 OPTIONS；注册、密码重置和 SaaS 管理端点不默认匿名公开。

**当前限制：** 缓存版本键不是可信版本服务。网关已实现 `GatewayIamHttpDecisionVerifier` 请求期在线决策，但默认关闭；启用时要求 Auth 服务端按最新数据库成员/授权版本重新验证，不依赖 Token 自报 `authVersion` 继续命中旧 Redis 权限集合。真实 MySQL/Redis/Nacos 环境中的服务身份、动态资源目录、撤销和故障联调仍未完成。**该分支暂不能投产**，CI 构建通过亦不等同于安全验收。


### 实时 IAM 受信链路（#208 与 #209 联调中）

当 `peach.gateway.iam.enabled=true` 时，网关的 `GatewayIamHttpDecisionVerifier` 通过 `@LoadBalanced WebClient.Builder` 访问服务发现名称 `peach-auth` 的 `POST /auth/external/iam/check-api`，使用当前最终用户 Sa-Token 与网关从 Sa-Token 获取的 Same-Token 两套独立凭证。认证域执行当前数据库成员版本与权限事实查询，并返回授权结果及**同次核验后的物理成员快照**；网关逐字段比较租户/成员/应用/年度和两个版本，完全一致且动作允许才继续进入业务服务。WebClient 网络过程不阻塞 Netty EventLoop，获取 Sa-Token 的潜在阻塞工作使用 boundedElastic。失败、超时、空响应、401/403/5xx 和 Token 切换均拒绝，没有旧 Redis 缓存兜底。

`peach.gateway.iam.enabled` 默认 `false`，网关受保护 API 因无验证器而**默认拒绝**；`peach.gateway.iam.timeout` 默认 `1500ms`。启用前必须先部署 #209 受信认证接口、配置正确的服务发现与 Same-Token、确保 Auth 数据库中已注册真实精确 METHOD:PATH API 权限定义，并用合法/撤销/跨租户/跨年度/停用成员及故障用例联调。当前仅提供精确 URL 权限，动态路径模板映射及真实业务对象权限接入尚未完成，不应把代码构建成功当作可直接上线。

#### 匿名白名单严格匹配（#208）

网关 `peach.gateway.satoken.public-endpoints` 的非 OPTIONS 配置现在只接受**明确 HTTP 方法 + 完全相等的公开路径**；`ANY`、`*`、`**`、`{id}` 不可用于匿名放行。仅允许精确的 `OPTIONS /**` 预检规则，其余携带编码字符、分号、双斜杠或路径遍历片段的可疑 URI 不会命中公开白名单，受保护路径仍需进入 IAM 并安全拒绝。不依赖 Nacos 传入的宽泛配置关闭系统权限防线。注意新增公开接口需要逐项登记精确方法和路径，不能用通配放行代替资源授权。

#### 登录初始化与平台初始化的语义区分

经核实 `LoginController.init()` 实际只调用 `IUserService.initLogin()` 读取登录页配置与 RSA 公钥，**不会创建租户或管理员**；它是未登录用户完成登录的前置能力。因此仅将 `POST /auth/init`、`POST /api/auth/init` 作为明确方法和精确路径的匿名入口恢复。所有 `/auth/saas/**` 和 `/auth/external/iam/**` 管理或内部受信接口仍不可匿名，`GET /auth/init` 也不放行。此修正只恢复登录所需的公开读取，不等于允许任意设置项对外泄露；登录配置内容白名单和生产值集检查仍须在 #220/#211 联调确认。

#### 外部内部凭证头隔离（#208）

`GatewayRequestGlobalFilter` 在其他授权与路由处理前移除客户端提供的 `SaSameUtil.SAME_TOKEN` 和 `X-Peach-Subject-Token`，同时重建可信请求 ID；不剥离真实用户的 `Authorization`。只有通过认证、授权的受保护请求才可由 `GatewaySameTokenGlobalFilter` 按当前服务配置注入 Same-Token；网关自行调用 Auth 的 IAM 验证器只在内部专用 HTTP 客户端设置真实最终用户 Token 和服务 Same-Token。即使客户端将这两个内部头送到公开端点，也不会转发到业务服务。生产部署仍须对 Auth 实例进行网络访问控制与 TLS 防护，不能把请求头清理当作唯一隔离机制。

#### 网关到 Auth 的 HTTP 契约回归

新增 `GatewayIamHttpContractTest`，用真实 WebClient 编解码器和模拟的 Auth HTTP 响应验证服务发现目标路径、POST 方法、最终用户 Token、独立 Same-Token、JSON Content-Type，以及数据库版本一致时允许。远端 503、空 200、授权版本不一致、缺少任一凭证或 25ms 测试超时均必须返回拒绝。此测试只验证 HTTP 协议与失败安全策略，不能代替真实 MySQL/Redis/Nacos 联调。

#### Nacos 初始化白名单和 IAM 上线门槛

已同步 `deploy-pipline/init/nacos/config/peach-gateway.yml`：删除旧的 `ANY`、文档/Actuator 通配匿名入口，以及 `/api/auth/register`、`/api/auth/logout`、`/api/auth/forget`、`/api/auth/reset` 等不应默认匿名的旧项。仅保留精确方法的登录、登录只读初始化、公钥、验证码和 `OPTIONS /**`。新增明确配置 `peach.gateway.iam.enabled=false` 与 `timeout=1500ms`；**该开关保持关闭时受保护请求默认拒绝，不能当作允许旧鉴权回退的开关**。先完整部署 #209 的受信 Auth 接口、Redis/MySQL、Nacos 服务发现及必要的 API 资源目录，再将在线 IAM 设置为 `true` 并完成合法/撤销/跨租户/跨年度安全回归。修改 Nacos 模板并不会自动更新已运行的 Nacos 配置实例，部署时需同步核对实际生效配置。

#### 登录态自服务操作（不依赖年度业务角色）

仅针对精确的 `POST /api/auth/logout` 与 `POST /api/auth/switchContext`（及对应非 `/api` 前缀），网关在 `StpUtil.checkLogin()` 成功后允许进入下游，而**不要求给每个年度角色额外配置登出/切换动作权限**。这不是匿名白名单：没有真实用户登录态仍返回 401，网关认证关闭仍 Fail-Closed。登出仅注销当前 Token；切换租户与年度由 Auth 重新验证现有成员与目标租户/应用有效性，并由 Auth 专用过滤器校验 Same-Token/数据库版本。任何 SaaS 角色管理或其他业务 API 仍必须通过在线 IAM。
