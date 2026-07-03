# peach-mongo

English | [中文](README.md)

## Purpose

`peach-mongo` is a MongoDB starter that configures `MongoClient`, `MongoTemplate`, transaction manager, and a generic `IMongoService<T>` operation entrypoint.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-mongo-autoconfigure` | Mongo auto-configuration, properties, and generic service |
| `peach-mongo-starter` | Starter exposed to business modules |

## Core Objects

- `PeachMongoProperties`: binds `peach.mongo.*`.
- `MongoAutoConfigure`: creates MongoDB related beans.
- `IMongoService<T>` / `MongoService<T>`: generic Mongo operation service.

## Configuration Example

```yaml
peach:
  mongo:
    uri: mongodb://localhost:27017
    database: peach
    pool:
      max-size: 100
      min-size: 5
    socket:
      connect-timeout-ms: 10000
      read-timeout-ms: 10000
    transaction:
      enabled: false
```

## Boundaries

- Mongo transactions require replica set or cluster deployment; standalone MongoDB usually does not satisfy transaction requirements.
- `removeClassField=true` removes `_class`; this reduces document noise but may affect polymorphic deserialization.
- The generic service does not replace complex aggregation queries or business index design.

## Verification

```bash
mvn -f "peach-middleware/peach-mongo/pom.xml" -DskipTests package
```
