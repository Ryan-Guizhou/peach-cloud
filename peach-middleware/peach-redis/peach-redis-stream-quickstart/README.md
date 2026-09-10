# Peach Redis Stream Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-redis-stream-starter` 的最小接入。运行前提供开发环境 Redis；消费者仍必须定义业务幂等、ACK、重试和积压治理。

```bash
mvn -f peach-middleware/peach-redis/peach-redis-stream-quickstart/pom.xml spring-boot:run -Pdevelopment
```

完整能力边界见父级 [`README.md`](../README.md)。
