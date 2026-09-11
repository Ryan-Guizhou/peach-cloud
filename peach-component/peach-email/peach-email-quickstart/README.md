# Peach Email Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-email-starter` 的最小启动和自动装配。SMTP provider、用户名和授权码通过本地环境变量或配置中心提供，本模块不保存真实凭据。

```bash
mvn -f peach-component/peach-email/peach-email-quickstart/pom.xml spring-boot:run -Pdevelopment
```

邮件发送 API、provider、重试和幂等边界见父级 [`README.md`](../README.md)。
