# peach-code

English | [中文](README.md)

`peach-code` allocates business codes within each `TENANT_ID + CODE_PREFIX` scope. **MySQL is the sole sequence authority**; Redis is no longer an alternative allocator.

## Modules and integration

| Module | Capability |
| --- | --- |
| `peach-code-autoconfigure` | `CodeGenerator` API, MySQL transaction allocation, Spring auto-configuration |
| `peach-code-starter` | Business integration dependency |
| `peach-code-quickstart` | Database-free in-memory demonstration of the public API only |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

**Startup dependency contract:** the allocator no longer carries the old Redis constructor argument or a Spring Data Redis compile dependency. The default `CodeGenerator` is auto-configured only when `JdbcTemplate` is available, and allocation requires a transaction manager. Applications without JDBC are not failed by this auto-configuration, while applications injecting `CodeGenerator` must provide a working JDBC/transaction setup.



Create the rule table with [sql/PEACH_CODE_RULE.sql](../../sql/PEACH_CODE_RULE.sql); each tenant and prefix must identify one rule. Call `CodeGenerator.next(tenantId, prefix)` to obtain a code such as `MENU_00000001`.

## Allocation and failure contract

```text
CodeGenerator.next
  -> MySQL REQUIRES_NEW transaction
  -> SELECT ... FOR UPDATE by TENANT_ID + CODE_PREFIX
  -> validate ENABLE, width and overflow
  -> conditional UPDATE CURRENT_VALUE
  -> return business code only after COMMIT
```

- MySQL is the sole authority. Redis outages and recovery cannot switch the allocator or roll back the sequence.
- InnoDB row locks serialize allocation within one tenant/prefix; independent scopes can run concurrently.
- Database failure throws an exception; there is no Redis/in-memory fallback. A committed allocation followed by a caller failure can leave a gap.
- The allocation transaction commits independently of the caller's business transaction. Business table unique constraints remain necessary.
- `MAX_CODE_WIDTH` supports 1–18 digits. Overflow does not update the counter.
- `peach.code.enabled` defaults to `true`. The historical `peach.code.redis-enabled` and `peach.code.redis-key-prefix` properties are still accepted but **have no effect on allocation** and should be removed from future deployment configs.

## Mandatory migration from Redis-primary allocation

**Do not run old Redis-primary allocators together with new MySQL-authoritative instances.**

1. Quiesce all code-generating traffic and wait for in-flight calls on old instances.
2. Back up `PEACH_CODE_RULE` and historical Redis counters; compare the highest issued Redis value with `CURRENT_VALUE` for each tenant/prefix.
3. Advance each MySQL counter to at least the maximum previously allocated Redis value. Never lower counters. Ensure no external writer is still allocating.
4. Detect duplicate rules before creating the unique constraint:
   ```sql
   SELECT TENANT_ID, CODE_PREFIX, COUNT(*) AS COUNT_ROWS
   FROM PEACH_CODE_RULE
   GROUP BY TENANT_ID, CODE_PREFIX HAVING COUNT(*) > 1;
   ```
5. Resolve duplicate rules and add `UNIQUE KEY UK_PEACH_CODE_TENANT_PREFIX (TENANT_ID, CODE_PREFIX)`. Fresh databases can use the supplied CREATE TABLE script.
6. Deploy all instances of the new implementation, then resume traffic. Do not restart old allocators without another full cutover and watermark reconciliation.

`ORG_ID` remains rule ownership metadata rather than a sequence isolation key. The public API does not accept `ORG_ID`; multiple organizations under one tenant/prefix must be resolved explicitly, never merged automatically.

**Real MySQL concurrency/fault validation:** `MysqlCodeConcurrentIntegrationTest` uses MySQL 8.4 Testcontainers to allocate 32 codes concurrently from two independently configured generator instances. It verifies no duplicate sequence values, injects a `BEFORE UPDATE` SQL failure, and ensures no Redis/in-memory fallback and a safe next allocation after recovery. Tests skip when Docker is unavailable. **Production rollout still requires historical Redis/MySQL counter reconciliation, duplicate-rule cleanup, unique-index verification and old-instance shutdown**; CI cannot validate customer data migration.

## Verification and limits

- Run: `mvn -pl peach-component/peach-code/peach-code-quickstart -am spring-boot:run`
- Test: `mvn -pl peach-component/peach-code/peach-code-quickstart -am test`
- The quickstart uses an in-process allocator; it does not validate production MySQL concurrency, outages, or migration. Verify same-key contention, rollback, database outage and overflow against a real MySQL instance before deployment.
- Business-table tenant constraints and unique keys are outside this module.

Project baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, Spring Cloud Alibaba 2025.0.0.0. Chinese Javadoc, English logs, UTF-8 without BOM.
