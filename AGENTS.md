# Peach Cloud Agent Guide

本文件只定义 peach-cloud 的仓库级事实来源、最小上下文路由、MCP 使用边界和交付门禁。具体编码风格由 Rules 管理；领域实现约束由对应 Skill 管理；文档生产统一由 `project-doc-engineer` 管理。

## 1. Source of Truth

技术事实按以下优先级确认：

1. 当前分支源码、测试、POM、配置、SQL 与可执行 quickstart。
2. 当前依赖版本的官方文档。
3. 当前仓库正式文档。
4. Rules / Skills。
5. 历史实现、历史 README 与模型记忆。

源码与 README、Skill、历史实现冲突时，以当前分支源码为准。禁止根据旧文档、Skill 或记忆推断项目已经实现某项能力。

## 2. Project Baseline

- 后端：Java 21，Maven 多模块，Spring Boot `3.5.4`，Spring Cloud `2025.0.0`，Spring Cloud Alibaba `2025.0.0.0`。
- 前端：`peach-cloud-front`，Vue 3 + TypeScript + Vite，独立 npm 工程，不属于 Maven reactor。
- starter 组件优先采用 `autoconfigure / starter / quickstart` 结构；是否存在额外 `core/provider/transport` 必须由真实隔离职责决定。
- 具体依赖版本始终以当前 `pom.xml`、`peach-dependencies` 与 `package.json` 为准。

## 3. Minimal Context

只加载当前任务真正需要的 Rule、Skill 和源码，不为了“保险”读取全部规则、全部 Skill 或整个仓库。

- 普通 Java 修改：读取 Java Rule；涉及领域组件时再读取一个对应 Domain Skill。
- 前端修改：读取 Frontend Rule + `using-peach-front`；只有视觉/交互任务才按需叠加设计 Skill。
- README、设计文档、需求文档、新手指引、Reference、ADR、排障文档：使用 `project-doc-engineer`。
- 代码 + 文档：先完成代码事实与验证，再进入文档工作流；文档阶段不重复加载无关领域材料。

一次任务默认只使用一个主 Domain Skill。跨领域事实确实耦合时才增加第二个，避免 Skill 链式叠加。

## 4. MCP Routing

MCP 是“证据缺口补全工具”，不是每个任务的默认前置步骤。优先使用当前文件和仓库证据；只有出现明确证据缺口时才触发对应 MCP。

### CodeGraph

在以下情况使用：

- 判断 Java / TypeScript 符号引用关系、调用方或被调用方。
- 分析跨模块调用链、公共 API 影响面、starter 自动装配链或生命周期关系。
- 需要确认“改这个符号会影响谁”，且单文件阅读不足以回答。

不要在以下情况机械调用：

- 纯 README 文案、SQL、脚本或静态配置调整。
- 已知文件内的局部实现且依赖关系清晰。
- 当前源码、测试或 POM 已经能直接证明结论。

约束：

- 查询范围限定到 peach-cloud 仓库。
- 默认只分析目标符号上下 `1~2` 层关系；影响面分析才扩大。
- 只保留当前任务需要的 symbol、file、caller/callee 与关键关系，不把大图原样灌入上下文。
- CodeGraph 用于结构关系；最终运行事实仍由源码、配置和测试确认。

### Context7

仅在第三方框架或 SDK 的当前版本 API、配置或行为存在不确定性时使用，例如 Spring Boot、Spring Cloud、MyBatis、RocketMQ、Redis/Redisson、存储 SDK、Vue/Vite 等。

- 先从当前 POM/package.json 确认实际版本，再查对应版本资料。
- 不使用 Context7 推断 peach-cloud 自身的业务语义。
- 当前源码已经明确第三方调用方式时，不重复查询。

### GitHub

仅在以下场景使用：

- 用户明确要求查看远程仓库、PR、Issue、Commit、Branch、Release 或 Actions。
- 需要比较远程分支/提交，或确认远程仓库最新状态。
- 需要创建 PR、评论、合并或执行其他远程协作动作。

本地/当前分支源码足够时，不额外调用 GitHub 重复取证。Push、PR/Issue 修改、评论、合并等远程写操作必须有用户明确授权。

### Evidence Escalation

默认按下列顺序补证据，并跳过无关步骤：

`Current File -> Related Source/Test/Config/POM -> CodeGraph -> Context7 -> GitHub Remote`

如果一个工具已经解决当前证据缺口，不继续调用其他 MCP 做重复验证。

## 5. Skill Routing

Domain Skill 只保存领域中容易写错的非通用规则，不重复 Java 风格、UTF-8、构建、日志或 README 通用规范。

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

不存在对应 Domain Skill 时，优先依赖当前源码和通用 Rule，不为了覆盖所有模块而新建低价值 Skill。

## 6. Engineering Principles

优先级：`Correctness > Security > Maintainability > Consistency`。

- peach-cloud 采用统一目标风格，不再根据相邻历史代码临时选择风格。
- 公共 API、公共模型、DAO/XML、配置契约、starter 装配变化必须检查受影响调用方。
- 不复制已知错误的历史模式，包括事务失效、资源泄漏、错误日志级别、敏感数据泄露、跨层依赖与无效注解。
- Spring Bean 使用构造器注入；具体 Java 规则由 Java Rule 定义。
- 中文 Javadoc、English log；类型级 Javadoc 保留并统一 `@Author / @Version / @CreateTime`。
- 所有文本文件使用 UTF-8 无 BOM。
- 禁止把 secret、token、password、私钥、签名 URL、身份证号等敏感数据写入源码、日志、文档、测试快照或回复。

## 7. Documentation Principles

Documentation is part of the product.

- 所有技术事实必须能追溯到 source、configuration、test、POM、SQL、quickstart 或当前版本官方文档。
- 无法证明的事实不得写入正式文档；规划能力明确标记 `Planned`，实验能力标记 `Experimental`，已废弃能力标记 `Deprecated`。
- `README.md` 为中文主文档，`README.en-US.md` 为英文等价文档；有效内容变更必须同步。
- README 只承担入口、定位、Quick Start、关键边界和深入文档导航；复杂实现原理下沉到 `docs/`。
- 小模块不要机械创建一套空洞 docs；文档数量由真实复杂度决定。
- 架构、关系、流程、调用链、状态、生命周期、部署、数据模型优先用 Mermaid / PlantUML / draw.io 表达；简单列表和配置不要为了“图文并茂”硬画图。

## 8. Verification

优先运行：

```bash
node scripts/verify-changes.mjs
```

代码变更还应运行受影响模块的编译/测试；前端变更运行对应 npm 类型检查、测试或构建。无法执行的检查必须说明原因和残余风险，不得声称已经验证。

## 9. Output Contract

最终回复只保留对用户有价值的内容：

1. 实际修改内容。
2. 关键设计与影响面。
3. 实际运行的验证及结果。
4. 未验证项与残余风险。

不要输出 Rules/Skills/MCP 的加载流水账，也不要粘贴大段工具原始结果。
