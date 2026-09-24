# Peach Cloud Agent Workspace

`.agents` 保存可以被不同 Agent 共享的项目级能力。

## 目录

```text
.agents/
├── README.md
├── skills/
│   └── <skill-name>/
│       ├── SKILL.md
│       ├── agents/       # 可选 UI/运行时元数据
│       ├── references/   # 可选参考资料
│       ├── scripts/      # 可选确定性脚本
│       └── assets/       # 可选输出资源
└── evals/
    └── golden-tasks.md
```

## Ownership

- `AGENTS.md`：跨 Agent 工作引擎、硬约束、路由和交付门禁。
- `.agents/skills`：共享专业能力的唯一事实源。
- `.agents/evals`：跨 Agent 行为回归规格。
- `.codex`：只保留 Codex 原生运行时配置。
- `.cursor`：只保留 Cursor 原生运行时配置和必要薄适配 Rule。

禁止在 `.codex/skills`、`.cursor/skills` 再维护共享 Skill 副本。

## Skill 设计原则

1. 优先跨 Agent、跨 IDE 的相对路径和能力描述。
2. 禁止硬编码 `.cursor/skills`、`.codex/skills`、`.claude/skills`。
3. Skill 保存“需要按需加载的专业知识/工作流”，常驻硬约束放 `AGENTS.md`。
4. 一个 Skill 只承担清晰职责，避免重复 Java/安全/质量等公共规则。
5. 新增 Skill 后必须让 `scripts/check-agent-governance.mjs` 能验证引用和目录治理。
