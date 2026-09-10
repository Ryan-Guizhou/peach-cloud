# Peach Initialize Quickstart

English | [中文](README.md)

This module validates the minimal integration of `peach-initialize-starter`. Real initialization handlers belong to business beans and must define bounded latency, idempotency and startup-failure behavior.

```bash
mvn -f peach-component/peach-initialize/peach-initialize-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for the handler contract and runtime boundaries.
