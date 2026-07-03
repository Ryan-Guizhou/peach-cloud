# peach-gateway

English | [中文](README.md)

## Purpose

`peach-gateway` is the unified traffic entrypoint. It is based on Spring Cloud Gateway and handles routing, gateway filters, and Sa-Token Gateway integration.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-gateway-core` | Gateway core configuration, filters, and extensions |
| `peach-gateway-launch` | Runtime application module |

## Key Entrypoints

- Application: `peach-gateway-launch/src/main/java/com/peach/gateway/launch/PeachGatewayApplication.java`
- Core package: `peach-gateway-core/src/main/java/com/peach/gateway/core`

## Boundaries

- Permission data is provided by `peach-auth`; the gateway does not own users, roles, or menus.
- Sa-Token integration is provided by `peach-satoken`.
- Routes and auth whitelists must be environment-specific and must not hard-code production endpoints.

## Verification

```bash
mvn -f "peach-gateway/pom.xml" -DskipTests package
```
