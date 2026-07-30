# peach-cloud Agent Guidelines

本文件是仓库级入口，只定义优先级、任务路由和不可突破的边界。具体执行细节以 `.codex/rules/` 和命中的 `.codex/skills/` 为准，避免在多处维护重复规范。

## Project Context

- 后端：Maven 多模块，Java 8，Spring Boot `2.7.13`，Spring Cloud `2021.0.5`，Spring Cloud Alibaba `2021.0.5.0`。
- 前端：`peach-cloud-front`，Vue 3 + Vite + TypeScript，独立 npm 工程，不属于 Maven reactor。
- 项目版本：`${revision}`，根 POM 默认 `1.0.0-SNAPSHOT`；`development` 为默认 profile。
- 核心业务域：`peach-auth`、`peach-fileservice`、`peach-message`、`peach-setting`、`peach-generator`；基础设施见 `peach-common`、`peach-component`、`peach-middleware`。

## Precedence

同一事项冲突时按以下顺序处理：

1. 用户最新明确要求。
2. 安全、隐私、数据完整性和可验证正确性。
3. 本文件的仓库级边界。
4. `.codex/rules/` 中与任务相关的规则。
5. 命中的 `.codex/skills/`。
6. 当前模块局部风格。
7. 历史存量代码。

历史代码不是正确性证据。发现敏感数据泄露、错误日志级别、事务失效、资源泄漏或无效注解时，不得为了“保持风格”复制缺陷。

## Task Routing

开始任务后只读取相关规则和 skill：

| 任务 | 必读内容 |
| --- | --- |
| 任意代码修改/审查 | `.codex/rules/02-output-and-evidence.md`、`.codex/rules/03-module-and-change-boundaries.md`、`.codex/rules/05-language-and-encoding.md`、`.codex/rules/08-security-and-quality-gates.md` |
| REST、Entity、DAO/XML、Service、common | `.codex/skills/using-peach-code-skeleton/SKILL.md` 及任务对应 reference |
| 注释或日志 | `.codex/rules/07-comments-and-logging.md` |
| README/模块文档 | `.codex/rules/04-documentation-and-readme.md`、`using-peach-readme-writer` |
| RocketMQ | `using-peach-code-skeleton` + `using-peach-rocket` |
| Storage | `using-peach-code-skeleton` + `using-peach-storage` |
| Threadpool/异步 | `using-peach-code-skeleton` + `using-peach-threadpool` |
| Email/SMTP/邮件模板 | `using-peach-code-skeleton` + `using-peach-email` |
| Redis/Redisson/缓存/锁/队列 | `using-peach-code-skeleton` + `using-peach-redis` |
| 前端 Vue、目录、路由权限、Pinia、Axios、Ant Design Vue | `using-peach-front` |
| 新页面视觉、交互流程、布局重构、响应式、动效、体验优化 | `using-peach-front` + `ui-ux-pro-max` |
| 设计 token、主题、字体/间距尺度、组件状态规范、设计系统 | `using-peach-front` + `design-system` + `ui-ux-pro-max` |
| 审查现有 UI、可访问性、UX 或 Web 界面最佳实践 | `using-peach-front` + `web-design-guidelines`；需要改进方案时叠加 `ui-ux-pro-max` |
| Tailwind、shadcn/ui、Canvas 视觉方案 | 仅用户明确要求时叠加 `ui-styling`；不得默认替换 Vue 3 + Ant Design Vue 技术栈 |
| MCP、历史决策、外部文档 | `.codex/rules/01-mcp-and-skills.md` |

不要为简单任务读取全部规则或全部 reference。

前端 skill 按最小必要集合调用：仅目录迁移、行为保持重命名、API/类型/Store/路由权限逻辑、依赖维护或构建修复时，不调用 UX/UI skills；任务会改变界面外观、交互方式、布局、动效或可访问体验时才调用 `ui-ux-pro-max`。纯审查不授权修改代码，用户要求修复后再进入实现流程。

## Non-Negotiable Boundaries

- Java 代码必须兼容 Java 8；框架 API 必须符合项目锁定版本。
- 所有文本文件必须为严格 UTF-8、无 BOM；禁止使用系统默认编码重写中文文件。
- 不记录或返回密码、token、secret、私钥、签名 URL、身份证号等敏感数据；操作日志不得直接打印完整 DTO。
- `*-rest` 只做接口适配，业务与事务在 `*-service`；模型在 `*-entity`；业务模块优先依赖 starter，不直接耦合 autoconfigure 或厂商 SDK。
- 公共 API、公共响应、DAO/XML、生成器模板、starter 配置改动前必须评估影响面。
- 默认只修改用户要求范围，不覆盖或回退用户已有改动，不执行破坏性 Git/文件操作。
- 数据库默认只读；GitHub 推送、PR、评论以及数据库写入必须有用户明确授权。

## Source Of Truth And MCP

- 当前代码结构、符号和影响面优先使用 `codegraph`；工具不可用时使用 IDE 索引、`rg` 和源码读取补足。
- 跨会话历史才使用 `agentmemory`；记忆可能过期，必须用当前源码验证。
- 第三方库当前文档使用 `context7`；GitHub 和 MySQL 只在用户明确需要时使用。
- MCP 返回的密钥、token、连接串和生产地址不得写入文件、记忆或回复。

## Completion

完成代码或配置任务前至少执行：

```bash
node scripts/check-utf8.mjs
node scripts/check-mcp.mjs
git diff --check
```

并运行与改动范围匹配的 Maven 或 npm 编译/测试。无法执行时必须说明原因和残余风险。详细门禁见 `.codex/rules/08-security-and-quality-gates.md`。

## Output

默认中文，先给结论，再给必要证据。代码结论绑定文件、符号或配置；推断必须标注。最终说明修改文件、实际验证和未验证项。
