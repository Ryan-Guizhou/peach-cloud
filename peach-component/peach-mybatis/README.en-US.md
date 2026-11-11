# peach-mybatis

English | [中文](README.md)

`peach-mybatis` provides MyBatis `databaseId` detection, SQL logging, and slow-SQL warnings. A business launch module depends on `peach-mybatis-starter`. Each service still declares its own data source, transaction manager, and `@MapperScan`.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-mybatis-autoconfigure` | Database detection, SQL logging interceptor, PageHelper settings, and optional Micrometer metrics |
| `peach-mybatis-starter` | Business integration dependency |
| `peach-mybatis-quickstart` | Minimal startup check, not a business template |

```mermaid
flowchart LR
    App[Business Service] --> Starter[peach-mybatis-starter]
    Starter --> Auto[peach-mybatis-autoconfigure]
    Quick[peach-mybatis-quickstart] --> Starter
    Auto --> Dialect[PeachDatabaseIdProvider]
    Auto --> SqlLog[SqlCostInterceptor]
```

## Integration

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-mybatis-starter</artifactId>
</dependency>
```

When the application creates its own `SqlSessionFactoryBean`, a bean post-processor installs the `DatabaseIdProvider` and interceptors. The official MyBatis auto-configuration collects those beans directly.

A custom `logback-spring.xml` must include the SQL logger appender, otherwise `SQL_LOG` has no destination:

```xml
<include resource="logback/peach-mybatis.xml"/>
```

Do not declare `databaseIdProvider` or the paging and timing plugins again in `mybatis-config.xml`, or the same SQL is logged twice.

## Logging

By default, Preparing keeps `?`; Parameters contains a bounded summary of parameter count and Java types (at most 16 types), **not parameter values**. `peach.mybatis.log.executable=true` explicitly enables value-substituted SQL in DEBUG for troubleshooting and may expose sensitive values: do not enable it in production. Slow-query and missing-WHERE WARN logs always retain parameter placeholders.

Preparing and missing-WHERE SQL text is bounded at every log level (default 4000, hard maximum 16384). Non-positive configured limits fall back to the safe default; very large binary/text parameters skip executable rendering.

| Property | Default | Meaning |
| --- | --- | --- |
| `peach.mybatis.enabled` | `true` | Skip database detection and SQL logging when false |
| `peach.mybatis.long-query-time` | `3000` | Slow-SQL threshold in milliseconds |
| `peach.mybatis.jdbc-type-for-null` | `false` | Set the JDBC type of null parameters to NULL when true |
| `peach.mybatis.log.executable` | `false` | Emit executable SQL |
| `peach.mybatis.log.warn-missing-where` | `true` | Warn when UPDATE / DELETE has no WHERE after quotes and comments are removed |
| `peach.mybatis.log.max-sql-length` | `4000` | Maximum SQL text length for DEBUG/WARN; non-positive uses safe default; hard cap 16384 |
| `peach.mybatis.log.large-result-threshold` | `5000` | Warn when the row count exceeds this value; zero or negative disables it |
| `peach.mybatis.pagehelper.helper-dialect` | empty | PageHelper detects the dialect once from the current connection |
| `peach.mybatis.pagehelper.auto-runtime-dialect` | `false` | Detect the dialect on every page when true |

Default vendor mapping: MySQL, MariaDB, and DM use `mysql`; PostgreSQL and KingbaseES use `postgresql`; Oracle uses `oracle`; Microsoft SQL Server uses `sqlserver`; H2 uses `h2`. An unrecognized product name fails startup. `peach.mybatis.database-ids` overrides a default with the same vendor name, and the longer vendor name wins.

Executable SQL writes booleans as `true` / `false` and bytes as `X'...'`. Those literals apply to MySQL and PostgreSQL.

When a Micrometer `MeterRegistry` exists, the component records `peach.mybatis.sql` with the full mapper method id and a slow flag. PageHelper auto-configuration stays unloaded when PageHelper is absent from the classpath.

## Quick Start

The sample lives in [`peach-mybatis-quickstart`](./peach-mybatis-quickstart/). It checks that the starter starts and registers database detection plus SQL logging. It does not open a database connection.

```bash
mvn -pl peach-component/peach-mybatis/peach-mybatis-quickstart -am test
```

## Boundaries

- Statements are selected by `databaseId`. The component does not translate one SQL dialect into another.
- Data sources, transaction managers, and mapper scanning stay in the business service.
- `SqlCostInterceptor` wraps the pagination plugin and logs SQL from before pagination rewrites it.
