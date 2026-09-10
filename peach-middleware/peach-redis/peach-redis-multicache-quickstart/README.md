# Peach Redis MultiCache Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-redis-multicache-starter` 的最小接入。运行前提供开发环境 Redis，缓存 TTL、回源和一致性策略仍由业务明确。

```bash
mvn -f peach-middleware/peach-redis/peach-redis-multicache-quickstart/pom.xml spring-boot:run -Pdevelopment
```

完整能力边界见父级 [`README.md`](../README.md)。
