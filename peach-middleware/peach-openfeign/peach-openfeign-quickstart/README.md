# Peach OpenFeign Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-openfeign-starter` 的最小接入和治理自动配置。真实 Feign 契约仍应来自业务 `*-openfeign-external` 模块，本 quickstart 不复制业务接口。

```bash
mvn -f peach-middleware/peach-openfeign/peach-openfeign-quickstart/pom.xml spring-boot:run -Pdevelopment
```

Same-Token、RequestId、超时、重试与 Sentinel 边界见父级 [`README.md`](../README.md)。
