---
name: using-peach-scheduler
description: 修改或审查 peach-component/peach-scheduler 与 peach-scheduled 控制面协作时使用，覆盖 @PeachJob、JobHandler、PeachJobExecutor、ExecutionLeaseClient、Claim、ExecutionResultReporter、SchedulingProvider、Quartz Provider、RocketMQ Transport、Outbox、执行幂等和虚拟线程 scheduler 分组。
---

# Peach Scheduler

先区分角色：`peach-scheduler` 是嵌入业务服务的执行侧 SDK/Provider/Transport；`peach-scheduled` 是任务定义、状态与调度控制面。不要把 Trigger 与业务 Handler 执行混在同一职责。

## 核心不变量

- Trigger 与 Execute 分离：调度引擎负责触发，业务 Handler 在目标业务服务执行。
- 执行命令在真正运行 Handler 前必须经过 Claim/Lease；重复 MQ 消息不能绕过 Claim 直接执行。
- Claim 不能替代业务幂等；At-Least-Once 场景下 Handler 对稳定业务键或 executionId 保持幂等。
- 执行结果上报、Outbox 和消费幂等不能宣称跨数据库/MQ Exactly-Once。
- Scheduler 阻塞 Handler 使用 `peach-virtual-thread` 的 `scheduler` 业务组时，必须尊重其并发、Pending、拒绝、取消和关闭语义。
- Quartz Provider 是调度实现，不成为业务数据事实源；控制面任务/执行状态仍由 Peach 侧模型治理。
- 新 Provider 实现现有 `SchedulingProvider` 等 SPI，保持稳定 providerId，并独立验证 schedule/reschedule/pause/resume/delete/trigger 生命周期。

## 修改检查

执行链变化检查：command -> consumer -> `PeachJobExecutor` -> claim -> executor -> handler -> result reporter。

Transport 变化同时检查 `using-peach-rocket`；虚拟线程执行边界变化同时检查 `using-peach-virtual-thread`。只有真实跨领域修改才叠加第二个 Skill。

需要文档时进入 `project-doc-engineer`。
