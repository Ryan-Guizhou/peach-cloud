---
name: using-peach-readme-writer
description: 为 peach-cloud 的 starter、autoconfigure、example、业务模块生成或刷新高质量 README.md，重点解决每次编辑完模块后 README 输出不稳定、结构混乱、缺少边界说明、缺少验证命令和排障信息的问题。Use when asked to write README.md/readme.md, after editing a starter or module, or when documenting peach-rocket, peach-storage, peach-threadpool and other peach-cloud modules.
---

# Peach README Writer

## 工作流

1. 仅在用户明确要求文档，或公共 API、配置、扩展点、运行机制和生产边界变化时触发；纯内部重构不扩大文档范围。
2. 先读取模块 `pom.xml`、源码入口、配置类、自动配置文件、示例代码和已有 README。
3. 不直接复用乱码或过期段落；以当前源码和构建文件为准重写。
4. README 使用中文 `README.md`，如已有 `README.en-US.md`，只在用户要求时同步英文版。
5. starter 文档必须讲清楚“模块提供什么、不提供什么、业务如何接入、如何扩展、如何验证”。
6. 写完后检查所有路径、类名、配置项、命令都能在仓库中找到或被当前改动引入，并运行 `node scripts/check-utf8.mjs` 确认为 UTF-8 无 BOM。

## 标准结构

按模块复杂度裁剪，但顺序尽量保持稳定：

1. 标题、简介、适用版本、维护信息。
2. 模块定位：解决什么问题，不解决什么问题。
3. 目录结构：只列源码、资源和文档，不列 `target/`。
4. 快速接入：Maven 依赖、最小配置、最小代码示例。
5. 核心概念：入口类、注解、配置类、SPI、请求/响应模型。
6. 配置说明：列出真实有效配置项、默认值和适用场景。
7. 扩展方式：如何覆盖 Bean、实现 SPI、注册 provider 或 handler。
8. 运行机制：关键链路用简短流程图或步骤说明。
9. 边界与限制：不承诺的语义、生产注意事项、已知限制。
10. 构建与验证：给出可执行 Maven 命令和测试范围。
11. 排障指南：用表格列出现象、检查点、处理方式。

## 写作规则

- 使用准确的模块名和 artifactId，不混用 starter、autoconfigure、example。
- 代码示例必须能和当前 API 对上；不要编造不存在的方法、配置项或注解。
- 对默认值要谨慎，优先从配置类读取；不确定时写“当前未在配置类中声明默认值”，不要猜。
- 明确生产边界，例如内存实现、自动创建资源、批量删除、异步执行、事务语义。
- 说明“如何覆盖默认实现”，优先给 `@Bean` 或 SPI 注册方式。
- 排障表必须面向真实失败：Bean 未注入、配置未生效、路径越界、消费重复、线程池阻塞等。
- 不把 README 写成营销文案；用工程事实、约束和可执行示例。

## 质量检查

- `README.md` 中每个类名、配置项、路径和命令都经过本地搜索确认。
- 没有复制 `target/`、`.flattened-pom.xml` 等构建产物作为源码结构。
- 没有泄露密钥、token、签名 URL 或真实生产地址。
- 没有承诺源码未实现的能力。
- 修改 starter 或模块后，README 包含本次新增/变更能力的用法和限制。
- 如果无法运行验证命令，在最终回复中说明原因。

## 与其他 Peach 技能配合

- 写 rocket README 时先使用 `$using-peach-rocket` 理清 MQ 边界。
- 写 storage README 时先使用 `$using-peach-storage` 理清 provider、路径和能力边界。
- 写 threadpool README 时先使用 `$using-peach-threadpool` 理清注解真实语义和配置参数。
- 写 email README 时先使用 `$using-peach-email` 理清 provider、凭证、重试和幂等边界。
- 写 Redis/Redisson README 时先使用 `$using-peach-redis` 理清缓存、消息、锁和 key 安全边界。
