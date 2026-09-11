# Peach ThreadPool Quickstart

[English](README.en-US.md) | 中文

本模块仅用于验证存量 `peach-threadpool-starter` 兼容接入。新建阻塞 IO 异步能力优先使用 `peach-virtual-thread`，不要把该 quickstart 当成新业务模板。

```bash
mvn -f peach-component/peach-threadpool/peach-threadpool-quickstart/pom.xml spring-boot:run -Pdevelopment
```

存量 API、配置和迁移边界见父级 [`README.md`](../README.md)。
