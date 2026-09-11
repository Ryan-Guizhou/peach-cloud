# peach-mongo

English | [中文](README.md)

`peach-mongo` provides MongoDB auto-configuration and a shared access entry point. Business modules integrate through `peach-mongo-starter`.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo configuration, auto-configuration and shared service contracts |
| `peach-mongo-starter` | Business integration dependency entry point |
| `peach-mongo-quickstart` | Minimal connection and auto-configuration verification |

```xml
<dependency><groupId>com.peach</groupId><artifactId>peach-mongo-starter</artifactId></dependency>
```

## QuickStart

The example lives in [`peach-mongo-quickstart`](./peach-mongo-quickstart/) and verifies minimal starter bootstrap and auto-configuration. MongoDB URIs and credentials are supplied only through local configuration or environment variables.

```bash
mvn -f peach-middleware/peach-mongo/peach-mongo-quickstart/pom.xml spring-boot:run -Pdevelopment
```

## Boundaries

- The starter does not deploy MongoDB.
- Mongo URI and credentials must come from secure configuration sources, never README or source code.
- Generic access APIs do not replace business aggregation queries, indexing or data-lifecycle design.
- Document-schema evolution requires explicit old-data compatibility and migration planning.
