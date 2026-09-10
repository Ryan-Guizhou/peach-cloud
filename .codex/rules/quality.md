# Quality Gate Rule

能由程序检查的要求交给自动门禁，不在 Skill 中重复占用上下文。

默认执行：

```bash
node scripts/verify-changes.mjs
```

该入口负责组合当前仓库已有或本 PR 新增的检查，包括：

- UTF-8 / BOM。
- `git diff --check`。
- Codex/Cursor Rules 与核心 Skills 同步检查。
- README 中英文成对修改与可选 section marker 同步。
- Markdown fenced code block、相对链接和 Mermaid fence 基础校验。

Java 行为修改另外执行受影响 Maven 模块的 compile/test；前端行为修改执行 `peach-cloud-front` 的类型检查、测试或 build。自动检查通过不代表业务语义正确，仍需结合源码、测试和领域不变量审查。
