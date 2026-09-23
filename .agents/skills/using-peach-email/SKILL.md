---
name: using-peach-email
description: 修改或审查 peach-component/peach-email 的 EmailSendService、EmailTransport、TemplateResolver、SMTP Provider、重试、幂等、附件/内嵌资源、安全和 Provider 扩展时使用。
---

# Peach Email

## 核心不变量

- 业务通过 `EmailSendService` 和现有契约发送，不在业务模块自行创建 JavaMail Session 或复制 SMTP 发送逻辑。
- SMTP 密码/授权码、收件人列表、完整正文和附件内容不进入日志、异常、测试快照或文档。
- 自动发送使用稳定业务幂等键；生产多实例不能把默认内存 `IdempotencyStore` 当成持久化保证。
- 重试只覆盖可恢复故障；认证失败、地址无效、参数错误不盲目重试。
- provider 故障转移和重试必须评估重复发送风险。
- 附件/内嵌资源校验文件名、大小、类型、来源、路径和读取权限，避免路径越界和不可信内容。
- 新 `EmailTransport` / `TemplateResolver` 同步考虑 SPI/自动配置、优先级、资源关闭和测试。
- 模板变量使用受控输入；不可信 HTML 按来源执行转义或净化。

具体配置和当前默认实现按需读取现有 `references/module-guide.md`。

需要文档时进入 `project-doc-engineer`。
