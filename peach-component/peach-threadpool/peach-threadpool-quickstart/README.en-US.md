# Peach ThreadPool Quickstart

English | [中文](README.md)

This module only validates compatibility integration for the legacy `peach-threadpool-starter`. New blocking-I/O asynchronous code should prefer `peach-virtual-thread`; do not use this quickstart as a new-business template.

```bash
mvn -f peach-component/peach-threadpool/peach-threadpool-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for legacy APIs, configuration and migration boundaries.
