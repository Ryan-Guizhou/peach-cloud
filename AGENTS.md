# peach-cloud Agent Guidelines

本文件是 `peach-cloud` 仓库内 Codex、子代理和其他自动化编码助手的项目级工作规范。进入本仓库后，优先遵守本文件；如与用户当次明确要求冲突，以用户最新要求为准。

## Project Context

- 根目录：`D:\Coding\mine\new-mine\peach-cloud`
- 后端：Maven 多模块工程，Java 8，Spring Boot `2.7.13`，Spring Cloud `2021.0.5`，Spring Cloud Alibaba `2021.0.5.0`
- 前端：`peach-cloud-front`，Vue 3 + Vite + TypeScript，独立 npm 工程，不属于 Maven reactor
- 项目版本：`${revision}`，当前根 POM 默认 `1.0.0-SNAPSHOT`
- 常见 profile：`development`、`production`、`docker`、`test`，其中 `development` 默认激活

核心模块：

- `peach-auth`：认证、用户、角色、资源、操作日志
- `peach-gateway`：Spring Cloud Gateway 网关
- `peach-fileservice`：文件业务域
- `peach-message`：站内消息、公告、待办、推送
- `peach-setting`：字典、值集、通知、多语言配置
- `peach-monitor`：监控与审计
- `peach-generator`：代码生成
- `peach-common`：公共基础能力
- `peach-component`：captcha、email、initialize、storage、threadpool
- `peach-middleware`：redis、redission、mongo、openfeign、satoken、rocket、kafka
- `peach-sample`：组件和中间件示例

## Lookup And Navigation

文件查找和代码理解优先使用 `codegraph`。

优先级：

1. 使用 `mcp__codegraph` 查询项目结构、符号、调用关系和影响范围。
2. `codegraph` 无法覆盖的纯文本、配置、README、SQL、脚本，再使用 `rg` / `rg --files`。
3. 只在需要确认具体文件内容时读取文件。

`codegraph` 使用规则：

- 查目录：`codegraph_files`
- 查符号：`codegraph_search`
- 理解功能、架构、bug 背景：`codegraph_context`
- 评估公共符号改动影响：`codegraph_impact`
- 检查索引状态：`codegraph_status`
- 当前会话可能只暴露部分 codegraph 工具；未暴露的调用方、被调用方、追踪类能力，用已暴露工具结合源码读取补足。

不要先用全仓库 grep 重建 codegraph 已经能回答的问题。若 codegraph 提示索引滞后，针对提示中的 pending 文件直接读取文件确认。

## MCP Usage

本仓库 `.codex/config.toml` 当前声明以下 MCP server：

| MCP | 用途 | 使用要求 |
| --- | --- | --- |
| `codegraph` | 项目文件、符号、调用关系、影响范围 | 本项目代码查找优先使用 |
| `context7` | 第三方库、框架、SDK、CLI 的当前文档 | 涉及外部库用法、版本迁移、配置语法时优先使用 |
| `github` | GitHub 仓库、PR、issue、CI 信息 | 只有用户明确要求 GitHub/PR/CI 时使用 |
| `mysql` | MySQL 数据库上下文 | 只有用户要求查库或验证 SQL，并且环境变量可用时使用 |

运行时还可能存在未写入本仓库 `config.toml` 的 IDE 连接器，例如 `idea`。这类工具可以在当前会话中可用，但不能假设仓库已显式配置，使用前应先确认工具是否真的存在。

MCP 选择建议：

- 分析模块职责、调用链、公共 API 影响面：优先 `codegraph`
- 查 Spring Boot、Spring Cloud、Sa-Token、MyBatis、Vite 等第三方库最新用法：优先 `context7`
- 看 IntelliJ 当前打开文件、模块、符号文档、数据库连接、格式化文件：使用 `idea`
- 查数据库 schema、表结构、只读 SQL：使用 `mysql`
- 查 GitHub PR、issue、CI：仅在用户明确要求时使用 `github`

MCP 调用规范：

- 不要假设 MCP 一定可用；工具不可用时说明原因并使用本地文件作为 fallback。
- `context7` 用于库文档，不用于项目源码搜索。
- `mysql` 只读优先，避免无明确授权的数据变更。
- `github` 操作涉及提交、推送、PR、评论时，先确认用户意图。
- `idea` 适合 IDE 上下文和数据库连接信息，不替代 `codegraph` 的仓库级结构分析。
- 不把 MCP 返回的密钥、token、连接串、真实生产地址写入文件或回复。

## Skills

本仓库内置本地 skills，路径在 `.codex/skills/`。目录名、`SKILL.md` 的 `name` 和 `$skill` 调用名保持一致：

| 目录 | Skill 调用名 | 适用场景 |
| --- | --- | --- |
| `using-peach-code-skeleton` | `using-peach-code-skeleton` | `peach-cloud` 代码编写/审查的第一准则；约束 REST、Entity、DAO/DAO XML、Service、common/peach-common 的骨架与分层 |
| `using-peach-readme-writer` | `using-peach-readme-writer` | 编写或刷新根 README、模块 README、starter 文档 |
| `using-peach-rocket` | `using-peach-rocket` | `RocketMQ`、`peach-rocket-*`、`@MqEvent`、`@MqConsumer`、Outbox、幂等、事务消息 |
| `using-peach-storage` | `using-peach-storage` | `peach-storage`、`peach-store-*`、`StorageTemplate`、provider、路径安全、分片、前端直传 |
| `using-peach-threadpool` | `using-peach-threadpool` | `peach-threadpool`、`@AsyncExecuted`、`ThreadPoolManager`、线程池配置、上下文传递 |

Skill 使用规范：

- 命中 skill 场景或用户显式提到 `$skill-name` 时，先完整读取对应 `SKILL.md`。
- 编写、生成、重构或审查 Java 分层代码时，先用 `using-peach-code-skeleton`；涉及 README、RocketMQ、Storage、Threadpool 时再叠加对应 skill。
- `SKILL.md` 指向 `references/module-guide.md` 时，涉及配置、边界、SPI、示例或 README，必须继续读取参考文件。
- 不要编造 skill 中没有确认过的配置项、默认值、注解语义或 SPI。

## Module Boundaries

通用约束：

- `*-launch`：只放启动类和运行时配置。
- `*-rest`：REST Controller 和接口适配。
- `*-service`：领域服务和业务逻辑。
- `*-entity`：DO、DTO、QO、VO 等模型。
- `*-common`：当前业务域共享对象，避免反向依赖上层模块。
- `*-openfeign-external`：对其他服务暴露的 Feign 客户端。
- `*-autoconfigure`：starter 的核心 API、配置、默认实现、自动配置和 SPI。
- `*-starter`：业务接入依赖聚合，不放复杂业务逻辑。
- `*-example`：可运行示例和覆盖默认实现的样例。

组件和中间件接入：

- 业务模块优先依赖 `*-starter`，不要直接依赖 `*-autoconfigure`。
- RocketMQ 业务接入优先使用 `MqPublisher`、`@MqEvent`、`@MqConsumer`、`MqMessageHandler<T>`。
- Storage 业务接入统一使用 `StorageTemplate`，不要直接调用厂商 SDK。
- Threadpool 异步任务优先使用 `ThreadPoolManager` 或方法级 `@AsyncExecuted`，不要随手 `new Thread` 或创建游离线程池。
- `peach-common` 只承载公共基础能力，不承载具体业务域逻辑。

## Rule Responsibilities

详细规则拆分在 `.codex/rules/` 和 `.codex/skills/` 中，避免在入口文件重复维护：

- Java 版本、框架版本、编码：见 `.codex/rules/05-language-and-encoding.md`。
- 分层模型、REST、Service、DAO/XML、common 归属：见 `using-peach-code-skeleton` 及其 `references/`。
- 注释、Javadoc、日志：见 `.codex/rules/07-comments-and-logging.md`。
- README 和模块文档：见 `.codex/rules/04-documentation-and-readme.md` 与 `using-peach-readme-writer`。
- RocketMQ、Storage、Threadpool 专项边界：先读对应 module skill，再按需读 `references/module-guide.md`。
- 公共 API、生成器、开放接口、公共响应对象等高影响区域：改动前先确认影响面。

## Verification

后端常用命令：

```bash
mvn clean validate -Pdevelopment
mvn clean package -DskipTests -Pdevelopment
```

按模块构建：

```bash
mvn -pl peach-auth -am clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-storage -am clean package -DskipTests -Pdevelopment
mvn -pl peach-middleware/peach-rocket -am clean package -DskipTests -Pdevelopment
```

按模块 POM 构建：

```bash
mvn -f "peach-component/peach-threadpool/pom.xml" clean package -DskipTests -Pdevelopment
```

前端：

```bash
cd peach-cloud-front
npm install
npm run build
```

如果无法运行验证命令，最终回复必须说明原因和残余风险。

## Safety And Secrets

- 不提交真实生产地址、密钥、token、签名 URL、数据库密码、对象存储密钥。
- 日志和 README 中涉及 `access-key`、`secret-key`、SMTP 授权码、签名 URL 时必须脱敏。
- SQL 和数据库操作默认只读或本地开发环境；写操作必须有用户明确授权。
- 不执行 `git reset --hard`、`git checkout --`、递归删除等破坏性操作，除非用户明确要求。
- 工作区可能已有用户改动，不要回退或覆盖无关文件。

## Git And Change Scope

- 默认只修改用户要求范围内的文件。
- 修改前先确认当前文件是否已有未提交改动，并与之协作，不覆盖用户工作。
- 文档任务只改文档；代码任务只改必要代码、测试和相关文档。
- 最终回复说明改了哪些文件、验证了什么、哪些命令未运行。

## Output Expectations

- 回复优先中文，除非用户明确要求英文。
- 先回答结论，再给必要依据，避免把过程性噪音写成大段汇报。
- 涉及代码结论时尽量给出文件路径或符号名，避免空泛描述。
- 未验证的推断必须明确标注为推断，不要把猜测写成事实。
- 发现仓库规则、skill 指南和用户要求冲突时，以用户最新要求为准，并说明取舍。

## Encoding

- 项目源码和文档使用 UTF-8。
- 中文 README 必须正常显示中文，不能保留乱码片段。
- 如果终端或 PowerShell 输出出现乱码，不要立刻覆盖源文件；先判断是控制台编码问题还是文件实际损坏。
- 修改中文源码、注释、Markdown、YAML、properties 后，必须自检是否出现异常拉丁字符组合、Unicode replacement character（U+FFFD）等典型 mojibake 片段。
- 新增文件默认使用 LF 内容即可；Git 在 Windows 上可能提示 CRLF 替换，不为此单独改动全仓换行。
