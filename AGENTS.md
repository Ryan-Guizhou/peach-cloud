# peach-cloud Agent Guidelines

本文件只规定仓库级工作边界、Rules/Skills/MCP 路由和验证门禁。具体编码细节放在当前 Agent 原生 rules 与 skills 中维护。

## Agent Native Configuration

- Codex 使用 `.codex/rules/*.md`、`.codex/skills/*/SKILL.md` 和 `.codex/config.toml`。
- Cursor 使用 `.cursor/rules/*.mdc`、`.cursor/skills/*/SKILL.md` 和 `.cursor/mcp.json`。
- Agent 优先使用自身原生目录；另一套目录只作为只读参考，不能宣称已自动加载。
- 两套目录独立维护，不使用符号链接；同步语义时保留客户端专属格式。

## Project Context

- 后端：Maven 多模块，Java 21，Spring Boot `3.5.4`，Spring Cloud `2025.0.0`，Spring Cloud Alibaba `2025.0.0.0`。
- 前端：`peach-cloud-front`，Vue 3 + Vite + TypeScript，独立 npm 工程，不属于 Maven reactor。
- 项目版本：`${revision}`，根 POM 默认 `1.0.0-SNAPSHOT`；`development` 为默认 profile。
- 核心业务域：`peach-auth`、`peach-fileservice`、`peach-message`、`peach-setting`、`peach-generator`。
- 基础设施：`peach-common`、`peach-component`、`peach-middleware`。

## Precedence

冲突时按以下顺序处理：

1. 用户最新明确要求。
2. 安全、隐私、数据完整性和可验证正确性。
3. 本文件的仓库级边界。
4. 当前 Agent 原生 rules。
5. 当前任务命中的 skills。
6. 当前模块局部风格。
7. 存量代码。

存量代码只能作为兼容性证据。敏感数据泄露、错误日志级别、事务失效、资源泄漏、无效注解和占位注释不能继续复制。

## Standard Workflow

### 1. 先分类，再加载最小上下文

1. 判断任务属于代码修改、代码审查、文档/配置、外部系统操作还是纯问答。
2. 代码修改/审查先读取当前 Agent 原生 rules：`02-output-and-evidence`、`03-module-and-change-boundaries`、`05-language-and-encoding`、`08-security-and-quality-gates`。
3. Java/XML 或后端生成器任务再读取 `06-layered-java-style`、`09-java21-coding-style`；README/模块文档读取 `04-documentation-and-readme`；starter/SPI/公共契约读取 `10-architecture-and-starters`；注释或日志读取 `07-comments-and-logging`；MCP/Skill/外部文档读取 `01-mcp-and-skills`。
4. 需要判断 Java/TypeScript 符号、调用链、影响面、starter 装配或运行机制时，先用 CodeGraph 建立事实。纯文本 README、SQL、脚本和静态配置可直接读文件。
5. 按 Skill 路由表选择最小必要集合，完整读取命中 `SKILL.md`，再按其说明只读取必要 reference。
6. 用当前源码、POM、配置、测试和 SQL 复核事实；Skill、记忆、README 和相邻代码不能替代当前证据。

### 2. 编辑中控制范围

- 只修改完成用户目标所需的代码、测试和行为相关文档。
- 保留用户已有未提交改动，不执行破坏性 Git/文件操作。
- Controller、Service、DTO/VO、DAO/XML、配置和文档存在契约联动时同步检查。
- 不确定第三方 API 或版本行为时查当前依赖源码或官方文档。

### 3. 完成后验证

- Java/XML 修改优先使用 IDEA 构建与文件问题检查；IDE 工具不可用时运行受影响 Maven 模块编译/测试。
- 前端修改运行匹配范围的 npm 类型检查、构建或测试；不要用 Maven 验证 `peach-cloud-front`。
- 所有代码、配置、README、rules、skills 修改至少运行 `node scripts/check-utf8.mjs` 和 `git diff --check`。
- 无法执行的门禁必须说明原因、未验证范围和残余风险。

## MCP Routing

| 意图 | 首选工具 | 要求 |
| --- | --- | --- |
| 当前代码结构、符号、调用链、影响面 | CodeGraph | 查询范围限定到 `peach-cloud` 根目录；结果出现 `.m2`、`target`、IDE 插件或仓外目录时不得作为证据 |
| 框架、SDK、配置语法和版本差异 | Context7 或官方文档 | 不用不明博客替代版本事实 |
| PR、Issue、Actions、远程仓库 | GitHub MCP/App | 仅用户明确要求；推送、评论、开 PR 需明确授权 |
| schema、样例数据、只读 SQL | MySQL MCP | 仅用户明确要求且只读连接可用；禁止未经授权写库 |
| 运行时状态、断点、调用时值 | IDEA Debugger | 静态证据不足时使用 |
| README、SQL、脚本、配置精确文本 | 本地读取或 `rg` | 不用全文搜索替代调用链分析 |

CodeGraph 不可用时，先用项目内 CodeGraph CLI；CLI 不可用再用 IDEA 索引；仍不可用才用 `rg` + 精确源码读取，并在最终回复说明降级。

MCP 配置不得写入 secret。secret 只通过环境变量传入，不写源码、文档、记忆或回复。

## Skill Routing

Skill 是执行规范，不是背景材料。命中后先读 `SKILL.md`，再按需读取 reference；不得一次性加载全部 skills。

### 后端与基础设施

| 修改范围 | 必用 Skill | 叠加条件 |
| --- | --- | --- |
| REST、Entity、DAO/XML、Service、common | `using-peach-code-skeleton` | 按任务层级读取对应 reference |
| starter、autoconfigure、quickstart、SPI、provider 装配与依赖 | `using-peach-code-skeleton` | 读取 `references/starter.md` 和 `10-architecture-and-starters`；检查配置提示、条件装配、传递依赖和 quickstart |
| Scheduler、Quartz、调度执行、租约、结果上报 | `using-peach-code-skeleton` + `using-peach-scheduler` | 先用 CodeGraph 分析定义、触发、分发、执行和上报链路；涉及 MQ transport 叠加 `using-peach-rocket`；涉及 Handler 阻塞 IO 叠加 `using-peach-virtual-thread` |
| RocketMQ、事件、事务消息、Outbox | `using-peach-code-skeleton` + `using-peach-rocket` | 生产、消费、幂等、Topic 或 SPI 相关均触发 |
| Storage、上传下载、签名 URL、provider | `using-peach-code-skeleton` + `using-peach-storage` | 路径安全、分片、直传、多 provider 均触发 |
| 虚拟线程、阻塞 IO 异步、旧线程池迁移 | `using-peach-code-skeleton` + `using-peach-virtual-thread` | `VirtualExecutorService`、`VirtualExecutorRegistry`、`@VirtualGroup`、`peach.virtual-thread`、旧 `ThreadPoolManager` 迁移均触发 |
| Email、SMTP、模板、附件、重试 | `using-peach-code-skeleton` + `using-peach-email` | transport、resolver、幂等或扩展点均触发 |
| Redis、Redisson、缓存、锁、队列 | `using-peach-code-skeleton` + `using-peach-redis` | Stream、多级缓存、Bloom、repeat 等均触发 |
| README 或模块接入文档 | `using-peach-readme-writer` | starter、autoconfigure、quickstart、业务模块或公共配置/API 变化时同步使用 |

### 前端与设计

| 修改范围 | 必用 Skill | 叠加条件 |
| --- | --- | --- |
| Vue 3、TypeScript、Vite、Axios、Pinia、Router、Ant Design Vue | `using-peach-front` | 页面、组件、API、状态、路由权限和目录结构均触发 |
| 新页面视觉、布局、交互、响应式、可访问体验 | `using-peach-front` + `ui-ux-pro-max` | 仅视觉体验任务叠加 |
| 设计 token、主题、字体/间距尺度、组件状态规范 | `using-peach-front` + `design-system` + `ui-ux-pro-max` | 保持 Ant Design Vue 技术栈 |
| Tailwind、shadcn/ui、Canvas | `using-peach-front` + `ui-styling` | 仅用户明确要求 |
| Banner、品牌、演示稿等设计资产 | 对应设计 Skill | 仅设计交付物明确命中时使用 |

## Non-Negotiable Boundaries

- Java 代码必须使用 Java 21；Spring API 必须符合项目锁定版本。
- Spring Bean 使用构造器注入；新增或实质修改依赖时不使用字段 `@Resource` 或 `@Autowired`。
- 所有文本文件必须为 UTF-8 无 BOM。
- 禁止在日志、异常、文档、审计或非授权响应中暴露密码、token、secret、私钥、签名 URL、身份证号等敏感数据。
- 操作日志不得直接打印完整 DTO、完整 command 或完整业务参数。
- `*-rest` 只做接口适配，业务与事务在 `*-service`，模型在 `*-entity`。
- 业务模块优先依赖 starter，不直接耦合 autoconfigure 或厂商 SDK。
- 标准 starter 结构为 `autoconfigure / starter / quickstart`；禁止新增或保留 `example` 层，现有示例统一迁移为 quickstart。
- 公共 API、公共响应、DAO/XML、生成器模板、starter 配置改动前必须评估影响面。
- 数据库默认只读；GitHub 推送、PR、评论以及数据库写入必须有用户明确授权。

## Output Contract

默认中文，先给结论，再给证据。非平凡代码或配置任务最终回复包含：

1. 实际使用的 CodeGraph/IDE/CLI 能力和读取的 Skills；发生降级时说明原因。
2. 关键符号与调用链，绑定文件或符号。
3. 检查过的关键文件、实际修改文件和受影响调用方。
4. 实际运行的构建、测试、UTF-8、MCP 配置和 diff 检查；未执行项单列。

不要倾倒原始命令流水账，也不要声称使用了当前会话未提供的工具。
