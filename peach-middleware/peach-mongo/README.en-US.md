# peach-mongo

English | [中文](README.md)

`peach-mongo` provides MongoDB auto-configuration and a shared access entry point. Business modules integrate through `peach-mongo-starter`. The public API is the auto-configured `IMongoService` (`MongoTemplate` wrapper).

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo configuration, auto-configuration and the `IMongoService` contract |
| `peach-mongo-starter` | Business integration dependency entry point |
| `peach-mongo-quickstart` | Minimal capability verification using an audit-log scenario |

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-mongo-starter</artifactId>
</dependency>
```

Minimum configuration: `peach.mongo.uri`. Set `peach.mongo.database` when the URI does not include a database name. Transactions are off by default (`peach.mongo.transaction.enabled=false`) and require a replica set or sharded cluster.

## Quick Start

The example lives in [`peach-mongo-quickstart`](./peach-mongo-quickstart/). It has no REST port (`spring.main.web-application-type=none`) and demonstrates `IMongoService` with an audit-log scenario.

| Item | Detail |
| --- | --- |
| Capability samples | **Write / query**: `insertOne` / `findList`; **Update**: `updateOne` + `$set`; **Page / delete**: `insertMany` / `findPage` / `count` / `deleteOne` |
| Runner | `MongoDemoRunner`; disable the demo with `quickstart.mongo.demo.enabled=false` |
| Tests | Testcontainers `mongo:7.0`; skipped when Docker is absent (`disabledWithoutDocker=true`) |
| Prerequisites | Local Mongo or `PEACH_MONGO_URI` |

```bash
mvn -pl peach-middleware/peach-mongo/peach-mongo-quickstart -am spring-boot:run
mvn -pl peach-middleware/peach-mongo/peach-mongo-quickstart -am test
```

MongoDB URIs and credentials are supplied only through local configuration or environment variables.

## Boundaries

- The starter does not deploy MongoDB.
- Mongo URI and credentials must come from secure configuration sources, never README or source code.
- Generic access APIs do not replace business aggregation queries, indexing or data-lifecycle design.
- Update documents for `updateOne` / `updateMany` must use operators such as `$set`.
- Document-schema evolution requires explicit old-data compatibility and migration planning.
