---
name: using-peach-threadpool
description: 规范 peach-cloud 项目中 peach-threadpool / peach-threadpool-starter / peach-threadpool-autoconfigure 的 Java 21 并发模型、Virtual Thread 适用边界、CPU 有界线程池、@AsyncExecuted、ThreadPoolManager、上下文传递、拒绝策略、队列、超时和 README。Use when editing thread pool code, async execution, virtual threads, task execution, context propagation, or peach.threadpool configuration.
---

# Peach Threadpool

## Highest Priority

- 保留当前 `peach-threadpool` 对外 API、注解语义、配置字段、拒绝策略、上下文传播和监控行为，除非用户明确授权行为升级。
- Java 21 并不意味着删除线程池：**IO concurrency、CPU concurrency、resource concurrency、reliable async 是四个不同问题**。

## Workflow

1. 判断任务属于阻塞 IO、CPU 密集、定时任务、可靠异步、上下文传播或现有 API 兼容。
2. 先读取 `09-java21-modern-style`，再读取 `references/module-guide.md` 核对当前真实实现。
3. 修改 ThreadPoolManager、PoolType、`@AsyncExecuted` 或配置字段前评估所有调用方。
4. 若引入 Virtual Threads，必须明确连接池、Sentinel、Semaphore、RateLimiter 等外部资源边界。
5. 改动后运行 UTF-8、模块测试与 `git diff --check`；并发语义变化必须增加专项测试。

## Java 21 Execution Model

### Blocking IO

优先评估 Virtual Threads，典型场景：HTTP/Feign、JDBC 外围编排、Redis/OSS/File IO、第三方 API。Virtual Thread 解决线程成本，不解决外部资源容量。

### CPU Bound

图片编码、压缩、Hash、加解密、复杂计算等继续使用有界 Platform Thread Pool。线程数与 CPU 核心、任务特征和基准测试关联，不创建无限线程。

### Reliable / Long Running

需要持久化、削峰、可靠重试、跨进程恢复的长任务继续使用 RocketMQ/任务调度系统，不能因为 Virtual Thread 廉价就把可靠异步退化为进程内任务。

### Scheduled

定时任务继续使用受管理 Scheduler；不要用 sleep + Virtual Thread 替代调度系统。

## Existing API Compatibility

- 配置前缀仍是 `peach.threadpool`。
- `@AsyncExecuted` 当前真实语义必须以源码为准；没有完整迁移方案时不得偷偷改变同步等待、返回值、timeout、取消语义。
- `PoolType.IO/CACHED/CPU/SCHEDULED` 等现有值若已对外使用，不能直接删除或改名；可通过内部实现演进或新增模式兼容迁移。
- MDC/SecurityContext/用户上下文传播语义必须保持；Virtual Thread 切换后重点验证 ThreadLocal 生命周期与清理。

## Virtual Thread Guardrails

- 不设置传统“大队列 + 大线程数”来限制 Virtual Thread；有限资源并发使用 Sentinel/Semaphore/连接池等资源级边界。
- 禁止在 `synchronized` 临界区做 DB、Redis、HTTP、File 阻塞 IO。
- 不在业务代码随手创建 `Executors.newVirtualThreadPerTaskExecutor()`；优先由 Spring/peach 执行组件统一托管生命周期、指标和上下文。
- 不把 Virtual Thread 用于期望通过线程数限制 CPU 使用率的任务。

## Review Checklist

- 是否创建游离 Executor 且未关闭。
- IO 是否错误使用 CPU pool；CPU 任务是否错误无限并发。
- 是否改变 queue/rejection/backpressure 语义。
- 是否吞掉 Future/CompletableFuture 异常。
- 是否在异步边界丢失 MDC/SecurityContext/CurrentContext。
- 是否因 Virtual Thread 放大数据库、Redis、HTTP 或第三方服务压力。
- 是否存在 synchronized pinning 风险或锁内阻塞 IO。

## Verification

编辑 `peach-component/peach-threadpool` 后至少执行：

```bash
node scripts/check-utf8.mjs
mvn -pl peach-component/peach-threadpool -am test
git diff --check
```

涉及公共配置/API 时同步使用 `using-peach-readme-writer` 更新 README。