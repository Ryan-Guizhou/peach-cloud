# Peach Cloud Agent Guide

本文件是 peach-cloud 仓库级唯一工作入口，只定义事实优先级、最小上下文路由、MCP 触发边界、Starter 架构和统一交付门禁。具体编码风格由 Rules 管理；领域不变量由 Domain Skill 管理；项目文档统一由 `project-doc-engineer` 管理。

## 1. Source of Truth

技术事实按以下优先级确认：

1. 当前分支源码、测试、POM、配置、SQL 与可执行 quickstart。
2. 当前依赖版本的官方文档。
3. 当前仓库正式文档。
4. Rules / Skills。
5. 历史实现、历史 README 与模型记忆。

源码与 README、Skill 或历史实现冲突时，以当前分支源码为准。禁止根据旧文档、Skill、相邻模块或模型记忆推断项目已经实现某项能力。

## 2. Project Baseline

- 后端：Java 21，Maven 多模块，Spring Boot `3.5.4`，Spring Cloud `2025.0.0`，Spring Cloud Alibaba `2025.0.0.0`。
- 前端：`peach-cloud-front`，Vue 3 + TypeScript + Vite，独立 npm 工程，不属于 Maven reactor。
- 具体依赖版本始终以当前根 `pom.xml`、`peach-dependencies` 与 `package.json` 为准。

## 3. Minimal Context

只加载当前任务真正需要的 Rule、Skill 和源码，不为了“保险”读取全部规则、全部 Skill 或整个仓库。

- 普通 Java 修改：Java Rule；命中复杂领域时再读取一个对应 Domain Skill。
- 前端修改：Frontend Rule + `using-peach-front`；只有视觉/交互任务才叠加设计 Skill。
- README、设计、需求、新手指引、Reference、ADR、排障：使用 `project-doc-engineer`。
- 代码 + 文档：先完成代码事实与行为验证，再进入文档工作流。

一次任务默认一个主 Domain Skill。只有真实跨领域耦合才增加第二个，禁止 Skill 链式叠加。

## 4. MCP Routing

MCP 是证据缺口补全工具，不是默认前置步骤。当前源码已经足够时不要额外调用 MCP。

### CodeGraph

用于：

- Java / TypeScript 符号引用、caller/callee。
- 跨模块调用链、公共 API 影响面。
- Starter 自动装配链、生命周期和依赖关系。

默认只分析目标符号上下 `1~2` 层；影响面分析才扩大。只保留当前任务所需 symbol、path 和关键关系。CodeGraph 证明结构关系，运行事实仍由源码、配置和测试确认。

### Context7

仅在第三方框架或 SDK 的当前版本 API、配置、生命周期或行为存在不确定性时使用。先从 POM/package.json 确认实际版本；不得使用 Context7 推断 peach-cloud 自身业务语义。

### GitHub

仅用于远程仓库、Branch、Commit、PR、Issue、Release、Actions 和明确授权的远程协作动作。本地/当前分支证据足够时不重复查询 GitHub。

### Evidence Escalation

按实际缺口选择最短路径：

`Current File -> Related Source/Test/Config/POM -> CodeGraph -> Context7 -> GitHub Remote`

一个工具已经解决问题时停止升级，不做重复取证。

## 5. Skill Routing

Domain Skill 只保存领域中容易写错的非通用规则，不重复 Java 风格、UTF-8、日志、构建或文档门禁。

| 任务范围 | Skill |
| --- | --- |
| Virtual Thread、背压、Permit、取消、关闭 | `using-peach-virtual-thread` |
| Scheduler、Claim、执行侧、Provider、Transport | `using-peach-scheduler` |
| RocketMQ、事件、消费、事务消息、Outbox | `using-peach-rocket` |
| Storage、对象路径、Provider、直传、分片 | `using-peach-storage` |
| Redis / Redisson、缓存、Stream、锁、队列 | `using-peach-redis` |
| Email、SMTP、模板、幂等、附件 | `using-peach-email` |
| Vue 3、路由、权限、Pinia、Axios、UI | `using-peach-front` |
| README、Design、Requirement、Tutorial、Reference、ADR | `project-doc-engineer` |

没有对应 Domain Skill 时依赖当前源码和通用 Rule，不为了覆盖模块数量新增低价值 Skill。

## 6. Starter Architecture

`peach-component` 与 `peach-middleware` 下可复用 Starter 能力统一采用以下对外结构：

```text
peach-<family>/
├── README.md
├── README.en-US.md
├── peach-<capability>-autoconfigure/
├── peach-<capability>-starter/
└── peach-<capability>-quickstart/
```

职责：

- `*-autoconfigure`：公共契约、`ConfigurationProperties`、自动配置、默认 Bean、SPI/Provider 装配和配置 metadata；不放 demo。
- `*-starter`：业务接入的最小依赖聚合；不承载复杂实现和示例代码。
- `*-quickstart`：可运行最小接入样例；可以依赖 starter，生产模块不得反向依赖 quickstart；必须提供无需生产外部基础设施即可执行的最小自动化测试。

`core`、`common`、`provider-*`、`transport-*` 等模块只有存在真实独立职责时保留，不为了目录对称拆模块。

非业务 Starter 家族禁止新增 `example` / `*-example` 模块；示例统一使用 `quickstart`。业务服务自己的 sample/test fixture 不受该命名约束。

Starter 家族主 README 负责定位、模块关系、最小接入、QuickStart 运行方式、关键边界和深入文档导航；复杂设计进入 `docs/`。中文 `README.md` 与英文 `README.en-US.md` 必须语义等价同步。QuickStart 不维护独立 README，只保留完成最小闭环所需代码、配置和自动化测试，不复制生产配置或真实凭据。

## 7. Engineering Principles

优先级：`Correctness > Security > Maintainability > Consistency`。

- peach-cloud 采用统一目标风格，不再按相邻历史代码临时选择风格。
- 公共 API、公共模型、DAO/XML、配置契约和 Starter 装配变化必须检查影响面。
- 不复制事务失效、资源泄漏、敏感日志、跨层依赖、静默吞错等历史错误。
- Spring Bean 使用构造器注入。
- 中文 Javadoc、English log；类型级 Javadoc 统一保留 `@Author / @Version / @CreateTime`。
- 所有文本文件使用 UTF-8 无 BOM。
- secret、token、password、私钥、签名 URL 等敏感数据不得进入源码、日志、文档、测试快照或回复。

## 8. Documentation Principles

Documentation is part of the product.

- 技术事实必须能追溯到 source、configuration、test、POM、SQL、quickstart 或当前版本官方文档。
- 无法证明的事实不写入正式文档；明确区分 `Supported / Experimental / Planned / Deprecated / Unsupported`。
- `README.md` 为中文主文档，`README.en-US.md` 为英文等价文档；有效内容变更必须同步。
- README 只承担入口、定位、Quick Start、关键边界和深入阅读；复杂实现下沉 `docs/`。
- 文档数量由复杂度决定，不为模板齐全制造空文件。
- 架构、关系、流程、调用链、状态、生命周期、部署和数据模型优先用 Mermaid / PlantUML / draw.io；简单信息不硬画图。

## 9. Verification

所有任务完成后统一执行仓库级入口：

```bash
node scripts/verify-changes.mjs
```

这是 Agent 唯一需要知道的门禁命令。其内部子检查、执行顺序和新增检查由脚本维护，Rules 与 Skills 不复制脚本名称或执行命令。

行为代码变更仍需按统一入口给出的提示执行受影响模块最小充分的 Maven/npm 编译、测试或构建。无法执行的检查必须说明原因和残余风险，不得声称已验证。

## 10. Output Contract

最终只报告：实际修改、关键设计/影响面、实际验证结果、未验证项与残余风险。不要输出 Rule/Skill/MCP 加载流水账，也不要粘贴大段工具原始结果。
