---
name: using-peach-threadpool
description: 规范 peach-cloud 项目中 peach-threadpool / peach-threadpool-starter / peach-threadpool-autoconfigure 的线程池配置、PoolType 选择、@AsyncExecuted 使用、ThreadPoolManager 调用、MDC/SecurityContext 传递、拒绝策略、队列容量、超时和 README 编写。Use when editing thread pool code, adding async execution, configuring peach.threadpool, or writing README for peach-component/peach-threadpool.
---

# Peach Threadpool

## 工作流

1. 先判断任务是配置线程池、使用注解、直接提交任务、扩展 starter，还是补充 README。
2. 需要异步执行时优先使用 `ThreadPoolManager` 或 `@AsyncExecuted`，不要随手 `new Thread`、`Executors.newFixedThreadPool`。
3. 涉及配置字段、默认行为或当前实现限制时，读取 `references/module-guide.md`。
4. 改动后运行 `node scripts/check-utf8.mjs`、编译 `peach-component/peach-threadpool` 并执行 `git diff --check`；涉及 AOP 行为时补充或执行相应测试。

## 使用规则

- 配置前缀是 `peach.threadpool`。
- 根据任务性质选择 `PoolType`：CPU 密集选 `CPU`，IO/远程调用选 `IO`，短生命周期突发任务可评估 `CACHED`，定时任务选 `SCHEDULED`。
- 队列容量不要无脑设置极大值；高吞吐场景要在延迟、内存和拒绝策略之间明确取舍。
- 拒绝策略优先选择可解释的行为：关键任务用 `CALLER_RUNS` 做背压，需要快速失败用 `ABORT`，可丢弃任务才使用 `DISCARD` 或 `DISCARD_OLDEST`。
- 线程名前缀必须能定位业务域，例如 `order-async-`、`storage-upload-`。
- 默认启用 MDC 和 SecurityContext 传递；关闭前必须确认日志追踪和权限上下文不受影响。

## @AsyncExecuted 当前语义

- 注解类路径为 `com.peach.threadpool.annoation.AsyncExecuted`，注意包名当前拼写是 `annoation`。
- 注解可配置 `type`、`async`、`timeoutMs`。
- 当前 AOP 只切方法注解，不切类注解；不要只在类上标注后假设所有方法生效。
- 当前普通返回值路径会提交到线程池后执行 `Future.get()`；这会让调用线程等待结果，不是 fire-and-forget。
- 需要真正异步返回时，优先让方法返回 `CompletableFuture`，并检查当前实现是否正确使用目标线程池。
- `timeoutMs > 0` 只限制等待结果的时间，不等于可靠取消底层任务。

## 代码审查重点

- 检查是否创建了游离线程池且没有关闭。
- 检查耗时 IO 是否误用 CPU 池。
- 检查 `queueCapacity` 是否掩盖过载并导致延迟堆积。
- 检查 `async=false` 是否是有意同步执行。
- 检查异常是否被吞掉，尤其是 `Future`、`CompletableFuture` 链路。
- 检查 AOP 自调用是否导致注解不生效。

## README 提醒

编辑 `peach-component/peach-threadpool` 或子模块后，使用 `$using-peach-readme-writer` 刷新 README。README 必须写明当前注解真实语义、配置字段、默认池、拒绝策略、MDC/SecurityContext 传递和已知限制。
