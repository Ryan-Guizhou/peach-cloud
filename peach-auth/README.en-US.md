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


## Scheduler integration demo

Launch the existing application entry point in IDEA with the normal `dev` (or `docker`) profile. Local application YAML retains startup and Nacos imports; Nacos manages scheduler settings, the managed `scheduler` virtual-thread group, RocketMQ, Outbox and task defaults. The service registers `authSchedulerDemo`, sends heartbeats and consumes execution commands through the managed executor and Claim gate. The demo returns success without changing business data. Set `PEACH_SCHEDULER_DEMO_ENABLED=false` to disable the demo Handler and consumer.

Task registration follows service startup automatically; inspect its definition and initial state in task management. Services and the control plane require the same reachable Nacos, Redis, MySQL Outbox and RocketMQ environment. Verify execution against live services.


## Automatic task registration

Services declare stable `jobCode` and `jobName` in `@PeachJob` and supply complete defaults through Nacos `peach.scheduler.executor.jobs[handlerName]`: schedule, time zone, misfire and concurrency policies, timeout, retries, parameters and initial state. The control plane validates the definition on first registration and creates the task. Missing metadata permits Handler registration and heartbeats only, without creating a placeholder draft. Later heartbeats preserve schedules, parameters and lifecycle or deleted states maintained by administrators. Additional tasks may use the same Handler. Initial state is managed centrally.

## Tenant management

The `/tenant` page uses `/auth/tenant` APIs for pagination, details, creation, editing, status changes and logical deletion. It uses existing administrator, menu, API and button grants without introducing a platform role.

Lifecycle: `PENDING → ACTIVE ↔ SUSPENDED → CLOSED`; pending tenants can also close directly. Only pending or closed tenants may be logically deleted. The current login tenant cannot be suspended, closed or deleted. Updates, transitions and deletion check the version. Organization/account provisioning is outside this feature.

Back up existing databases before applying the [tenant migration](../sql/migrations/20261121_tenant_management.sql). It preserves tenant identifiers and user/organization bindings, maps legacy `ENABLE` to `ACTIVE` and retains legacy columns for rollback. MySQL DDL cannot be transactionally rolled back. Unavailable tenants reject new logins/context switches and revoke matching sessions after commit. Administrators should sign in again to load the new menu/resource grants.

## Function Definition maintenance

`POST /auth/function/list` returns the authenticated tenant's complete function list, ordered by sequence and code, for tree construction. `/auth/resource/pageList` supports function code, resource type and a code/name `keyword` filter. IDs may be omitted when creating functions or resources; the server generates them.

Function codes are immutable after creation. Parents must belong to the same tenant, without cycles. Function deletion checks children, resources, menus, routes and authorization references. Authorized resources cannot be deleted or have their code/type changed. Function and resource maintenance uses transactions and function row locks; other modules writing related tables must still respect reference constraints.

Existing environments must run the [migration SQL](../sql/migrations/20261122_function_definition.sql) and sign in again. `FunctionDefinitionTest` covers service rules. With `PEACH_FUNCTION_DB_SMOKE=true`, `FunctionDefinitionDatabaseTest` reads datasource configuration from Nacos in memory, verifies the workflow against MySQL, and rolls back all test writes.

## Login audit and function categories

Provides independent-transaction login auditing, tenant-isolated read-only queries and cleanup with a default90-day retention period. The function tree contains a function-list root, System Settings, Log Audit, Files and Storage, and Scheduling. See [Login audit](docs/login-audit.en-US.md) for upgrades, permissions and capture boundaries.

## Roles, routes and operation logs

The role workspace supports atomic function/resource grants, membership, default menus and calibration previews. Routes support combined filters. Operation logging maps tenant/organization and retries retained failed batches. See [administration workspace](docs/admin-workspace.en-US.md) for migration, activation and reliability boundaries.
