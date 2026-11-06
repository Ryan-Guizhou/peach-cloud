# peach-gateway

English | [中文](README.md)

Last updated: 2026-11-05

`peach-gateway` is the unified traffic entry point for Peach Cloud. It is based on Spring Cloud Gateway and provides routing, service discovery, gateway-side Sa-Token login checks, Same-Token injection, basic risk control, request ID propagation, access logs, CORS, Knife4j aggregation, and safe unified error responses.

## Scope

This module handles external HTTP/WebSocket traffic before it reaches backend business services.

It provides:

- HTTP and WebSocket routing through Spring Cloud Gateway.
- Multi-dimensional distributed rate limiting across Global, IP, URI, IP_URI, Tenant, and Tenant_URI scopes, with fixed window, sliding-window log, sliding-window counter, token bucket, leaky bucket, and GCRA algorithms.
- Nacos rule refresh, trusted-proxy IP resolution, per-token tenant isolation, rate-limit allow/deny lists, and Redis failure policies.
- Standard 429 rate-limit headers, low-cardinality Micrometer metrics, and real-Redis concurrency integration tests.
- Exact OSS Lua host/port allowlists, DNS address validation and connection pinning, SHA-256, a constrained single-key Lua contract, and readiness metrics.
- Nacos service discovery and Nacos configuration import.
- Gateway-side Sa-Token login-state verification.
- Gateway-local Sa-Token Redis DAO, Session serialization, and token strategy for Reactor runtime.
- Same-Token injection for authenticated downstream service calls.
- Basic risk control for URI length, header count, unsafe HTTP methods, static blocklists, and Redis dynamic IP blocklist.
- Safe exception mapping without leaking raw internal exception details.
- Request ID propagation and access logs without query, body, token, password, or full DTO data.
- Knife4j/OpenAPI document aggregation.

It does not provide:

- User, role, menu, or permission data management. Those belong to `peach-auth`.
- Business-service `SecurityContextHolder` restoration. Business services should use `peach-satoken-starter`.
- Full WAF, DDoS protection, gray release, or full production gateway governance.
- Protection for traffic that bypasses the gateway and calls business services directly.

## Modules

| Module | Responsibility |
| --- | --- |
| `peach-gateway-core` | Gateway filters, security endpoint matching, gateway-side Sa-Token configurations, properties, and shared support code |
| `peach-gateway-launch` | Spring Boot launch module, profile bootstrap config, and logback config |

## Gateway-side Sa-Token Configurations

Gateway does not depend on `peach-satoken`, but it maintains its own Sa-Token support inside `peach-gateway-core`. This module is an internal gateway business module, not a starter, and it does not expose Spring Boot auto-configuration through `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.

These configuration classes are loaded by `@ComponentScan("com.peach")` on the `peach-gateway-launch` application class:

| Class | Responsibility |
| --- | --- |
| `GatewayCorePropertiesConfiguration` | Enables `GatewaySaTokenProperties` and `GatewayRiskControlProperties` |
| `GatewaySaTokenDaoConfiguration` | Registers gateway-side `SaTokenDao` using `PeachSaTokenDao` |
| `GatewaySaTokenSessionStrategyConfiguration` | Overrides Sa-Token Session creation to keep Jackson serialization compatible |
| `GatewaySaTokenStrategyConfiguration` | Overrides Sa-Token token generation strategy when enabled |

The class names intentionally avoid the `AutoConfiguration` suffix to prevent confusion with the business-service `peach-satoken` starter auto-configurations.

## Key Classes

| Capability | Class / Config | Notes |
| --- | --- | --- |
| Request ID | `GatewayRequestGlobalFilter` | Writes `X-Request-Id` to downstream requests and responses |
| Risk control | `GatewayRiskControlGlobalFilter`, `GatewayRiskControlProperties` | Rejects abnormal request shapes and explicit blocklist hits |
| `GatewayRateLimitGlobalFilter`, `RateLimitEnforcementService` | Executes prioritized rate-limit rules after authentication |
| `RateLimitRuleRegistry`, `RateLimitRuleMatcher` | Validates Nacos rules and atomically refreshes rule snapshots |
| `RedisRateLimitExecutor`, `RateLimitScriptRepository` | Executes Lua through reactive Redis and loads OSS/local scripts |
| Authentication | `GatewayAuthorizationGlobalFilter`, `GatewaySaTokenProperties` | Runs `StpUtil.checkLogin()` for non-public endpoints |
| Same-Token | `GatewaySameTokenGlobalFilter` | Injects Sa-Token Same-Token for non-public downstream calls |
| Unified errors | `GatewayExceptionGlobalFilter` | Maps auth, permission, routing, 4xx, and 5xx errors to safe JSON |
| Access logs | `GatewayAccessLogGlobalFilter` | Logs method, path, status, durationMs, requestId, and client only |
| Public endpoints | `GatewaySecurityEndpointMatcher`, `GatewaySecurityEndpointRule` | Gateway-local public endpoint matching |
| Sa-Token storage | `PeachSaTokenDao`, `PeachSaSessionForJacksonCustomized` | Gateway-local implementation compatible with the shared Redis/session contract |
| CORS | `GatewayCorsConfig` | Registers reactive CORS configuration |

## Filter Order

| Order | Filter | Purpose |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE` | `GatewayExceptionGlobalFilter` | Handles exceptions from later filters and route forwarding |
| `-300` | `GatewayRequestGlobalFilter` | Creates and propagates request ID |
| `-250` | `GatewayRiskControlGlobalFilter` | Applies basic gateway risk-control checks |
| `-200` | `GatewayAuthorizationGlobalFilter` | Checks Sa-Token login state |
| `-175` | `GatewayRateLimitGlobalFilter` | Applies limits using trusted IP and Token Session tenant context |
| `-150` | `GatewaySameTokenGlobalFilter` | Adds Same-Token |
| `Ordered.LOWEST_PRECEDENCE` | `GatewayAccessLogGlobalFilter` | Writes final access logs |

Requests matching `peach.gateway.satoken.public-endpoints` skip risk control, authentication, and Same-Token injection.

## Logging Format

Gateway security logs use English parameterized messages and keep these field orders:

```text
requestId={}, method={}, path={}, status={}, reason={}
requestId={}, method={}, path={}, client={}, reason={}
requestId={}, method={}, path={}, status={}, durationMs={}, client={}
```

Logs must not include request body, query, token, password, full DTO data, or Redis password. Allowed operational fields are requestId, method, path, status, durationMs, client, and reason.

## Configuration Layout

Local profile files only bootstrap the runtime environment:

- `peach-gateway-launch/src/main/resources/application-dev.yml`
- `peach-gateway-launch/src/main/resources/application-prod.yml`
- `peach-gateway-launch/src/main/resources/application-docker.yml`

Gateway runtime configuration is centralized in Nacos:

- `deploy/nacos/config/peach-gateway.yml`
- `deploy-pipline/init/nacos/config/peach-gateway.yml`

See the [multi-algorithm rate limiting guide](docs/rate-limit.en-US.md) for configuration, algorithm selection, response headers, metrics, and failure handling. The guide also covers production OSS host allowlists, the Lua security contract, and recovery behavior. Load tests, manual Redis Cluster/Sentinel/TLS verification, Grafana dashboards, and Prometheus alerts are documented in [Rate-limit performance and operations verification](docs/performance/README.md).

The profile files import Nacos config through:

```yaml
spring:
  config:
    import:
      - optional:nacos:${spring.application.name}.${spring.cloud.nacos.config.file-extension}?group=${spring.cloud.nacos.config.group}&namespace=${spring.cloud.nacos.config.namespace}
```

## Routes

Business routes configured in `deploy/nacos/config/peach-gateway.yml`:

| Route ID | Path | Upstream | Filter |
| --- | --- | --- | --- |
| `peach-auth` | `/api/auth/**` | `lb://peach-auth` | `StripPrefix=1` |
| `peach-monitor` | `/api/monitor/**` | `lb://peach-monitor` | `StripPrefix=1` |
| `peach-fileservice` | `/api/file/**` | `lb://peach-fileservice` | `StripPrefix=1` |
| `peach-setting` | `/api/setting/**` | `lb://peach-setting` | `StripPrefix=1` |
| `peach-message` | `/api/message/**` | `lb://peach-message` | `StripPrefix=1` |
| `peach-message-ws` | `/webSocket/**` | `lb:ws://peach-message` | none |
| `peach-generator` | `/api/generator/**` | `lb://peach-generator` | `StripPrefix=1` |

Document routes:

| Route ID | Path | Upstream | Filter |
| --- | --- | --- | --- |
| `peach-auth-swagger` | `/api/auth/v3/api-docs` | `lb://peach-auth` | `SetPath=/v3/api-docs` |
| `peach-monitor-swagger` | `/api/monitor/v3/api-docs` | `lb://peach-monitor` | `SetPath=/v3/api-docs` |
| `peach-fileservice-swagger` | `/api/file/v3/api-docs` | `lb://peach-fileservice` | `SetPath=/v3/api-docs` |
| `peach-setting-swagger` | `/api/setting/v3/api-docs` | `lb://peach-setting` | `SetPath=/v3/api-docs` |
| `peach-message-swagger` | `/api/message/v3/api-docs` | `lb://peach-message` | `SetPath=/v3/api-docs` |
| `peach-generator-swagger` | `/api/generator/v3/api-docs` | `lb://peach-generator` | `SetPath=/v3/api-docs` |

## Properties

`peach.gateway.satoken` maps to `GatewaySaTokenProperties`.

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables gateway authentication and gateway Sa-Token customization |
| `inject-same-token` | `true` | Injects Same-Token for non-public downstream requests |
| `token-strategy-enabled` | `true` | Overrides Sa-Token token generation strategy |
| `log-path` | `true` | Logs public endpoint bypasses |
| `public-endpoints` | source defaults / Nacos override | Endpoints skipped by risk control, authentication, and Same-Token injection |

`peach.gateway.risk-control` maps to `GatewayRiskControlProperties`.

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `false` in code, `true` in Nacos sample | Enables the risk-control filter |
| `max-uri-length` | `2048` | Maximum raw URI length |
| `max-header-count` | `100` | Maximum number of request headers |
| `blocked-ips` | empty | Static client IP blocklist, comma-separated |
| `blocked-user-agents` | empty | Static User-Agent blocklist, comma-separated |

Redis dynamic IP blocklist uses this Redis Set:

```text
peach:gateway:risk-control:blocked-ip:
```

Gateway Redis connectivity uses `peach.redis.*` from `peach-redis.yml` and must point to the same Sa-Token Redis data as business services. The gateway process opens one Lettuce connection: rate limiting uses the reactive API, and Sa-Token reuses that connection through the blocking API on `boundedElastic`. The gateway does not load Jedis or Redisson; business services keep Jedis.

## Boundaries

- Gateway does not depend on `peach-satoken`, does not load business-side Servlet filters, and does not use business-side `SecurityContextHolder`.
- Gateway must keep Sa-Token Redis keys, `sa-token.token-name`, and Session serialization compatible with business services.
- Public endpoint rules are maintained by gateway config and are not automatically synchronized from business services.
- Redis dynamic blocklist lookup fails open and logs an English WARN message.
- Rate limiting is disabled by default. When enabled, Redis failures default to FAIL_CLOSED (503); Nacos can switch to FAIL_OPEN.
- Tenant limits read the tenant ID only from the current Sa-Token Token Session, never from request headers or a user-wide shared cache. Authentication issues a distinct token per login to prevent cross-device context overwrites.
- Rate-limit allowlists bypass quotas and denylists reject requests. Existing gateway access allowlists and risk-control denylists keep their earlier filter order.
- When rate limiting is dynamically enabled, quota checks stay unavailable until all six Lua scripts load. failure-mode then applies: FAIL_CLOSED returns 503 and FAIL_OPEN forwards the request. IP and tenant allowlists and denylists still run. Public login endpoints skip tenant rules, so a missing token is not turned into 401.
- Same-Token is injected only for non-public endpoints. Unauthenticated public requests do not depend on service credentials.

## Verification

Verify gateway and authentication service:

```bash
mvn -pl peach-gateway/peach-gateway-core,peach-auth/peach-auth-service -am verify -Pdevelopment
```

Redis integration tests use Testcontainers to start a real Redis instance; Docker is required locally.

Check encoding and diff:

```bash
node scripts/check-utf8.mjs
git diff --check -- peach-gateway
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| `401` response | Token header, Sa-Token Redis data, public endpoint config | Verify login token creation, then check `peach.gateway.satoken.public-endpoints` |
| `403` response | Permission error, Same-Token error, or risk-control rejection | Search logs for `Gateway authorization rejected`, `Gateway same-token relay failed`, or `Gateway risk-control rejected request` |
| `404` response | Route path, `StripPrefix`, downstream controller path | Check `spring.cloud.gateway.routes` in Nacos and downstream mappings |
| Swagger cannot open | Gateway document route and downstream `/v3/api-docs` | Test downstream docs first, then check gateway `SetPath` |
| WebSocket fails | `lb:ws://peach-message`, `/webSocket/**`, service registration | Check browser URL, Nacos service name, and message endpoint |
| Risk control does not work | `peach.gateway.risk-control.enabled` and public endpoint match | Ensure Nacos enables risk control and the path is not public |
| `429` response | Matched rule, algorithm, quota, and `Retry-After` | Check `peach.gateway.rate-limit.rules` scope, paths, methods, priority, and algorithm parameters |
| `503` response | Redis connectivity, Lua loading, and `failure-mode` | Check `peach.redis.*`, the Lettuce reactive connection, OSS SHA-256 values, and whether FAIL_CLOSED is expected |
| Tenant limit returns `401` | Whether the current token's Token Session contains `tenantId` | Sign in again or call the tenant-switch endpoint to refresh Token Session; do not send tenant ID in a request header |


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
