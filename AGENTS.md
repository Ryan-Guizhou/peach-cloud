# Peach Cloud Agent Guide

本文件是 peach-cloud 的仓库级 Agent 工作引擎，也是跨 Codex、Cursor 及其他兼容 Agent 的唯一总入口。

## 1. Source of Truth

技术事实按以下优先级确认：

1. 当前分支源码、测试、POM、配置、SQL 与可执行 quickstart。
2. 当前依赖版本的官方文档。
3. 当前仓库正式文档。
4. `.agents/skills` 中的共享 Skill。
5. Agent 专属运行时配置。
6. 历史实现、历史 README 与模型记忆。

源码与 README、Skill 或历史实现冲突时，以当前分支源码为准。禁止根据旧文档、Skill、相邻模块或模型记忆推断项目已经实现某项能力。

## 2. Project Baseline

- 后端：Java 21，Maven 多模块，Spring Boot `3.5.4`，Spring Cloud `2025.0.0`，Spring Cloud Alibaba `2025.0.0.0`。
- 前端：`peach-cloud-front`，Vue 3 + TypeScript + Vite，独立 npm 工程，不属于 Maven reactor。
- 具体依赖版本始终以当前根 `pom.xml`、`peach-dependencies` 与 `package.json` 为准。

## 3. Work Engine

所有 Agent 使用同一工作引擎：

```text
Task Intake
  -> Complexity Gate
     -> SIMPLE: 直接取证
     -> COMPLEX: grill-me -> grilling -> 用户确认
  -> Evidence Routing
  -> Skill Routing
  -> Implementation
  -> Verification
  -> Documentation when affected
  -> Delivery
```

### 3.1 Complexity Gate

满足以下条件时可判定为 SIMPLE，并跳过 Grill：

- 需求明确且只有一个明显合理实现。
- 影响范围局部。
- 不改变公共 API、SPI、配置契约、数据模型、权限、安全边界、并发/事务模型。
- 不涉及模块拆分、跨模块重构、迁移策略或兼容性取舍。
- 不存在需要用户决策的隐含边界。

典型 SIMPLE：明确 bug、拼写/文案、确定性的 import/type 修复、小范围测试补充、无行为变化机械修改。

命中以下任一项即为 COMPLEX：

- 新功能、架构调整、模块拆分/合并。
- 公共 API / SPI / 配置契约 / 数据库模型变化。
- 权限、认证、安全边界变化。
- 并发、事务、资源生命周期变化。
- 跨模块重构、迁移策略、兼容性取舍。
- 存在多个合理方案或需求边界不清。
- 用户明确要求 grill / stress-test / 先确认边界。

无法判断时按 COMPLEX 处理。

### 3.2 Grill Gate

COMPLEX 任务在修改代码前必须使用 `grill-me`，由其调用 `grilling`。

Grill 必须形成 design tree，按 frontier 分轮确认；环境事实由 Agent 自己查证，不把可查事实反问给用户。

frontier 清空后输出 Implementation Contract，至少包含：

- Goal
- Scope
- Non-goals
- Compatibility
- Constraints
- Migration
- Verification
- Risks

只有用户明确回复“确认 / 开始 / 可以实现”等授权后，才能进入实现阶段。用户已明确授权继续实现的当前任务，不重复 Grill。

## 4. Minimal Context

只加载当前任务真正需要的 Skill 和源码。

- Java 修改：`peach-java-engineering`；命中复杂领域时再叠加一个 Domain Skill。
- 前端修改：`using-peach-front`；视觉/交互任务才叠加设计 Skill。
- README、Design、Requirement、Tutorial、Reference、ADR：`project-doc-engineer`。
- 用户要求需求/方案拷问：`grill-me`。
- 代码 + 文档：先完成代码事实与行为验证，再进入文档工作流。

默认一个主 Domain Skill。只有真实跨领域耦合才增加第二个，禁止无意义 Skill 链式叠加。

## 5. Skill Routing

共享 Skill 的唯一事实源是 `.agents/skills/<skill-name>/SKILL.md`。

| 任务范围 | Skill |
| --- | --- |
| Java 工程规范、Javadoc、日志、事务/资源基础规则 | `peach-java-engineering` |
| Virtual Thread、背压、Permit、取消、关闭 | `using-peach-virtual-thread` |
| Scheduler、Claim、执行侧、Provider、Transport | `using-peach-scheduler` |
| RocketMQ、事件、消费、事务消息、Outbox | `using-peach-rocket` |
| Storage、对象路径、Provider、直传、分片 | `using-peach-storage` |
| Redis / Redisson、缓存、Stream、锁、队列 | `using-peach-redis` |
| Email、SMTP、模板、幂等、附件 | `using-peach-email` |
| Vue 3、路由、权限、Pinia、Axios、UI | `using-peach-front` |
| README、Design、Requirement、Tutorial、Reference、ADR | `project-doc-engineer` |
| 复杂需求、方案、设计边界确认 | `grill-me` |

没有对应 Domain Skill 时依赖当前源码与共享工程 Skill，不为了覆盖模块数量新增低价值 Skill。

## 6. Agent-specific Adapters

共享规则和 Skill 不得复制到 `.codex/skills` 或 `.cursor/skills`。

允许保留 Agent 原生运行时适配：

- `.codex/config.toml`：Codex MCP / approval / runtime 配置。
- `.cursor/mcp.json`：Cursor MCP 配置。
- `.cursor/rules/*.mdc`：仅允许薄适配，例如通过 `globs` 将特定文件类型路由到共享 Skill；不得复制共享规范正文。

Agent 专属目录只负责“怎么接入”，共享行为定义由本文件和 `.agents/skills` 负责。

## 7. MCP Routing

MCP 是证据缺口补全工具，不是默认前置步骤。

### CodeGraph

用于 Java / TypeScript 符号引用、caller/callee、跨模块调用链、公共 API 影响面、Starter 自动装配链。默认只分析目标符号上下 1~2 层；影响面分析才扩大。

### Context7

仅在第三方框架或 SDK 的当前版本 API、配置、生命周期或行为存在不确定性时使用。先从 POM/package.json 确认实际版本。

### GitHub

仅用于远程仓库、Branch、Commit、PR、Issue、Release、Actions 和明确授权的远程协作动作。

### Evidence Escalation

`Current File -> Related Source/Test/Config/POM -> CodeGraph -> Context7 -> GitHub Remote`

一个工具已经解决问题时停止升级。

## 8. Starter Architecture

`peach-component` 与 `peach-middleware` 下可复用 Starter 统一采用：

```text
peach-<family>/
├── README.md
├── README.en-US.md
├── peach-<capability>-autoconfigure/
├── peach-<capability>-starter/
└── peach-<capability>-quickstart/
```

- `*-autoconfigure`：公共契约、ConfigurationProperties、自动配置、默认 Bean、SPI/Provider 装配和 metadata。
- `*-starter`：业务接入的最小依赖聚合。
- `*-quickstart`：可运行最小接入样例；生产模块不得反向依赖 quickstart。

不为了目录对称拆模块。非业务 Starter 家族禁止新增 `example` / `*-example`；示例统一使用 `quickstart`。

## 9. Engineering Hard Rules

优先级：`Correctness > Security > Maintainability > Consistency`。

- 公共 API、公共模型、DAO/XML、配置契约和 Starter 装配变化必须检查影响面。
- 不复制事务失效、资源泄漏、敏感日志、跨层依赖、静默吞错等历史错误。
- Spring Bean 使用构造器注入。
- 中文 Javadoc、English log；类型级 Javadoc 保留 `@Author / @Version / @CreateTime`。
- 所有文本文件 UTF-8 无 BOM。
- secret、token、password、私钥、签名 URL 等敏感数据不得进入源码、日志、文档、测试快照或回复。

## 10. Documentation

Documentation is part of the product.

- 技术事实必须追溯到 source、configuration、test、POM、SQL、quickstart 或当前版本官方文档。
- 无法证明的事实不写入正式文档。
- `README.md` 为中文主文档，`README.en-US.md` 为英文等价文档；有效内容变更必须同步。
- README 只承担入口、定位、Quick Start、关键边界和深入阅读；复杂实现下沉 `docs/`。
- 架构、流程、调用链、状态、生命周期、部署和数据模型优先用 Mermaid / PlantUML / draw.io。

## 11. Verification

所有任务完成后执行：

```bash
node scripts/verify-changes.mjs
```

行为代码变更仍需按统一入口提示执行受影响模块最小充分的 Maven/npm 编译、测试或构建。无法执行的检查必须说明原因和残余风险。

## 12. Output Contract

最终只报告：实际修改、关键设计/影响面、实际验证结果、未验证项与残余风险。不要输出 Rule/Skill/MCP 加载流水账。
