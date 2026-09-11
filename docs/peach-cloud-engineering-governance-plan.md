# Peach Cloud 工程治理与 JDK 21 改造计划

## 目标

本次改造以当前仓库 `AGENTS.md`、`.codex/rules`、`.codex/skills`、源码、测试、POM 与可执行 QuickStart 为事实依据，集中解决 QuickStart 文档归属、示例测试缺失、构建效率、构建 Warning、Java 21 合规和工程治理问题。

## 范围与原则

- QuickStart 只承载可运行最小接入样例和与样例对应的测试，不维护独立 README；说明统一收口到 capability family 根目录 `README.md` / `README.en-US.md`。
- 不为了目录对称拆模块；`core`、`provider-*`、`transport-*` 等只有存在真实独立职责时保留。
- 不为了“看起来现代”机械使用 `record`、`var`、Stream、虚拟线程等 Java 21 特性；只在能提高语义清晰度、正确性或资源管理质量时调整。
- Warning 指 Maven/Javac/插件在真实构建中由项目源码、POM 或项目配置产生的可修复警告；不通过粗暴屏蔽日志或全局 `@SuppressWarnings` 规避。
- 确定性约束进入脚本门禁；领域语义继续由源码、测试和 Domain Skill 证明；Rules/Skills 不复制具体门禁命令。

## 执行阶段

### 1. 规范与门禁对齐

涉及：`AGENTS.md`、`.codex/rules`/`.codex/skills` 中与 Starter/QuickStart/JDK 21 直接相关的条目、`scripts/check-starter-layout.mjs`。

- 明确 family root 是 README 唯一入口，禁止 `*-quickstart/README*.md`。
- 明确 QuickStart 必须具有与最小闭环对应的测试；测试可以是 Spring Boot smoke test 或不依赖外部基础设施的 happy-path test，按实际能力选择，不强制同一模板。
- 检查 Rule/Skill 是否存在与 Java 21、README 归属或统一门禁相冲突的旧约束，仅修正真实冲突。
- 将 QuickStart README 禁止项和测试存在性变成确定性门禁。

### 2. QuickStart 与 Family README 治理

涉及：`peach-component`、`peach-middleware` 下所有符合 Starter family 契约的 capability。

- 删除 QuickStart 中的 `README.md` / `README.en-US.md`。
- 将 QuickStart 中仍有价值且 family root 未覆盖的接入信息迁移到 family root 中英文 README，并保持语义等价。
- 为缺少测试的 QuickStart 增加最小、可重复、无外部基础设施强依赖的测试；若样例本身必须依赖 Redis/RocketMQ/SMTP/Object Storage 等外部服务，则优先测试 Spring 装配边界或替换为可控 fake/mock，而不是让 CI 依赖真实服务。

### 3. 构建效率治理

涉及：Maven reactor、CI workflow、构建辅助脚本。

- 根据 `git diff` 将变化文件映射到最近 Maven module；普通 PR 优先用 `-pl <affected> -am` 构建受影响模块及其上游依赖。
- 根 POM、依赖/BOM/parent、全局插件或无法安全缩小影响面的变化自动回退全量 reactor 验证。
- GitHub Actions 使用 Java 21、Maven dependency cache 和 batch/no-transfer-progress 模式；并行构建只在插件线程安全前提下启用。
- `scripts/verify-changes.mjs` 继续作为仓库确定性门禁唯一入口，不把具体 Maven 命令复制到 Rule/Skill。

### 4. Java 21、注释、日志、注解与 Warning 审计

- 静态检查项目中明显违反 Java Rule 的模式：`System.out/err`、`printStackTrace()`、不必要的字段注入、JDK internal API、遗留 `javax.*`（明确不属于 Jakarta 的标准包除外）、无理由的宽泛 warning suppression 等。
- 对本次触达的 Java 文件按仓库 Java Rule 整理中文 Javadoc、English log、Spring 注入和资源生命周期。
- 以 Java 21 实际 Maven 构建输出为准处理项目可控 Warning；第三方依赖自身输出但当前无法由项目修复的内容单独记录，不伪装成项目零 Warning。
- Java 21 现代化只处理可证明收益的点，不进行无关的大面积语法迁移。

### 5. 项目规划与架构边界

- 保持 `autoconfigure / starter / quickstart` family 契约，并通过门禁防止 production module 依赖 QuickStart。
- 在正式架构文档中补充模块扩张时的创建准则、推荐依赖入口和构建/测试策略，避免模块数量增长后继续线性增加维护成本。
- 复杂架构说明进入 `docs/`，README 保持入口型文档定位。

### 6. 验证与 PR

- 执行仓库统一门禁。
- 在 Java 21 上执行受影响 reactor 的 Maven `verify`；如果改动触发全局构建配置，则执行全量 reactor `verify`。
- PR CI 必须成功；失败则根据真实日志修复后再次验证。
- PR 描述记录：实际改动、影响面、真实构建/测试结果、仍无法验证的外部依赖风险（如有）。

## 完成标准

1. Starter family 的 QuickStart 不再存在 README，且门禁会阻止回归。
2. 所有受本次 family 契约管理的 QuickStart 都有可由 CI 执行的最小测试入口。
3. PR 的工程门禁和 Java 21 Maven 验证均通过。
4. 本次构建中由项目代码/POM/插件配置导致的 Warning 已处理或有可审查的明确例外说明。
5. 代码、注释、日志、注解和 Java 21 调整符合当前仓库规则，没有为了“现代化”引入无收益重构。
6. 构建策略能对普通变更缩小 reactor 范围，对高风险全局变更可靠回退全量验证。
