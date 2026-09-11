# peach-cloud Cursor Configuration

本目录是 peach-cloud 的 Cursor 原生项目配置，与 `.codex` 保持语义同步但不使用符号链接。

- `rules/*.mdc`：仅保留 Java、Frontend、Documentation、Security、Quality 五类通用规则。
- `skills/*/SKILL.md`：只保留真正需要领域知识或文档工作流的 Skills。
- `mcp.json`：项目 MCP 配置；secret 仅通过环境变量提供。

MCP 的触发时机统一由根目录 `AGENTS.md` 约束：CodeGraph 用于项目内部结构/调用关系，Context7 用于第三方当前版本事实，GitHub 用于远程仓库状态与协作操作。不要把 MCP 当作所有任务的默认前置步骤。
