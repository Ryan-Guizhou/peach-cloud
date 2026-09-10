# README Pattern

本规范吸收成熟开源项目常见信息架构：Spring Boot 倾向让 README/Starter 首页保持简洁并把完整细节导向 Reference；Apache Dubbo 将定位、Architecture、Getting Started 和 Features 放在前部；Redisson 首页优先给 Quick Start、Documentation 与 Code Examples 导航。Peach 文档采用这些模式，而不是复制其文案。

## README 的职责

让读者在几分钟内回答：

1. 这是什么？解决什么问题？
2. 不解决什么？是否适合我的场景？
3. 最小接入怎么做？
4. 最常用 API/配置是什么？
5. 生产使用有什么关键边界？
6. 深入原理和完整 Reference 去哪里看？

## 推荐结构

按实际内容裁剪，不要求所有章节都存在：

1. 标题 + 一句话定位 + 中英文切换。
2. Why / 模块定位。
3. Architecture（只有关系复杂时）。
4. Quick Start：Dependency -> Configuration -> Minimal Code。
5. Core Concepts / API Choice（只保留高频入口）。
6. Production Notes / Limitations。
7. Documentation Navigation。
8. Compatibility / Contributing（只有项目需要时）。

不要把内部状态机、长篇算法、所有配置项、所有 API、全部异常码直接堆在 README。它们属于 Design 或 Reference。

## 示例要求

- Maven 坐标、类名、方法、配置 key 必须来自当前代码。
- Quick Start 应能形成最小闭环，避免展示无关配置。
- 默认值只有从配置代码或测试确认后才能写。
- 中文和英文的代码、配置、命令、图、链接和限制必须语义一致。
