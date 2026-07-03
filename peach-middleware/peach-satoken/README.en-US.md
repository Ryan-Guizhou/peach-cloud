# peach-satoken

English | [中文](README.md)

## Purpose

`peach-satoken` adapts Sa-Token for Peach. It provides core session DAO support, Servlet Web interceptor integration, and Spring Cloud Gateway filter integration.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-satoken-core` | Sa-Token DAO, session serialization strategy, and shared properties |
| `peach-satoken-web-autoconfigure` / `starter` | Servlet Web auto-configuration |
| `peach-satoken-gateway-autoconfigure` / `starter` | Spring Cloud Gateway auto-configuration |

## Core Objects

- `PeachSaTokenProperties`: binds `peach.satoken.*`.
- `PeachSaTokenDaoAutoConfiguration`: Sa-Token DAO auto-configuration.
- `PeachSaTokenSessionStrategyAutoConfiguration`: session serialization strategy.
- `PeachSaTokenWebAutoConfiguration`: Web interceptor integration.
- `PeachSaTokenGatewayAutoConfiguration`: Gateway filter integration.

## Boundaries

- Choose Web or Gateway starter according to runtime type; do not import both blindly.
- Login and permission data are still owned by `peach-auth`.
- Redis-backed session persistence depends on project Redis configuration.

## Verification

```bash
mvn -f "peach-middleware/peach-satoken/pom.xml" -DskipTests package
```
