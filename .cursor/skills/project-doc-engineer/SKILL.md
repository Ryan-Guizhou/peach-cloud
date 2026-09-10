---
name: project-doc-engineer
description: 基于当前项目源码和可验证证据创建、重构、审查或同步高质量项目文档。用于 README、README.en-US.md、需求文档、技术/架构设计、ADR、Getting Started、新手指引、How-to、API/配置 Reference、部署运维、排障文档，以及需要 Mermaid、PlantUML、draw.io 表达架构、流程、时序、状态、数据模型的任务。适用于 peach-cloud 和其他软件项目；文档必须准确反映当前代码、配置、测试、POM/依赖和业务边界。
---

# Project Documentation Engineer

## 核心原则

文档是产品和代码契约的一部分。先证明事实，再组织文档；不能证明的能力不写成“已支持”。

成熟项目的文档组织模式已经沉淀在本 Skill references 中。日常写文档不要重复联网调研；只有用户明确要求重新对标，或遇到 references 未覆盖的新型文档体系时才补充研究。

## 工作流

1. 确认读者、文档类型和真实交付目标；不为了“完整”制造无用文档。
2. 确定模块边界，建立 Evidence Matrix，再读取最小必要源码、配置、测试、POM、SQL、quickstart。
3. 先解决证据缺口：需要调用关系时使用 CodeGraph；第三方版本行为不确定时使用 Context7；只有远程仓库状态/协作才使用 GitHub。
4. 选择当前文档类型对应的 reference；不要一次性加载全部 references。
5. 先设计信息架构，再决定哪些关系更适合用图表达。
6. 编写正文；代码、配置、命令必须来自当前证据或本次已实现改动。
7. README 有英文版本时同步修改；中文为主文档，英文保持语义等价，不要求逐字直译。
8. 运行 `node scripts/verify-changes.mjs`；失败项修复后再交付。

## Reference Routing

- README：读取 `references/readme.md`。
- 技术设计 / 架构设计 / ADR：读取 `references/design.md`。
- 需求文档 / 功能规格：读取 `references/requirements.md`。
- Getting Started / 新手指引 / Tutorial / How-to：读取 `references/tutorial.md`。
- API / 配置 / SPI / 状态 / 排障 Reference：读取 `references/reference.md`。
- 需要图：读取 `references/diagrams.md`。
- 需要核验事实或文档漂移：读取 `references/evidence.md`。

## 文档复杂度

- 简单组件：README + 英文等价 README 即可。
- 中等组件：README + 真实需要的 design / troubleshooting 等少量文档。
- 基础设施或复杂业务：按实际需要拆分 Getting Started、Design、Reference、Troubleshooting、Migration；禁止为了模板齐全创建空洞文件。

README 只负责“是什么、为什么、如何最小接入、关键边界、深入阅读”。复杂算法、完整 API 表、长篇设计权衡和维护者细节放入 `docs/`。

## 事实状态

正式文档区分：`Supported`、`Experimental`、`Planned`、`Deprecated`、`Unsupported`。Roadmap 不得写成当前能力。

## 图示原则

架构、模块关系、调用链、执行流程、生命周期、状态迁移、数据模型、部署拓扑优先图示。优先顺序：Mermaid -> PlantUML -> draw.io -> 静态 SVG/PNG。简单列表、配置表和线性步骤不强制画图。

图负责表达关系，文字负责解释原因、约束和例外；禁止用重复图或装饰图制造“图文并茂”的假象。

## README 双语

`README.md` 是中文主文档，`README.en-US.md` 是英文等价文档。功能、配置、API、命令、示例、图、链接、限制发生有效变更时必须同时同步。

对于复杂 README，推荐在中英文相同位置使用 `<!-- doc-sync:<section-id> -->` 标记，便于自动校验章节对应关系。

## 质量标准

- 每个类名、方法名、annotation、artifactId、配置 key、默认值、endpoint、SPI、状态和运行行为都有当前证据。
- 不把历史 README、注释、Skill 或模型记忆当成源码事实。
- 不复制目标项目以外成熟项目的文案，只吸收其信息架构和读者路径。
- 示例优先来自可运行 quickstart/test；不能验证时明确标记。
- 文档应该让目标读者完成任务或建立正确心智模型，而不是重复源码、堆术语或写营销文案。
