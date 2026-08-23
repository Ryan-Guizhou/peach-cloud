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


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
