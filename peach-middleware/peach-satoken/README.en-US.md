# peach-satoken

English | [中文](README.md)

Last updated: 2026-08-12

`peach-satoken` is the business-service Sa-Token integration module for Peach Cloud. It targets Servlet business services and provides Sa-Token Redis DAO, Session serialization strategy, Same-Token verification, request ID filter, and current user context restoration.

Gateway does not depend on this module. Gateway keeps its own Reactor-side Sa-Token DAO, Session strategy, token strategy, and filters in `peach-gateway-core`.

## Scope

It provides:

- Business-service Sa-Token Redis DAO.
- Business-service Jackson-compatible Session serialization strategy.
- Business-service Same-Token verification.
- Servlet request ID filter using `X-Request-Id`.
- Servlet current user context restoration into `SecurityContextHolder`.
- Public endpoint rules based on method + path, shared by Same-Token and user context filters.

It does not provide:

- User, role, menu, or permission data management.
- Login endpoint, token issuing, or user context cache writes.
- Gateway Reactor runtime support.
- Full server-side governance for traffic that bypasses Gateway.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-satoken-autoconfigure` | Public APIs, Redis DAO, Session strategy, and Servlet auto-configuration |
| `peach-satoken-starter` | Business-service starter that aggregates autoconfigure, Sa-Token Web, and Redis |

## Quick Start

Add this dependency to Servlet business services:

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-satoken-starter</artifactId>
</dependency>
```

Runtime configuration is imported from:

```text
deploy/nacos/config/peach-satoken.yml
```

## Auto-configurations

The auto-configuration import file is:

```text
peach-satoken-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

| Auto-configuration | Responsibility |
| --- | --- |
| `PeachSaTokenDaoAutoConfiguration` | Registers business-service Sa-Token Redis DAO |
| `PeachSaTokenSessionStrategyAutoConfiguration` | Overrides business-service Sa-Token Session creation strategy |
| `PeachSaTokenWebAutoConfiguration` | Registers Same-Token interceptor, request ID filter, user context support, and user context filter |

## Core APIs

| API | Purpose |
| --- | --- |
| `SecurityContextHolder.get()` | Gets current request `UserContext` |
| `SecurityContextHolder.set(UserContext)` | Binds current request user context |
| `SecurityContextHolder.clear()` | Clears current thread user context |
| `UserContextSupport` | Loads current user cache from Redis Hash by loginId |
| `UserContextFilter` | Restores current user context for non-public endpoints |
| `RequestIdFilter` | Propagates or creates `X-Request-Id` |

## Redis User Context Contract

`UserContextSupport` only reads Redis. Authentication or business services must write data with this contract:

```text
key: peach:security:user:profile:{loginId}
type: Redis Hash
```

Hash fields are defined in `SatokenConstant`:

| Constant | Redis Hash field | Notes |
| --- | --- | --- |
| `USER_PROFILE_FIELD_USER_ID` | `userId` | User ID; must match Sa-Token loginId |
| `USER_PROFILE_FIELD_USER_CODE` | `userCode` | User code |
| `USER_PROFILE_FIELD_USER_NAME` | `userName` | User display name |
| `USER_PROFILE_FIELD_TENANT_ID` | `tenantId` | Current tenant ID |
| `USER_PROFILE_FIELD_TENANT_NAME` | `tenantName` | Current tenant name |
| `USER_PROFILE_FIELD_ORG_ID` | `orgId` | Current organization ID |
| `USER_PROFILE_FIELD_ORG_CODE` | `orgCode` | Current organization code |
| `USER_PROFILE_FIELD_ORG_NAME` | `orgName` | Current organization name |
| `USER_PROFILE_FIELD_FISCAL` | `fiscal` | Current fiscal period |
| `USER_PROFILE_FIELD_LANG` | `lang` | Language code; login write-side source is not finalized yet |
| `USER_PROFILE_FIELD_CONTEXT_VERSION` | `contextVersion` | User context version |

Do not put passwords, tokens, identity numbers, secrets, or full DTO data into `UserContext` or logs.

## Servlet Runtime Flow

1. Gateway creates `X-Request-Id`, checks login state for non-public endpoints, and injects Same-Token for authenticated non-public downstream calls.
2. Business-service `RequestIdFilter` reuses a valid request ID or creates a new one, then writes it to the response header.
3. `PeachSaTokenWebAutoConfiguration` registers the Same-Token MVC interceptor. Public endpoints are skipped; non-public endpoints call `SaSameUtil.checkCurrentRequestToken()`.
4. `UserContextFilter` allows unauthenticated public endpoints and uses `StpUtil.getLoginIdDefaultNull()` for non-public endpoints.
5. Empty loginId or missing Redis user context returns `401`; valid cache with matching userId is written to `SecurityContextHolder`.
6. `UserContextFilter` clears `SecurityContextHolder` after the request to avoid Servlet thread reuse contamination.

## Filter and Interceptor Order

| Order | Component | Purpose |
| --- | --- | --- |
| `Ordered.HIGHEST_PRECEDENCE + 20` | `RequestIdFilter` | Creates or propagates `X-Request-Id` |
| MVC interceptor | Same-Token `SaInterceptor` | Skips public endpoints and verifies Same-Token for non-public endpoints |
| `Ordered.HIGHEST_PRECEDENCE + 40` | `UserContextFilter` | Skips public endpoints and restores `UserContext` for non-public endpoints |

Same-Token and user context restoration share `peach.satoken.user-context.public-endpoints`.

## Properties

| Property | Default | Description |
| --- | --- | --- |
| `peach.satoken.dao.enabled` | `true` | Registers business-service Sa-Token Redis DAO |
| `peach.satoken.session-strategy.enabled` | `true` | Uses Jackson-compatible Session type |
| `peach.satoken.same-token.enabled` | `true` | Enables Servlet Same-Token verification |
| `peach.satoken.same-token.log-path` | `true`, `false` in Nacos sample | Logs Same-Token paths at DEBUG level |
| `peach.satoken.same-token.exclude-path-patterns` | `/error` | MVC interceptor excluded paths |
| `peach.satoken.request-id.enabled` | `true` | Enables request ID filter |
| `peach.satoken.request-id.header-name` | `X-Request-Id` | Header name shared with Gateway/OpenFeign |
| `peach.satoken.user-context.enabled` | `true` | Enables Redis user context restoration filter |
| `peach.satoken.user-context.public-endpoints` | source defaults / Nacos override | Public endpoints allowed without login and Same-Token |

## Logging Format

Business-side Sa-Token logs use English parameterized messages. Recommended field order:

```text
requestId={}, method={}, path={}, userId={}, reason={}
userId={}, method={}, path={}
```

Logs must not include request body, query, token, password, full DTO data, Redis password, or full Redis keys. For user context cache diagnostics, log userId and field names only.

## Boundaries

- All business services and Gateway must connect to the same Sa-Token Redis data and keep `sa-token.token-name` consistent.
- Gateway does not depend on `peach-satoken`, does not load Servlet filters, and does not use business-side `SecurityContextHolder`.
- `UserContextSupport` only reads agreed Redis keys. It does not write, refresh TTL, or delete user context cache.
- Cache update strategy after profile changes or tenant/organization switch belongs to authentication or business services.
- The `lang` field is only part of the read contract for now. Its write-side source must be finalized before login writes it.

## Verification

```bash
mvn -pl peach-middleware/peach-satoken/peach-satoken-autoconfigure -am -DskipTests compile -Pdevelopment
node scripts/check-utf8.mjs
git diff --check -- peach-middleware/peach-satoken
```

## Troubleshooting

| Symptom | Check | Action |
| --- | --- | --- |
| First business request after login returns `401` | Whether `peach:security:user:profile:{loginId}` exists | Ensure authentication service writes the agreed Redis Hash and services connect to the same Redis DB |
| Gateway treats request as unauthenticated | Token header, Sa-Token Redis, `sa-token.token-name` | Ensure login response token matches client request header |
| Downstream Same-Token error | Gateway injection switch, business Same-Token config, public endpoint config | Check `peach.gateway.satoken.inject-same-token` and `peach.satoken.same-token.enabled` |
| Downstream has no requestId | Request header name | Use `X-Request-Id` consistently |
| `UserContextSupport` is not registered | Redis auto-config and starter dependency | Check whether `StringRedisTemplate` is created; avoid importing autoconfigure alone |
| Public API is blocked by Same-Token | `peach.satoken.user-context.public-endpoints` | Ensure the real Servlet path after Gateway `StripPrefix` is configured |


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
