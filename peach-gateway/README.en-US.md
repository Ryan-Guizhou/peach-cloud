# peach-gateway

English | [中文](README.md)

Last updated: 2026-08-09

`peach-gateway` is the unified traffic entrypoint for Peach Cloud. It is based on Spring Cloud Gateway and provides routing, service discovery, gateway-side Sa-Token authentication, Same-Token relay, basic risk control, request ID propagation, access logs, CORS, Knife4j aggregation, and safe unified error responses.

## Scope

This module is responsible for traffic entering backend services before it reaches business modules.

It provides:

- HTTP and WebSocket routing through Spring Cloud Gateway.
- Nacos service discovery and Nacos config import.
- Gateway-side Sa-Token login-state verification.
- Gateway-local Sa-Token Redis DAO and Jackson session serialization, without depending on `peach-satoken-core`.
- Same-Token injection for downstream service-to-service authentication.
- Basic risk control for URI length, header count, unsupported methods, static blocklists, and Redis dynamic IP blocklist.
- Safe gateway exception mapping that avoids returning raw exception details.
- Request ID propagation and access logs without query, body, token, or full DTO data.
- Knife4j/OpenAPI document aggregation.

It does not provide:

- User, role, menu, or permission data management. That belongs to `peach-auth`.
- Business-service `SecurityContextHolder` restoration. Business services should use the aggregate `peach-satoken` module and rebuild context from `StpUtil.getLoginId()` plus cache data.
- Full WAF, DDoS protection, gray release, or full production gateway governance.
- Protection for traffic that bypasses the gateway and calls business services directly.

## Modules

| Module | Responsibility |
| --- | --- |
| `peach-gateway-core` | Gateway filters, security matcher, Sa-Token Redis DAO, config properties, and shared support code |
| `peach-gateway-launch` | Spring Boot launch module, profile bootstrap config, and logback config |

## Key Classes

| Capability | Class / Config | Notes |
| --- | --- | --- |
| Request ID | `GatewayRequestGlobalFilter` | Writes `X-Request-Id` to downstream requests and responses |
| Risk control | `GatewayRiskControlGlobalFilter`, `GatewayRiskControlProperties` | Rejects abnormal request shape and explicit blocklist hits |
| Authentication | `GatewayAuthorizationGlobalFilter`, `GatewaySaTokenProperties` | Runs `StpUtil.checkLogin()` for non-public endpoints |
| Same-Token | `GatewaySameTokenGlobalFilter` | Injects Sa-Token Same-Token into downstream requests |
| Unified errors | `GatewayExceptionGlobalFilter` | Maps auth, permission, routing, 4xx, and 5xx errors to safe JSON |
| Access logs | `GatewayAccessLogGlobalFilter` | Logs method, path, status, duration, requestId, and client only |
| Public endpoints | `GatewaySecurityEndpointMatcher`, `GatewaySecurityEndpointRule` | Gateway-local public endpoint matching |
| Sa-Token storage | `PeachSaTokenDao`, `PeachSaSessionForJacksonCustomized` | Shared Sa-Token Redis/session implementation from `peach-satoken-autoconfigure` |
| CORS | `GatewayCorsConfig` | Registers reactive CORS configuration |

Filter order:

| Order | Filter | Purpose |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE` | `GatewayExceptionGlobalFilter` | Handles exceptions from later filters and route forwarding |
| `-300` | `GatewayRequestGlobalFilter` | Creates and propagates request ID |
| `-250` | `GatewayRiskControlGlobalFilter` | Applies basic gateway risk-control checks |
| `-200` | `GatewayAuthorizationGlobalFilter` | Checks Sa-Token login state |
| `-150` | `GatewaySameTokenGlobalFilter` | Adds Same-Token for downstream calls |
| `Ordered.LOWEST_PRECEDENCE` | `GatewayAccessLogGlobalFilter` | Writes final access logs |

## Configuration Layout

Local profile files only bootstrap the runtime environment:

- `peach-gateway-launch/src/main/resources/application-dev.yml`
- `peach-gateway-launch/src/main/resources/application-prod.yml`
- `peach-gateway-launch/src/main/resources/application-docker.yml`

Gateway runtime configuration is centralized in Nacos:

- `deploy/nacos/config/peach-gateway.yml`

The profile files import `peach-gateway.yml` through:

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

## Gateway Properties

`peach.gateway.satoken` maps to `GatewaySaTokenProperties`.

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `true` | Enables gateway authentication and gateway Sa-Token customization |
| `inject-same-token` | `true` | Injects Same-Token into downstream requests |
| `token-strategy-enabled` | `true` | Overrides Sa-Token token generation strategy |
| `log-path` | `true` | Logs skipped public endpoints |
| `public-endpoints` | source defaults | Public endpoints skipped by authentication and risk control |

`peach.gateway.risk-control` maps to `GatewayRiskControlProperties`.

| Property | Default | Description |
| --- | --- | --- |
| `enabled` | `false` in code, `true` in Nacos config | Enables the risk-control filter |
| `max-uri-length` | `2048` | Maximum raw URI length |
| `max-header-count` | `100` | Maximum number of request headers |
| `blocked-ips` | empty | Static client IP blocklist, comma-separated |
| `blocked-user-agents` | empty | Static User-Agent blocklist, comma-separated |

Redis dynamic IP blocklist uses this Redis Set key:

```text
peach:gateway:risk-control:blocked-ip:
```

Gateway Redis connectivity uses Spring Boot native Redis configuration. It is consumed by shared `PeachSaTokenDao` and the dynamic risk-control blocklist:

| Property | Description |
| --- | --- |
| `spring.redis.host` | Redis host |
| `spring.redis.port` | Redis port |
| `spring.redis.password` | Redis password, preferably injected through environment variables |
| `spring.redis.database` | Redis database |
| `spring.redis.timeout` | Redis command timeout |
| `spring.redis.lettuce.pool.*` | Lettuce connection pool settings |

## Boundaries

- Gateway Sa-Token is independent from business-side `peach-satoken-core`.
- Gateway does not use business-side `SecurityContextHolder`.
- Gateway does not depend on `peach-redis-common`.
- Gateway logs are written in English and must not include request body, token, password, or full DTO content.
- Public endpoint rules are maintained in gateway config and are not automatically synchronized from business services.
- Redis blocklist lookup fails open and logs an English WARN message.

## Verification

Compile gateway:

```bash
mvn -f peach-gateway\pom.xml -pl peach-gateway-launch -am clean compile -Pdevelopment
```

Check encoding and diff:

```bash
node scripts\check-utf8.mjs
git diff --check -- peach-gateway deploy/nacos/config/peach-gateway.yml
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| `401` response | Token presence, Redis Sa-Token data, public endpoint config | Verify login token creation, then check `peach.gateway.satoken.public-endpoints` |
| `403` response | Permission error, Same-Token error, or risk-control rejection | Search logs for `Gateway authorization rejected` or `Gateway risk-control rejected request` |
| `404` response | Route path, `StripPrefix`, downstream controller path | Check `spring.cloud.gateway.routes` in Nacos and downstream mappings |
| Swagger cannot open | Gateway document route and downstream `/v3/api-docs` | Test downstream docs first, then check gateway `SetPath` |
| WebSocket fails | `lb:ws://peach-message`, `/webSocket/**`, service registration | Check browser URL, Nacos service name, and message endpoint |
| Risk control does not work | `peach.gateway.risk-control.enabled` and public endpoint match | Ensure the Nacos config enables risk control and the path is not public |
