---
name: using-peach-readme-writer
description: Use when writing or refreshing peach-cloud README files for starters, autoconfigure modules, quickstart modules, business modules, public APIs, configuration, extension points, runtime behavior, or production boundaries.
---

# Peach README Writer

## 工作流

1. 仅在用户明确要求文档，或公共 API、配置、扩展点、运行机制和生产边界变化时触发；纯内部重构不扩大文档范围。
2. 涉及源码符号、调用链、starter 装配或运行机制时，先用 CodeGraph 建立事实，再读取模块 `pom.xml`、源码入口、配置类、配置元数据、自动配置注册文件、示例代码、测试和已有 README。
3. 判断文档类型：starter/autoconfigure/quickstart 读取 `references/starter-readme.md`；业务模块读取 `references/business-module-readme.md`。
4. 不直接复用乱码、过期段落或源码不存在的能力；以当前源码、POM、配置属性、自动配置和测试为准重写。
5. README 使用中文 `README.md`，如已有 `README.en-US.md`，只在用户要求或本次已修改英文版时同步英文版。
6. 写完后检查所有路径、类名、配置项、命令都能在仓库中找到或被当前改动引入，并运行 `node scripts/check-utf8.mjs` 与 `git diff --check`。

## 写作规则

- 使用准确的模块名和 artifactId，不混用 autoconfigure、starter、quickstart。
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

- 写 scheduler README 时先使用 `$using-peach-scheduler` 理清定义、触发、分发、租约、执行和上报链路。
- 写 rocket README 时先使用 `$using-peach-rocket` 理清 MQ 边界。
- 写 storage README 时先使用 `$using-peach-storage` 理清 provider、路径和能力边界。
- 写 virtual-thread README 时先使用 `$using-peach-virtual-thread` 理清分组、背压、取消、关闭和平台线程边界。
- 写 email README 时先使用 `$using-peach-email` 理清 provider、凭证、重试和幂等边界。
- 写 Redis/Redisson README 时先使用 `$using-peach-redis` 理清缓存、消息、锁和 key 安全边界。
