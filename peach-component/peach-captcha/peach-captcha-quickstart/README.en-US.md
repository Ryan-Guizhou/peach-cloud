# Peach Captcha Quickstart

English | [中文](README.md)

This module is only a minimal runnable integration for `peach-captcha-starter`. Production modules must not depend on it.

```bash
mvn -f peach-component/peach-captcha/peach-captcha-quickstart/pom.xml spring-boot:run -Pdevelopment
```

Configure development Redis or other required resources according to the parent [`README.md`](../README.md). Never commit production credentials to the quickstart.
