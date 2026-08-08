# peach-cloud Agent Guidelines

本文件是仓库级入口，负责规定优先级、任务工作流、Rules/Skills/MCP 路由和不可突破的边界。具体编码细节由当前 Agent 的原生项目配置承载，不在本文件重复维护。

> **代理必读**：涉及代码、配置、SQL 或生成器模板的修改/审查时，必须先按「标准工作流」定位和评估，再开始编辑。工具未暴露或不可用时必须按降级路径补足证据，不得假装已调用或直接跳过。

## Agent Native Configuration

- Cursor 使用 `.cursor/rules/*.mdc`、`.cursor/skills/*/SKILL.md` 和 `.cursor/mcp.json`。
- Codex 使用 `.codex/rules/*.md`、`.codex/skills/*/SKILL.md` 和 `.codex/config.toml`。
- Agent 必须优先使用自身原生目录，不跨目录混用另一客户端的 Rules、Skills 或 MCP 配置。
- 原生内容缺失时才允许把另一目录作为只读参考；不得宣称参考内容已由当前客户端自动加载，最终回复需说明降级。
- 两套目录均为独立真实文件，不使用符号链接；修改一套配置时应检查另一套是否需要同步语义，但不得机械覆盖客户端专属格式。

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
4. 当前 Agent 原生目录中与任务相关的 Rules。
5. 当前 Agent 命中的 Skills。
6. 当前模块局部风格。
7. 历史存量代码。

历史代码不是正确性证据。发现敏感数据泄露、错误日志级别、事务失效、资源泄漏或无效注解时，不得为了“保持风格”复制缺陷。

## Standard Workflow

### 1. 开始前：先分类，再加载最小上下文

1. 判断任务属于代码修改、代码审查、文档/配置、外部系统操作还是纯问答；纯审查和诊断默认不授权修改。
2. 代码修改/审查先应用当前 Agent 原生 Rules 目录中的以下规则：
    - `02-output-and-evidence`
    - `03-module-and-change-boundaries`
    - `05-language-and-encoding`
    - `08-security-and-quality-gates`
3. 涉及 MCP、跨会话历史或第三方文档时，再应用 `01-mcp-and-skills`。
4. 使用 CodeGraph 获取任务上下文；修改公共 API、公共响应、DAO/XML、starter 配置、基础实体或生成器模板前，必须额外执行影响面分析。
5. 按 Skill 触发表选择最小必要集合，完整读取每个命中 Skill 的 `SKILL.md`，再按其路由只读取必要 reference。
6. 对照当前源码、POM、配置、测试和 SQL 文件确认事实；Skill、历史记忆和相邻代码均不能替代当前证据。

README、Markdown、脚本、静态配置和 SQL 文本的纯文本编辑可直接读取文件；但只要需要判断 Java/TypeScript 符号、调用链或影响面，仍须走 CodeGraph。

### 2. 编辑中：控制范围

- 只修改完成用户目标所需的代码、测试和行为相关文档，保留用户已有未提交改动。
- 遵循命中的 Rules 与 Skills；安全、正确性、Java 8、模块边界和 UTF-8 无 BOM 是硬约束。
- 变更 Controller、Service、DTO/VO、DAO/XML、配置或文档之间存在联动时，同步检查，不留下半套契约。
- 不确定第三方 API 或版本行为时查当前依赖源码或 Context7，不凭记忆生成。

### 3. 完成后：验证与交付

1. Java/XML 修改优先使用当前会话已提供的 IDEA `build_project`，并对改动文件执行 `get_file_problems`；IDE 工具未提供时，使用受影响 Maven 模块的编译/测试作为降级验证。
2. 前端修改运行与范围匹配的 npm 类型检查、构建或测试；不要用 Maven 验证 `peach-cloud-front`。
3. 只有静态证据不足以定位运行时状态或长调用链问题时才使用调试器；数据库查询仅在用户明确要求且只读连接可用时执行。
4. 所有代码或配置任务至少执行：

```bash
git diff --check
```

5. 无法执行的门禁必须在最终回复中说明原因、未验证范围和残余风险，不得写成“已验证”。

## MCP Routing

配置存在不代表每次任务都要调用；以当前会话实际暴露的工具为准。先根据问题选择工具，不为“展示使用 MCP”制造无关调用。

### CodeGraph：代码事实与影响面

| 意图 | 首选工具 | 使用要求 |
| --- | --- | --- |
| 理解功能、模块或 Bug 上下文 | `codegraph_context` | 代码任务首选入口；通常再用一次 `codegraph_explore` 补源码即可 |
| 精确查找符号 | `codegraph_search` | 不先用全仓库 `rg` 猜符号位置 |
| 查看单个/批量符号源码 | `codegraph_node` / `codegraph_explore` | 批量读取优先 `explore`，避免循环调用 `node` |
| 分析 A 到 B 的调用路径 | `codegraph_trace` | 不用多次 callers 手工拼接完整链路 |
| 查询调用方/被调用方 | `codegraph_callers` / `codegraph_callees` | 适合直接上下游；深链路改用 `trace` |
| 评估修改爆炸半径 | `codegraph_impact` | 改公共符号、DAO 签名、配置契约前必做 |
| 检查索引状态 | `codegraph_status` | 初始化失败、结果异常或编辑后提示 stale 时使用 |

CodeGraph 查询必须限定在 `peach-cloud` 根目录。结果若出现 `.m2`、`target`、IDE 插件或仓外目录，视为 scope 错误，不得作为项目证据。CodeGraph 负责结构分析，不替代编译、测试或运行时证据。

### 其他 MCP / IDE 工具

| 工具 | 触发条件 | 不应使用的情况 |
| --- | --- | --- |
| `context7` | 框架、SDK、配置语法、版本差异或迁移行为不确定 | 纯业务逻辑、仓库内重构、已有源码可确认的事实 |
| `agentmemory` | 用户在延续跨会话工作，需找历史决策或已确认排障结论 | 当前源码事实、一次性任务；命中结果必须由当前源码复核 |
| GitHub MCP/App | 用户明确要求查看或操作 PR、Issue、Actions、远程仓库 | 本地代码修改；推送、评论、开 PR 需明确授权 |
| MySQL MCP | 用户明确要求核对 schema、样例数据或执行只读 SQL | 默认代码任务；禁止未经授权写库 |
| IDEA 工具 | 编辑后构建、静态问题、运行配置、语义重命名或运行时调试 | 不用 IDE 搜索替代可用的 CodeGraph 影响分析 |
| 本地文件读取/`rg` | README、SQL、脚本、配置、精确文本或 CodeGraph 缺失细节 | 不用全仓库文本搜索替代调用链和影响分析 |

### MCP 降级顺序

1. CodeGraph MCP 不可用时，使用项目内 CodeGraph CLI 的 `context`、`query`、`callers`、`impact`、`status` 等对应命令。
2. CLI 因未初始化或依赖不可用而失败时，使用 IDEA 索引；IDE 工具也不可用时，才使用 `rg` + 精确源码读取，并在最终回复说明降级与证据局限。
3. Context7 不可用时，优先查本地依赖源码和项目锁定版本的官方文档；不得用不明博客代替版本事实。
4. 工具未暴露时不得虚构结果。初始化索引、安装工具或进行联网下载若超出当前权限，应先说明并请求授权。

### MCP 安全

- secret 只通过环境变量传入，不写入 MCP 配置、源码、文档、记忆或回复。
- MySQL 默认只读；数据库写入、GitHub 推送/评论/PR、记忆删除/导出等外部副作用必须有用户明确授权。
- 修改 MCP 配置后必须运行 `node scripts/check-mcp.mjs`。
- 记忆只保存可复用且已验证的决策，并带项目、依据、适用模块、保存时间和重新验证条件；不得保存敏感数据、猜测或一次性过程。

## Skill Routing

Skill 是执行规范，不是背景资料。命中后必须先完整读取 `SKILL.md`，再开始修改；只读取 Skill 明确要求且与当前任务相关的 reference。

### 后端与基础设施

| 修改范围 | 必用 Skill | 叠加条件 |
| --- | --- | --- |
| REST、Entity、DAO/XML、Service、common | `using-peach-code-skeleton` | 按任务层级读取对应 reference |
| RocketMQ、事件、事务消息、Outbox | `using-peach-code-skeleton` + `using-peach-rocket` | 生产、消费、幂等、Topic 或 SPI 相关均触发 |
| Storage、上传下载、签名 URL、provider | `using-peach-code-skeleton` + `using-peach-storage` | 路径安全、分片、直传、多 provider 均触发 |
| Threadpool、异步、上下文传递 | `using-peach-code-skeleton` + `using-peach-threadpool` | `@AsyncExecuted`、拒绝策略、队列和超时均触发 |
| Email、SMTP、模板、附件、重试 | `using-peach-code-skeleton` + `using-peach-email` | transport、resolver、幂等或扩展点均触发 |
| Redis、Redisson、缓存、锁、队列 | `using-peach-code-skeleton` + `using-peach-redis` | Stream、多级缓存、Bloom、repeat 等均触发 |
| README 或模块接入文档 | `using-peach-readme-writer` | starter、autoconfigure、example 或公共配置/API 变化时同步使用 |

### 前端与设计

| 修改范围 | 必用 Skill | 说明 |
| --- | --- | --- |
| Vue 3、TS、Vite、Axios、Pinia、Router、Ant Design Vue | `using-peach-front` | 页面、组件、API、状态、路由权限和目录结构均触发 |
| 新页面视觉、布局、交互、响应式、动效、可访问体验 | `using-peach-front` + `ui-ux-pro-max` | 仅行为保持重命名、API/Store/权限逻辑或构建修复不叠加 UX Skill |
| 设计 token、主题、字体/间距尺度、组件状态规范 | `using-peach-front` + `design-system` + `ui-ux-pro-max` | 保持 Ant Design Vue 技术栈 |
| Tailwind、shadcn/ui、Canvas | `using-peach-front` + `ui-styling` | 仅用户明确要求；不得默认替换 Vue 3 + Ant Design Vue |
| Banner、品牌、演示稿等设计资产 | 对应 `banner-design`、`brand`、`design` 或 `slides` | 仅设计交付物明确命中时使用，不因普通页面开发全量加载 |

### Skill 使用规则

- 用户明确点名某个可用 Skill 时必须使用；若 Skill 不存在或无法读取，简要说明并采用最接近的仓库规则作为降级方案。
- 同时命中多个 Skill 时，采用最小必要集合，并先读基础 Skill，再读专项 Skill。例如 Redis Service 修改先读 `using-peach-code-skeleton`，再读 `using-peach-redis`。
- Skill 只对当前任务生效，不因上一次任务使用过而跳过本次读取。
- Skill 中的示例必须由当前源码、POM、配置或测试复核；不得把存量缺陷包装成范式。
- 纯审查只输出问题和证据，不自动修改；用户要求修复后再进入实现工作流。

## Rule Routing

开始任务后只读取相关 Rule，不一次性加载全部文件：

| 任务 | 追加读取 |
| --- | --- |
| MCP、Skills、历史决策、外部文档 | `01-mcp-and-skills` |
| README/模块文档 | `04-documentation-and-readme` |
| Java 分层与接口风格 | `06-layered-java-style` |
| 注释或日志 | `07-comments-and-logging` |

基础代码规则仍按「标准工作流」中的四个必读文件执行。

## Non-Negotiable Boundaries

- Java 代码必须兼容 Java 8；框架 API 必须符合项目锁定版本。
- 所有文本文件必须为严格 UTF-8、无 BOM；禁止使用系统默认编码重写中文文件。
- 不记录或返回密码、token、secret、私钥、签名 URL、身份证号等敏感数据；操作日志不得直接打印完整 DTO。
- `*-rest` 只做接口适配，业务与事务在 `*-service`；模型在 `*-entity`；业务模块优先依赖 starter，不直接耦合 autoconfigure 或厂商 SDK。
- 公共 API、公共响应、DAO/XML、生成器模板、starter 配置改动前必须评估影响面。
- 默认只修改用户要求范围，不覆盖或回退用户已有改动，不执行破坏性 Git/文件操作。
- 数据库默认只读；GitHub 推送、PR、评论以及数据库写入必须有用户明确授权。

## Output Contract

默认中文，先给结论，再给必要证据。非平凡的代码或配置任务最终回复应包含：

1. **工具与 Skills**：实际使用了哪些 CodeGraph/IDE/CLI 能力、读取了哪些 Skills；若发生降级，说明原因。
2. **关键符号与调用链**：入口 → Service → DAO/XML 或基础设施，并绑定文件或符号。
3. **检查与修改范围**：检查过的关键文件、实际修改文件和受影响调用方。
4. **验证结果**：实际运行的构建、测试、UTF-8、MCP 配置和 diff 检查；未执行项必须单列。

不要把原始工具输出或命令流水账倾倒给用户，也不要声称使用了当前会话未提供的工具。
