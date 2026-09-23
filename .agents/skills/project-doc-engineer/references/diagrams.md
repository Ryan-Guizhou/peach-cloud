# Diagram Rules

## 选择

| 信息 | 优先图 |
| --- | --- |
| 模块/组件关系 | Mermaid flowchart / C4 / draw.io |
| 请求/异步调用链 | sequenceDiagram |
| 状态迁移 | stateDiagram-v2 |
| 数据实体关系 | ER diagram |
| 生命周期/任务处理 | flowchart + 必要的 sequence |
| 部署拓扑 | Mermaid/draw.io deployment style |
| 简单配置/枚举 | Table，不画图 |

优先 Mermaid，因为可 diff、可搜索、可维护；表达复杂企业架构或需要手工布局时用 draw.io；PlantUML 用于已有 PlantUML 生态或类/时序表达更合适的场景。

## 质量

- 一张图只表达一个主要问题。
- 节点名使用项目真实模块/组件名。
- 不加入源码不存在的服务、队列、数据库或状态。
- 主路径清晰，异常路径只保留理解设计所需的关键分支。
- 图后补 2~5 条关键结论，不用一段文字重新复述所有箭头。
