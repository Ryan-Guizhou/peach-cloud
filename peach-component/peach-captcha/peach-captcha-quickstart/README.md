# Peach Captcha Quickstart

[English](README.en-US.md) | 中文

本模块只用于验证 `peach-captcha-starter` 的最小接入和自动装配，不作为生产依赖。

```bash
mvn -f peach-component/peach-captcha/peach-captcha-quickstart/pom.xml spring-boot:run -Pdevelopment
```

需要验证码缓存或外部 Redis 时，请按父级 [`README.md`](../README.md) 配置真实开发环境；不要在 quickstart 中提交生产凭据。
