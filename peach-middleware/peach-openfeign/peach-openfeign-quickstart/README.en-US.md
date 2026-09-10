# Peach OpenFeign Quickstart

English | [中文](README.md)

This module validates the minimal integration and governance auto-configuration of `peach-openfeign-starter`. Real Feign contracts still belong to business `*-openfeign-external` modules; this quickstart does not duplicate business APIs.

```bash
mvn -f peach-middleware/peach-openfeign/peach-openfeign-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for Same-Token, RequestId, timeout, retry and Sentinel boundaries.
