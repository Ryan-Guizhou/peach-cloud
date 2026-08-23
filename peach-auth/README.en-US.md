# peach-auth

English | [中文](README.md)

## Purpose

`peach-auth` is the authentication and authorization domain. It manages users, roles, menus, resources, login state, route permissions, and user operation logs. It provides REST APIs, domain services, entity models, external OpenFeign clients, and a launch module.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-auth-service` | Domain services for users, roles, menus, resources, permissions, and login |
| `peach-auth-rest` | REST APIs for login, users, roles, resources, menus, and routes |
| `peach-auth-launch` | Runtime application module |
| `peach-auth-common` | Shared auth-domain objects and utilities |
| `peach-auth-entity` | DO/DTO/QO/VO models |
| `peach-auth-external/peach-auth-openfeign-external` | OpenFeign clients for other services |
| `peach-auth-external/peach-auth-log-external` | `@UserOperLog` operation-log capability |

## Main Capabilities

- User, role, menu, and resource management.
- Login APIs and route permission lookup.
- Separation between internal REST APIs and external OpenFeign APIs.
- Operation-log annotation and log service.
- MyBatis DAO based persistence.

## Key Entrypoints

- Application: `peach-auth-launch/src/main/java/com/peach/auth/launch/PeachAuthServiceApplication.java`
- REST package: `peach-auth-rest/src/main/java/com/peach/auth/rest`
- Service interfaces: `peach-auth-service/src/main/java/com/peach/auth/service`
- Feign clients: `peach-auth-external/peach-auth-openfeign-external/src/main/java/com/peach/auth/openfeign`

## Verification

```bash
mvn -f "peach-auth/pom.xml" -DskipTests package
```

## Boundaries

- Gateway authentication filters are handled by `peach-gateway` and `peach-satoken`.
- Permission cache and session persistence depend on external Redis/Sa-Token configuration.
- Production environments must define password, token, audit, and rate-limit policies explicitly.


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
