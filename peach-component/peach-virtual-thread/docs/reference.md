# peach-virtual-thread Reference

本文提供当前实现的配置与公共入口速查。设计原理见 [design.md](./design.md)，最小接入见 [README](../README.md)。

## 1. 配置前缀

```text
peach.virtual-thread
```

全局配置：

| 配置项 | 类型 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- |
| `enabled` | `boolean` | `true` | - | 是否启用 Starter 自动装配 |
| `thread-name-prefix` | `String` | `peach-vt-` | 非空 | 虚拟线程统一名称前缀 |
| `shutdown-await` | `Duration` | `30s` | 不得为负；必须可安全转换为纳秒 | Spring 关闭阶段全部业务组共享的等待上限 |

业务组配置：

| 配置项 | 类型 | 默认值 | 约束 | 含义 |
| --- | --- | --- | --- | --- |
| `groups.<name>.max-concurrency` | `int` | `256` | `> 0` | 最大真实执行任务数 |
| `groups.<name>.max-pending` | `int` | `512` | `>= 0` | 最大 Pending 任务数 |
| `groups.<name>.backpressure` | `BackpressurePolicy` | `REJECT` | `REJECT` / `BLOCK` | Admission 容量耗尽后的策略 |
| `groups.<name>.acquire-timeout` | `Duration` | `50ms` | `BLOCK` 时必须为正且可转换为纳秒 | 提交线程等待 Admission Permit 的最长时间 |

此外，`max-concurrency + max-pending` 不能超过 `Integer.MAX_VALUE`。业务组名称不能为空。

## 2. 推荐最小配置

```yaml
peach:
  virtual-thread:
    enabled: true
    thread-name-prefix: peach-vt-
    shutdown-await: 30s
    groups:
      database:
        max-concurrency: 50
        max-pending: 100
        backpressure: BLOCK
        acquire-timeout: 50ms
```

仓库 quickstart 还给出了 `storage` 与 `remote` 分组示例。生产配置应依据数据库连接池、HTTP 客户端并发限制、对象存储吞吐等真实资源边界调整。

## 3. VirtualExecutorService

`VirtualExecutorService` 继承 `java.util.concurrent.ExecutorService`，因此可直接使用标准 `execute`、`submit`、`invokeAll`、`invokeAny`、关闭与终止查询 API。

额外公共入口：

| API | 返回值 | 用途 |
| --- | --- | --- |
| `groupName()` | `String` | 当前业务组名称 |
| `submit(Callable<T>)` | `Future<T>` | 标准 Future 提交；受业务组容量和生命周期管理 |
| `submit(Runnable)` | `Future<?>` | 无返回值任务提交 |
| `submit(Runnable, T)` | `Future<T>` | 指定成功结果的任务提交 |
| `supplyAsync(Supplier<T>)` | `CompletableFuture<T>` | 组件受管 CompletableFuture 提交 |
| `runAsync(Runnable)` | `CompletableFuture<Void>` | 组件受管无返回值 CompletableFuture 提交 |
| `snapshot()` | `VirtualGroupSnapshot` | 获取当前业务组只读运行快照 |
| `activeTasks()` | `List<VirtualTaskSnapshot>` | 获取当前活动任务安全快照 |

容量耗尽、等待超时、等待被中断或执行器已关闭时，提交可能以 `RejectedExecutionException` 体系异常失败；业务调用方不应把拒绝当成成功。

## 4. @VirtualGroup

按业务组注入执行器时，使用 `@VirtualGroup("groupName")` 对目标 `VirtualExecutorService` 做显式限定。例如：

```java
@Service
public class ReportService {

    private final VirtualExecutorService databaseExecutor;

    public ReportService(
            @VirtualGroup("database") VirtualExecutorService databaseExecutor) {
        this.databaseExecutor = databaseExecutor;
    }

    public Future<String> load() {
        return databaseExecutor.submit(() -> "result");
    }
}
```

仓库 quickstart 使用同样的构造器注入方式。

## 5. VirtualExecutorRegistry

`VirtualExecutorRegistry` 保存启动期已创建的业务组执行器，并提供按组访问入口：

| API | 说明 |
| --- | --- |
| `get(String group)` | 获取指定组执行器；未知或空组名抛出 `IllegalArgumentException` |
| `submit(String, Callable<T>)` | 按组提交 Callable |
| `submit(String, Runnable)` | 按组提交 Runnable |
| `submit(String, Runnable, T)` | 按组提交 Runnable 并指定成功结果 |
| `execute(String, Runnable)` | 按组直接执行任务 |
| `groupNames()` | 返回不可变业务组名称集合 |
| `all()` | 返回全部受管执行器的不可变列表 |

Registry 不绕过业务组并发、Pending、背压和 Permit 管理，也不在运行期动态创建或修改业务组。

## 6. 背压策略

### REJECT

Admission 已满时立即拒绝。调用方应结合接口语义决定返回错误、降级或有限重试，禁止静默吞掉任务。

### BLOCK

在提交线程上最多等待 `acquire-timeout`。等待可中断；中断时会恢复 interrupt flag 并以拒绝异常向上传播。

`BLOCK` 只限制“等待准入”的时间。任务一旦准入，Pending 阶段等待 Execution Permit 的时长由真实执行任务释放容量的速度决定。

## 7. Future 选择

| 场景 | 推荐入口 |
| --- | --- |
| 迁移传统 `ExecutorService` 调用 | `submit(...)` |
| 需要 CompletableFuture 编排与组件受管取消 | `supplyAsync(...)` / `runAsync(...)` |
| 需要按字符串业务组路由 | `VirtualExecutorRegistry` |
| Fire-and-forget | `execute(...)`，并明确处理未承接异常 |

当组件受管取消语义很重要时，不要用 `CompletableFuture.supplyAsync(fn, executor)` 替代组件自己的 `supplyAsync`。

## 8. 生命周期边界

- 业务组在应用启动期固定。
- Registry 在 Spring 容器销毁时协调全部组优雅关闭。
- `shutdown-await` 是所有组共享的总等待预算，不是每组单独获得一份完整预算。
- grace period 耗尽后，未终止组会执行 `shutdownNow()`。
- 底层阻塞调用若不响应中断，框架无法承诺业务逻辑立即停止。

## 9. 相关源码

维护实现时优先核对：

- `api/VirtualExecutorService.java`
- `config/VirtualThreadProperties.java`
- `concurrency/GroupConcurrencyController.java`
- `concurrency/PermitLedger.java`
- `executor/DefaultVirtualExecutorService.java`
- `executor/ManagedFutureTask.java`
- `executor/ManagedCompletableTask.java`
- `registry/VirtualExecutorRegistry.java`
- `peach-virtual-thread-quickstart/src/main/java/.../VirtualThreadScenarioService.java`
- `peach-virtual-thread-quickstart/src/main/resources/application.yml`
