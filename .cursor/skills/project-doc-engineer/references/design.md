# Technical Design Pattern

Design 面向维护者，解释“为什么这样设计、关键约束是什么、失败时如何表现”，而不是重新逐行描述代码。

## 推荐结构

按复杂度裁剪：

1. Background / Problem。
2. Goals / Non-goals。
3. Context & Constraints。
4. Architecture。
5. Key Components & Responsibilities。
6. Main Flow / Sequence。
7. State / Lifecycle（存在状态时）。
8. Data / Transaction / Consistency（涉及数据时）。
9. Concurrency / Backpressure / Cancellation（涉及异步时）。
10. Security。
11. Failure Handling / Recovery。
12. Observability。
13. Configuration / Extension Points。
14. Alternatives & Trade-offs。
15. Testing / Verification。
16. Migration / Compatibility（需要时）。

## Design Decision

重要取舍要写：Context -> Decision -> Consequence。避免“采用 X，因为 X 性能好”这类无证据口号。

复杂组件至少有一张总体架构或主流程图；存在并发、状态、生命周期、异步链路时增加对应时序/状态图，但不要重复表达同一关系。
