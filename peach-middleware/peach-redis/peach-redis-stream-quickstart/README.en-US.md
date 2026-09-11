# Peach Redis Stream Quickstart

English | [中文](README.md)

This module validates the minimal integration of `peach-redis-stream-starter`. Provide a development Redis instance; consumers still require explicit business idempotency, ACK, retry and backlog governance.

```bash
mvn -f peach-middleware/peach-redis/peach-redis-stream-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for capability boundaries.
