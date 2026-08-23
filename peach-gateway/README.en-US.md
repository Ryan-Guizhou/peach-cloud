# peach-gateway

English | [中文](README.md)

Last updated: 2026-08-12

`peach-gateway` is the unified traffic entry point for Peach Cloud. It is based on Spring Cloud Gateway and provides routing, service discovery, gateway-side Sa-Token login checks, Same-Token injection, basic risk control, request ID propagation, access logs, CORS, Knife4j aggregation, and safe unified error responses.

## Scope

This module handles external HTTP/WebSocket traffic before it reaches backend business services.

It provides:

- HTTP and WebSocket routing through Spring Cloud Gateway.
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

Gateway Redis connectivity uses `peach.redis.*` from `peach-redis.yml` and must point to the same Sa-Token Redis data as business services.

## Boundaries

- Gateway does not depend on `peach-satoken`, does not load business-side Servlet filters, and does not use business-side `SecurityContextHolder`.
- Gateway must keep Sa-Token Redis keys, `sa-token.token-name`, and Session serialization compatible with business services.
- Public endpoint rules are maintained by gateway config and are not automatically synchronized from business services.
- Redis dynamic blocklist lookup fails open and logs an English WARN message.
- Same-Token is injected only for non-public endpoints. Unauthenticated public requests do not depend on service credentials.

## Verification

Compile gateway:

```bash
mvn -pl peach-gateway/peach-gateway-core,peach-gateway/peach-gateway-launch -am -DskipTests compile -Pdevelopment
```

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


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
