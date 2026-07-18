# peach-cloud Rules

`AGENTS.md` 是仓库入口；本目录只保存可复用的专项规则。Agent 必须按任务路由读取，不得默认把全部规则装入上下文。

| 文件 | 触发场景 |
| --- | --- |
| `01-mcp-and-skills.md` | MCP、历史记忆、第三方文档、skill 选择 |
| `02-output-and-evidence.md` | 结论、证据、验证结果表达 |
| `03-module-and-change-boundaries.md` | 模块归属、跨层联动、改动范围 |
| `04-documentation-and-readme.md` | README 和模块文档 |
| `05-language-and-encoding.md` | Java/框架兼容性、UTF-8 无 BOM |
| `06-layered-java-style.md` | DO/DTO/QO/VO、DAO、REST/Service |
| `07-comments-and-logging.md` | Javadoc、普通日志、操作审计 |
| `08-security-and-quality-gates.md` | 敏感数据、正确性优先级、完成门禁 |

规则级别：`REQUIRED` 必须满足，`PREFERRED` 是新代码目标，`LEGACY_COMPATIBLE` 仅用于存量兼容，`FORBIDDEN` 不得复制。冲突时遵守 `AGENTS.md` 的优先级。
