# peach-email

[English](README.en-US.md) | 中文

最后更新时间：2026-07-03  
artifactId：`peach-email`  
类型：邮件组件聚合模块

## 模块定位

`peach-email` 提供邮件发送、SMTP 连接、模板渲染、provider 路由、重试和幂等等能力。业务模块通过 `peach-email-starter` 接入，不直接散落维护 JavaMail、模板和重试逻辑。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-email-autoconfigure` | 邮件核心 API、配置、默认实现和扩展点 |
| `peach-email-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `EmailProperties` | 绑定 `peach.email` 配置 |
| `EmailTransport` | 邮件发送传输接口 |
| `SmtpConnectionProvider` | SMTP 连接提供者 |
| `TemplateResolver` | 模板解析接口 |
| `TemplateRenderer` | 模板渲染接口 |
| `RetryPolicy` | 重试策略 |
| `IdempotencyStore` | 幂等存储 |
| `EmailProviderEnum` | 邮件 provider 枚举 |

## 接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-email-starter</artifactId>
</dependency>
```

最小配置应从 `EmailProperties` 对应字段确认。不要在 README、配置样例或日志中写入真实 SMTP 密码和生产邮箱授权码。

## 扩展方式

- 自定义 `EmailTransport`：接入非 SMTP provider 或厂商 API。
- 自定义 `SmtpConnectionProvider`：控制连接创建、复用和释放策略。
- 自定义 `TemplateResolver` / `TemplateRenderer`：替换模板来源或渲染引擎。
- 自定义 `RetryPolicy`：调整可重试异常、次数和退避策略。
- 自定义 `IdempotencyStore`：将幂等从内存扩展到 Redis、数据库或其他持久存储。

## 运行机制

1. 业务提交邮件发送请求。
2. 路由逻辑选择 provider。
3. 模板能力解析并渲染邮件内容。
4. 幂等组件判断是否重复发送。
5. 传输层发送邮件，失败时按重试策略处理。

## 边界与限制

- 默认简单实现不等于生产级邮件平台。
- SMTP 连接池、限流、退信、黑名单、邮件追踪需要结合 provider 单独治理。
- 幂等 key 必须和业务唯一性一致，否则可能误拦截或重复发送。
- 重试可能导致重复触达，生产环境应结合幂等存储使用。

## 构建与验证

```bash
mvn -f "peach-component/peach-email/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-email -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 邮件发送失败 | SMTP 地址、账号、授权码、端口、TLS 是否正确 | 先用测试邮箱验证，再检查应用日志 |
| 模板渲染失败 | 模板路径、变量名、空值处理 | 检查 `TemplateResolver` 和 `TemplateRenderer` |
| 重复发送 | 幂等 key 和 `IdempotencyStore` 是否稳定 | 使用持久化幂等存储 |
| 发送很慢 | SMTP 连接创建和 provider 限流 | 检查连接 provider、超时和重试策略 |
