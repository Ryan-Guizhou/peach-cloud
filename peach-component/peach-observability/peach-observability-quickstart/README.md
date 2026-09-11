# Peach Observability Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-observability-starter` 在 Servlet 应用中的最小接入，包括 Actuator/Micrometer 依赖和 RequestId 自动配置链路。

```bash
mvn -f peach-component/peach-observability/peach-observability-quickstart/pom.xml spring-boot:run -Pdevelopment
```

管理端点暴露、Trace/OTLP 和生产网络边界见父级 [`README.md`](../README.md)。
