# Peach Redis Tool Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-redis-tool-starter` 的最小接入。运行前提供开发环境 Redis，业务 key 需要保持命名空间、TTL 与数据隔离边界。

```bash
mvn -f peach-middleware/peach-redis/peach-redis-tool-quickstart/pom.xml spring-boot:run -Pdevelopment
```

完整能力边界见父级 [`README.md`](../README.md)。
