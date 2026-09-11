# peach-threadpool

English | [中文](README.md)

`peach-threadpool` is the legacy platform-thread-pool compatibility component, preserving `ThreadPoolManager`, `@AsyncExecuted` and the `peach.threadpool` contract. New blocking-I/O asynchronous work should prefer `peach-virtual-thread`.

## Structure

| Module | Responsibility |
| --- | --- |
| `peach-threadpool-autoconfigure` | Properties, manager, annotation aspect and default pool implementation |
| `peach-threadpool-starter` | Legacy business integration dependency entry point |
| `peach-threadpool-quickstart` | Minimal compatibility verification; not a new-business template |

Quickstart: [`peach-threadpool-quickstart`](peach-threadpool-quickstart/README.en-US.md).

## Current Boundaries

- Platform pools remain suitable for CPU-intensive work or workloads requiring explicitly bounded worker resources.
- New blocking-I/O code should prefer `peach-virtual-thread` group concurrency, pending and backpressure semantics.
- Thread-pool context propagation must not be interpreted as Spring transaction propagation across threads.
- Queueing, rejection, timeout and cancellation semantics follow the current implementation, not copied historical examples.
