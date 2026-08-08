# peach-cloud Cursor Configuration

本目录是 peach-cloud 的 Cursor 原生项目配置，所有内容均为仓库内真实文件，不使用符号链接。

- `rules/*.mdc`：Cursor Project Rules，按 frontmatter 自动常驻或按任务匹配。
- `skills/*/SKILL.md`：Cursor Project Skills；项目专项 Skill 已按 peach-cloud 工作流适配，通用设计 Skill 保留完整 references、scripts 和 data。
- `mcp.json`：参考 `.codex/config.toml` 的服务清单转换为 Cursor MCP 配置；secret 仅从环境变量读取。

Cursor 应优先使用本目录。Codex 继续使用 `.codex`，两套客户端配置互不跨目录加载。
