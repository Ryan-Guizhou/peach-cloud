# peach-code

English | [中文](README.md)

`peach-code` generates tenant-scoped business codes such as `MENU_00000001` and `NOTICE_00000001`. The sequence scope is `(TENANT_ID, CODE_PREFIX)`, so two tenants may independently receive the same numeric suffix.

The default implementation uses Redis as the fast primary allocator and MySQL as the durable fallback. Redis Lua atomically calibrates and increments the counter. When Redis is unavailable, MySQL uses an atomic `LAST_INSERT_ID` update and the recovered Redis counter is advanced monotonically. Redis can be disabled with `peach.code.redis-enabled=false` to use the MySQL-only mode.

This component does not add `TENANT_ID` to business tables and does not derive sequences from `MAX(code)`.

## Modules

| Module | Responsibility |
| --- | --- |
| `peach-code-autoconfigure` | `CodeGenerator`, `PeachCodeGenerator`, properties, and auto-configuration |
| `peach-code-starter` | Recommended dependency for business modules |
| `peach-code-quickstart` | Runnable in-memory demo of the public `CodeGenerator` API |

## Quick Start

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

Execute [`sql/PEACH_CODE_RULE.sql`](../../sql/PEACH_CODE_RULE.sql), then add one rule for each tenant and prefix. The table includes `ORG_ID`; the generator reads rules by `TENANT_ID + CODE_PREFIX`:

```sql
INSERT INTO PEACH_CODE_RULE
    (TENANT_ID, ORG_ID, CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS)
VALUES
    ('T001', 'ORG001', 'NOTICE', 8, 0, 'ENABLE');
```

`MAX_CODE_WIDTH` is the maximum length of the numeric part. Values shorter than the configured width are left-padded with zeroes. Values that exceed it are rejected.

Inject the public `CodeGenerator` API:

```java
public void saveNotice(String tenantId, NoticeDO notice) {
    notice.setNoticeCode(codeGenerator.next(tenantId, "NOTICE"));
    noticeDao.insert(notice);
}
```

`CodeGenerator.next` does not require an active business transaction. A Redis-allocated number is not reclaimed when the business transaction rolls back. If gaps on rollback are unacceptable, disable `peach.code.redis-enabled` and generate the code in the same MySQL transaction as the business insert.

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `peach.code.enabled` | `true` | Enables auto-configuration |
| `peach.code.redis-enabled` | `true` | Enables Redis-primary allocation and MySQL fallback |
| `peach.code.redis-key-prefix` | `peach:code:generate:` | Redis sequence key prefix |

Redis keys also append the tenant and the upper-cased prefix, for example `peach:code:generate:T001:NOTICE`. Complete business payloads are not written to Redis or logs.

## Runtime and failure semantics

```text
business call CodeGenerator.next
  -> read the PEACH_CODE_RULE value and width
  -> Redis Lua calibrates and increments atomically
  -> format the code using MAX_CODE_WIDTH and return

Redis unavailable
  -> MySQL atomically increments with LAST_INSERT_ID(CURRENT_VALUE + 1)
  -> commit the independent fallback transaction
  -> monotonically write the value back to Redis
```

- Redis-primary allocation is independent from the business transaction, so a business rollback does not reclaim the Redis number and may create a gap.
- If strict rollback-without-gap behavior is required, disable `peach.code.redis-enabled` and generate the code inside the same MySQL transaction as the business insert.
- Deleted business rows do not release codes for reuse.
- Redis loss is recovered from the MySQL rule value; Redis outages fall back to MySQL and recovery advances Redis monotonically.
- A retry must retry the complete transactional service method, not only `CodeGenerator.next`.
- Database repair or manual sequence changes can still create gaps; uniqueness is prioritized over continuity.

## Quickstart and verification

- Module: [`peach-code-quickstart`](./peach-code-quickstart/)
- Default in-memory mode: `peach.code.redis-enabled=false`, DataSource/Redis auto-configuration excluded, injects public `CodeGenerator`
- Capability samples:
  - **Sequential allocation**: two `next` calls for the same tenant + `MENU` yield `MENU_00000001` then `MENU_00000002`
  - **Isolation**: `T001/MENU`, `T001/NOTICE`, and `T002/MENU` keep independent sequences
  - **Invalid input**: unsupported tenant or prefix characters throw `CodeGeneratorException`
- `CodeDemoRunner` runs on startup; disable with `quickstart.code.demo.enabled=false`
- Run: `mvn -f peach-component/peach-code/peach-code-quickstart/pom.xml spring-boot:run`
- Test: `mvn -f peach-component/peach-code/peach-code-quickstart/pom.xml test`

Production use still needs `PEACH_CODE_RULE`, a datasource, and optional Redis (`peach.code.redis-enabled=true`).

## Boundaries

- The current implementation does not modify `PEACH_MENU` or `PEACH_NOTICE` to add tenant columns.
- Business tables should later add `TENANT_ID` and a composite unique key with their code column.
- Auto-configuration creates `PeachCodeGenerator` only when no `CodeGenerator` bean exists.
- Secrets, credentials, complete SQL payloads, and full Redis payloads must not be logged.

## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as target/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
