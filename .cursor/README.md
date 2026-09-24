# peach-cloud Cursor Configuration

本目录只保存 Cursor 原生运行时适配。

- `mcp.json`：Cursor MCP 配置；secret 仅通过环境变量提供。
- `rules/*.mdc`：只允许保留 Cursor 的薄适配 Rule，例如按 globs 路由到共享 Skill。
- 共享 Skill 的唯一事实源：`.agents/skills`。
- 跨 Agent 工作引擎与硬约束：根目录 `AGENTS.md`。

禁止在 `.cursor/skills` 维护共享 Skill 副本，也不要在 Cursor Rule 中复制 Java、Frontend、Documentation、Security、Quality 等完整规范。
