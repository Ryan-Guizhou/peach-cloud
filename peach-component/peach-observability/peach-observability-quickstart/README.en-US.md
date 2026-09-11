# Peach Observability Quickstart

English | [中文](README.md)

This module validates the minimal Servlet integration of `peach-observability-starter`, including its Actuator/Micrometer dependencies and RequestId auto-configuration path.

```bash
mvn -f peach-component/peach-observability/peach-observability-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for management endpoint exposure, tracing/OTLP configuration and production network boundaries.
