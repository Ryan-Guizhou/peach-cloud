---
name: using-peach-email
description: 规范 peach-cloud 中 peach-email-starter / peach-email-autoconfigure 的邮件发送、SMTP provider、模板渲染、重试、幂等、附件安全和扩展方式。Use when editing email code, configuring peach.email, adding EmailTransport/TemplateResolver, reviewing mail delivery logic, or writing peach-component/peach-email documentation.
---

# Peach Email

## 工作流

1. 先确认任务属于业务接入、provider 扩展、模板扩展、可靠性治理、代码审查或 README 更新。
2. 业务模块依赖 `peach-email-starter`，发送统一通过 `EmailSendService`。
3. 涉及配置、默认实现、SPI、模块路径或生产边界时，完整读取 `references/module-guide.md`。
4. 编写 Java 分层代码时同时使用 `using-peach-code-skeleton`；更新文档时同时使用 `using-peach-readme-writer`。
5. 改动后执行专项构建、UTF-8 检查和 `git diff --check`。

## REQUIRED

- SMTP 密码、授权码、收件人列表和邮件正文不得进入日志、异常、测试快照或文档。
- provider 凭证来自环境变量、配置中心或密钥服务，不得写死在仓库。
- 自动发送必须使用稳定业务幂等键；生产多实例环境覆盖默认内存 `IdempotencyStore`。
- 重试只覆盖可恢复故障；认证失败、地址无效和参数错误不得盲目重试。
- 附件和内嵌资源进入组件前校验文件名、大小、类型、来源和读取权限。
- 新增 `EmailTransport` 或 `TemplateResolver` 时同步 SPI 注册文件、配置说明和测试。

## PREFERRED

- 请求主链路只提交邮件任务；实际 SMTP 发送在可观测、可重试的异步边界执行。
- provider 路由、重试次数、发送耗时和最终失败使用脱敏指标观测。
- 模板只接受白名单变量，HTML 内容按来源执行转义或净化。
- 覆盖默认 Bean 时优先实现现有接口；`SmtpConnectionProvider` 还必须核对静态 `SmtpConnections` 注册链路。

## LEGACY_COMPATIBLE

- `Idempotency` 包名和现有 transport 类名拼写仅为兼容事实；新增 API 使用规范命名。
- 当前模板渲染与发送是两个独立步骤，不假设 `EmailSendService` 自动解析模板。
- 当前模板 resolve、附件数据源、默认 provider 排序和可重试错误分类存在已知限制，修复并测试前不得包装为可靠范式。

## FORBIDDEN

- 直接在业务模块创建 JavaMail Session 或绕过 `EmailSendService` 复制发送逻辑。
- 把默认内存幂等描述为跨进程、持久化或有 TTL 的保证。
- 在同步 Web 请求中进行无上限 SMTP 重试。
- 记录完整邮件对象来排障。

## 代码审查重点

- `send` 与 `sendAuto` 的语义是否被正确选择。
- 幂等记录是否只在成功后发生，key 是否包含通知类型和业务唯一标识。
- provider 优先级、故障转移与重试是否会放大重复发送。
- 自定义连接 provider 是否释放连接和线程资源。
- 模板路径和附件路径是否可被用户输入越界控制。
- starter 的 optional 运行时依赖是否已由最终应用提供。

## 验证

```bash
mvn -f "peach-component/peach-email/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```
