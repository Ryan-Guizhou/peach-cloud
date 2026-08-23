# peach-middleware

English | [中文](README.md)

## Purpose

`peach-middleware` aggregates infrastructure wrappers for Redis, Redisson, MongoDB, OpenFeign, Sa-Token, RocketMQ, and related middleware. It reduces repeated service-level configuration and direct SDK coupling.

## Submodules

| Submodule | Responsibility |
| --- | --- |
| `peach-redis` | RedisTemplate, Redisson, multi-level cache, Redis Stream, RedisDao |
| `peach-redission` | Redisson distributed lock, repeat-submit guard, delay queue, Bloom filter |
| `peach-mongo` | MongoDB generic service and auto-configuration |
| `peach-openfeign` | OpenFeign scanning, global settings, and log-level control |
| `peach-satoken` | Sa-Token Web/Gateway/Core adapters |
| `peach-rocket` | RocketMQ publishing, consuming, transaction, Outbox, and idempotency |
| `peach-kafka` | Kafka placeholder; keep documentation aligned with actual code |

## Usage Rules

- Business services should import concrete starters as needed.
- Middleware modules do not deploy infrastructure.
- Production configuration must explicitly manage endpoints, credentials, timeouts, retries, and resource limits.

## Verification

```bash
mvn -f "peach-middleware/pom.xml" -DskipTests package
```


## Project conventions

- Backend documentation follows the current peach-cloud baseline: Java 21, Spring Boot 3.5.4, Spring Cloud 2025.0.0, and Spring Cloud Alibaba 2025.0.0.0.
- Frontend documentation applies only to peach-cloud-front, which is a separate Vue 3 + Vite + TypeScript project and is not part of the Maven reactor.
- Source, scripts, SQL, and Markdown files must stay UTF-8 without BOM. Do not document generated output such as 	arget/, .flattened-pom.xml, dependency caches, or IDE files as source layout.
- Commands and examples must be verifiable against the current repository. Do not include real secrets, tokens, private keys, production passwords, signed URLs, or complete sensitive payloads.
