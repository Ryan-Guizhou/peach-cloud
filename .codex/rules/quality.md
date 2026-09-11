# Quality Rule

本 Rule 只定义质量含义，不维护任何门禁命令。门禁触发时机、唯一执行入口和失败处理统一由仓库根 `AGENTS.md` 管理。

- Java 行为修改必须具备与影响面匹配的编译或测试证据。
- 前端行为修改必须具备类型检查、测试或构建证据。
- 文档必须满足事实可追溯、链接有效、中英文 README 同步和图示可维护。
- Agent/Rule/Skill 修改必须保持 Codex/Cursor 对应配置一致且引用存在。
- Starter 结构必须满足仓库级 `autoconfigure / starter / quickstart` 契约。
- UTF-8、空白、Markdown 结构等确定性要求优先交给自动检查，而不是复制到多个 Skill。

自动检查通过不代表业务语义正确；领域正确性仍由源码、测试与 Domain Skill 不变量共同证明。
