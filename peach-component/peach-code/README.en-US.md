# peach-code

English | [中文](README.md)

## Purpose

`peach-code` generates tenant-scoped business codes such as `MENU_00000001` and `NOTICE_00000001`. The sequence scope is `(TENANT_ID, CODE_PREFIX)`, so two tenants may independently receive the same numeric suffix.

The default implementation follows the referenced `BizIdGenerator` pattern: Redis is the fast primary
allocator and MySQL is the durable fallback. Redis Lua atomically calibrates and increments the counter.
When Redis is unavailable, MySQL uses an atomic `LAST_INSERT_ID` update and the recovered Redis counter
is advanced monotonically. Redis can be disabled to use the MySQL fallback mode.

This component does not add `TENANT_ID` to business tables and does not derive sequences from `MAX(code)`.

## Modules

| Module | Responsibility |
| --- | --- |
| `peach-code-autoconfigure` | `CodeGenerator`, JDBC implementation, configuration, and auto-configuration |
| `peach-code-starter` | Recommended dependency for business modules |
| `peach-code-example` | Runnable MySQL and Redis startup-event example |

## Database setup

Execute `sql/PEACH_CODE_RULE.sql`, then add one rule for each tenant and prefix:

```sql
INSERT INTO PEACH_CODE_RULE
    (TENANT_ID, CODE_PREFIX, MAX_CODE_WIDTH, CURRENT_VALUE, STATUS)
VALUES
    ('T001', 'NOTICE', 8, 0, 'ENABLE');
```

`MAX_CODE_WIDTH` is the maximum length of the numeric part. Values shorter than the configured width are left-padded with zeroes. Values that exceed it are rejected.

## Usage

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-code-starter</artifactId>
</dependency>
```

```java
@Transactional(rollbackFor = Exception.class)
public void saveNotice(String tenantId, NoticeDO notice) {
    notice.setNoticeCode(codeGenerator.next(tenantId, "NOTICE"));
    noticeDao.insert(notice);
}
```

The generator requires an active transaction. The sequence update and the business insert must be in the same transaction.

## Configuration

| Property | Default | Description |
| --- | --- | --- |
| `peach.code.enabled` | `true` | Enables auto-configuration |
| `peach.code.redis-enabled` | `true` | Enables Redis-primary allocation and MySQL fallback |
| `peach.code.redis-key-prefix` | `peach:code:committed:` | Redis sequence key prefix |

## Runtime and failure semantics

```text
business call
  -> read the PEACH_CODE_RULE value and width
  -> Redis Lua calibrates and increments atomically
  -> format the code using MAX_CODE_WIDTH
  -> insert the business row

Redis unavailable
  -> MySQL atomically increments with LAST_INSERT_ID(CURRENT_VALUE + 1)
  -> commit the independent fallback transaction
  -> monotonically write the value back to Redis
```

- Redis-primary allocation is independent from the business transaction, so a business rollback does not
  reclaim the Redis number and may create a gap.
- If strict rollback-without-gap behavior is required, disable `peach.code.redis-enabled` and generate the
  code inside the same MySQL transaction as the business insert.
- Deleted business rows do not release codes for reuse.
- Redis loss is recovered from the MySQL rule value; Redis outages fall back to MySQL and recovery advances
  Redis monotonically.
- A retry must retry the complete transactional service method, not only `CodeGenerator.next`.
- Database repair or manual sequence changes can still create gaps; uniqueness is prioritized over continuity.

## Example and verification

The example uses the configured MySQL and Redis services. After the application is ready,
`PeachCodeEvent` generates MENU and NOTICE codes through `ExampleCodeService` and logs the format checks.

```bash
mvn -f peach-component/peach-code/peach-code-example/pom.xml spring-boot:run
```

Before starting the example, configure MySQL, ensure `PEACH_CODE_RULE` exists, and configure the Redis
password. The default `peach.code.example.tenant-id` is `T001`; set
`peach.code.example.verify-on-startup=false` to disable startup verification. The example does not create
production database resources.

## Boundaries

- The current implementation does not modify `PEACH_MENU` or `PEACH_NOTICE` to add tenant columns.
- Business tables should later add `TENANT_ID` and a composite unique key with their code column.
- The default JDBC implementation is intentionally small and does not contain domain-specific retry orchestration.
- Secrets, credentials, complete SQL payloads, and full Redis payloads must not be logged.
