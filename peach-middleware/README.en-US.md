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
