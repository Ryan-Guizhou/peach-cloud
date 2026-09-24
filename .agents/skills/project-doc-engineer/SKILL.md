---
name: project-doc-engineer
description: 基于当前项目源码和可验证证据创建、重构、审查或同步高质量项目文档。用于 README、README.en-US.md、需求文档、技术/架构设计、ADR、Getting Started、新手指引、How-to、API/配置 Reference、部署运维、排障文档，以及需要 Mermaid、PlantUML、draw.io 表达架构、流程、时序、状态、数据模型的任务。适用于 peach-cloud 和其他软件项目；文档必须准确反映当前代码、配置、测试、POM/依赖和业务边界。
---

# Project Documentation Engineer

## 核心原则

文档是产品和代码契约的一部分。先证明事实，再组织文档；不能证明的能力不写成“已支持”。成熟项目的信息架构基准已经沉淀在 references 中，日常任务直接复用，不重复联网调研。

## 工作流

1. 确认读者、文档类型和真实交付目标；不为了“完整”制造无用文档。
2. 确定模块边界并建立 Evidence Matrix，再读取最小必要源码、配置、测试、POM、SQL、quickstart。
3. 证据不足时按 `AGENTS.md` 的 MCP Routing 补证据，不在本 Skill 重复工具触发规则。
4. 只读取当前文档类型对应的 reference，不一次加载全部 references。
5. 先设计信息架构，再判断哪些关系必须用图表达。
6. 编写正文；代码、配置、命令必须来自当前证据或本次已实现改动。
7. README 修改时同步中文主文档和英文等价文档。
8. 完成后遵循 `AGENTS.md` 的仓库级 Definition of Done；本 Skill 不维护门禁命令。

## Reference Routing

- README：`references/readme.md`
- 技术设计 / 架构设计 / ADR：`references/design.md`
- 需求 / 功能规格：`references/requirements.md`
- Getting Started / Tutorial / How-to：`references/tutorial.md`
- API / 配置 / SPI / Troubleshooting：`references/reference.md`
- 图示：`references/diagrams.md`
- 事实核验：`references/evidence.md`
- 文档规范本身调整、首次建立新文档类型、复杂平台级模块或用户明确要求外部对标：`references/benchmarking.md`

## Benchmarking Boundary

普通 README、配置/API 同步、补图、排障和同类 Starter 文档不得默认重新搜索成熟项目；直接使用已沉淀 reference。只有 `references/benchmarking.md` 定义的触发条件成立时才重新对标 2~4 个成熟项目的官方资料，并把稳定结论沉淀回 reference，避免后续重复消耗上下文。

## 文档复杂度

- 简单组件：README + 英文等价 README。
- 中等组件：README + 真正需要的 design / troubleshooting 等少量文档。
- 复杂基础设施/业务：按需要拆分 Getting Started、Design、Reference、Troubleshooting、Migration。

README 只负责“是什么、为什么、如何最小接入、关键边界、深入阅读”。复杂算法、完整配置/API 表、长篇设计权衡和维护者细节进入 `docs/`。

## 事实状态

正式文档区分 `Supported / Experimental / Planned / Deprecated / Unsupported`，Roadmap 不得写成当前能力。

## 图示原则

架构、模块关系、调用链、执行流程、生命周期、状态迁移、数据模型、部署拓扑优先图示。优先顺序：Mermaid -> PlantUML -> draw.io -> 静态 SVG/PNG。图表达关系，文字解释原因、约束和例外；禁止重复图和装饰图。

## README 双语

`README.md` 是中文主文档，`README.en-US.md` 是英文等价文档。功能、配置、API、命令、示例、图、链接和限制发生有效变更时必须同步。复杂 README 可使用相同 `<!-- doc-sync:<section-id> -->` 标记辅助自动校验。

## 质量标准

- class、method、annotation、artifactId、配置 key、默认值、endpoint、SPI、状态和运行行为都有当前证据。
- 不把历史 README、注释、Skill 或模型记忆当成源码事实。
- 不复制成熟项目文案，只吸收信息架构、读者路径和可维护机制。
- 示例优先来自可运行 quickstart/test；不能验证时明确标记。
- 文档帮助目标读者完成任务或建立正确心智模型，不重复源码、堆术语或写营销文案。
