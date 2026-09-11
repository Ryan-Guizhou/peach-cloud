# peach-satoken

[English](README.en-US.md) | 中文

`peach-satoken` 封装 Peach Cloud 的 Sa-Token Web 与内部 Same-Token 接入能力。业务服务通过 `peach-satoken-starter` 使用统一认证基础设施。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-satoken-autoconfigure` | Sa-Token Web/Same-Token 配置与自动装配 |
| `peach-satoken-starter` | 业务接入依赖入口 |
| `peach-satoken-quickstart` | 最小 Web 接入验证 |

## QuickStart

示例代码位于 [`peach-satoken-quickstart`](./peach-satoken-quickstart/)，用于验证 starter 的最小 Web 接入。业务登录、权限数据和 Same-Token 上游来源仍由实际服务提供，QuickStart 不写入生产 Token 或固定密钥。

```bash
mvn -f peach-middleware/peach-satoken/peach-satoken-quickstart/pom.xml spring-boot:run -Pdevelopment
```

## 边界

- 登录、用户角色/机构/权限数据仍由业务认证域负责，Starter 不成为权限数据事实源。
- Same-Token 用于内部服务调用身份约束，不替代业务级 API 权限校验。
- Token、Same-Token、密码和认证响应中的敏感字段不得写入日志或示例。
- 网关和业务服务的职责必须区分：入口认证/粗粒度拦截与业务侧最终授权不能相互替代。
