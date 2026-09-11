# peach-threadpool

[English](README.en-US.md) | 中文

`peach-threadpool` 是平台线程池的存量兼容组件，保留 `ThreadPoolManager`、`@AsyncExecuted` 与 `peach.threadpool` 契约。新建阻塞 IO 异步能力优先迁移到 `peach-virtual-thread`。

## 结构

| 模块 | 职责 |
| --- | --- |
| `peach-threadpool-autoconfigure` | 配置、管理器、注解切面和默认线程池实现 |
| `peach-threadpool-starter` | 存量业务接入依赖入口 |
| `peach-threadpool-quickstart` | 兼容行为最小验证，不作为新业务模板 |

Quickstart：[`peach-threadpool-quickstart`](peach-threadpool-quickstart/README.md)。

## 当前边界

- 平台线程池仍适用于 CPU 密集或明确需要固定工作线程资源的场景。
- 阻塞 IO 新代码优先使用 `peach-virtual-thread` 的分组并发、Pending 和背压模型。
- 线程池上下文传播不能被理解为 Spring 事务跨线程传播。
- 队列、拒绝、超时和取消语义必须以当前实现为准，不从旧示例复制。
