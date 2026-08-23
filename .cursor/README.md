# peach-cloud Cursor Configuration

本目录是 peach-cloud 的 Cursor 原生项目配置，所有内容均为仓库内真实文件，不使用符号链接。

- `rules/*.mdc`：Cursor Project Rules，按 frontmatter 自动常驻或按任务匹配。
- `skills/*/SKILL.md`：Cursor Project Skills；项目专项 Skill 已按 peach-cloud 工作流适配，通用设计 Skill 保留完整 references、scripts 和 data。
- `mcp.json`：参考 `.codex/config.toml` 的服务清单转换为 Cursor MCP 配置；secret 仅从环境变量读取。

Cursor 应优先使用本目录。Codex 继续使用 `.codex`，两套客户端配置互不跨目录加载。


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
