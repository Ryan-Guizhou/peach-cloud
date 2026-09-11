# Peach Sa-Token Quickstart

[English](README.en-US.md) | 中文

用于验证 `peach-satoken-starter` 的最小 Web 接入。业务登录、权限数据和 Same-Token 上游来源仍由实际服务提供，quickstart 不写入生产 Token 或固定密钥。

```bash
mvn -f peach-middleware/peach-satoken/peach-satoken-quickstart/pom.xml spring-boot:run -Pdevelopment
```

认证、Same-Token 与服务接入边界见父级 [`README.md`](../README.md)。
