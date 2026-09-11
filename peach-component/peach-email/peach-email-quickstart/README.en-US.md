# Peach Email Quickstart

English | [中文](README.md)

This module validates the minimal bootstrap and auto-configuration of `peach-email-starter`. Supply SMTP provider credentials from local environment variables or a configuration service; never store real credentials here.

```bash
mvn -f peach-component/peach-email/peach-email-quickstart/pom.xml spring-boot:run -Pdevelopment
```

See the parent [`README.md`](../README.md) for mail APIs, provider routing, retry and idempotency boundaries.
