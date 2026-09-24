# Requirements Pattern

需求文档面向“为什么做、做什么、如何验收”，不要提前把实现方案写死。

推荐内容：

- 背景与问题。
- 目标与非目标。
- 角色/使用者。
- 业务流程或用户旅程（复杂时画流程图）。
- Functional Requirements：使用稳定编号 `FR-001`。
- Non-functional Requirements：性能、安全、可靠性、兼容性、可观测性。
- 数据与权限边界。
- 外部系统依赖。
- 异常/降级场景。
- Acceptance Criteria：必须可测试、可判定。
- 风险、假设、Open Questions。

避免“高性能、用户友好、企业级”等不可验收描述，除非给出可验证指标或明确行为。
