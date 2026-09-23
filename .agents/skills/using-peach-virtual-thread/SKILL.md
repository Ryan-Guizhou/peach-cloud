---
name: using-peach-virtual-thread
description: 修改或审查 peach-component/peach-virtual-thread 的 VirtualExecutorService、DefaultVirtualExecutorService、业务分组、并发控制、Pending、REJECT/BLOCK 背压、Permit 生命周期、Future/CompletableFuture 取消、任务状态、指标、自动配置或优雅关闭时使用。
---

# Peach Virtual Thread

先以当前 `VirtualExecutorService`、`DefaultVirtualExecutorService`、`GroupConcurrencyController`、`PermitLedger`、`VirtualThreadProperties`、受管 Future、自动配置和测试建立事实，不从旧线程池语义反推。

## 核心不变量

- One Task One Virtual Thread；不把虚拟线程重新池化成 worker pool。
- `max-concurrency` 限制真实运行任务，`max-pending` 限制已准入但等待执行的任务。
- `REJECT` 必须显式拒绝；`BLOCK` 必须有 `acquire-timeout` 等有界等待。
- Admission / Execution Permit 只能由拥有它的任务生命周期释放一次，成功、异常、拒绝、取消、中断、关闭都不能泄漏或重复释放。
- `cancel(true)` 可请求中断 runner，但 Execution Permit 只能在 runner 真实退出后释放。
- shutdown 后不接收新任务；优雅关闭与 `shutdownNow` 必须让 Future、任务注册表和 Permit 最终收敛。
- 不跨线程传播 Spring 事务；需要数据一致性时使用明确事务边界、幂等、Outbox/补偿等机制。
- 适合阻塞 IO；CPU 密集任务仍应使用受控平台线程/专用执行资源。

## 修改检查

执行模型变化至少覆盖：success、rejection、block timeout、interruption、cancellation、exception、shutdown、shutdownNow。

公共 API 变化检查接口、默认实现、受管 Future、registry、quickstart 和调用方。配置变化以 `VirtualThreadProperties` 为事实源并同步配置 metadata/tests。

需要文档时进入 `project-doc-engineer`，不要在本 Skill 复制 README 模板。
