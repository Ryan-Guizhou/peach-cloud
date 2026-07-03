# peach-threadpool 模块参考

## 模块路径

- 聚合模块：`peach-component/peach-threadpool`
- 自动配置：`peach-component/peach-threadpool/peach-threadpool-autoconfigure`
- 对外 starter：`peach-component/peach-threadpool/peach-threadpool-starter`

## 核心对象

- `ThreadPoolProperties`：绑定 `peach.threadpool`，包含 `global` 和 `pools`。
- `GlobalProperties`：`enableMdc`、`enableSecurityContext`，默认均为 `true`。
- `PoolProperties`：线程池参数模型。
- `ThreadPoolManager`：持有并路由 `ExecutorService`，提供 `get`、`submit`、`execute`。
- `TaskWrapper`：包装任务上下文。
- `NamedThreadFactory`：生成带业务前缀的线程名。
- `@AsyncExecuted`：方法级 AOP 注解。

## 配置示例

```yaml
peach:
  threadpool:
    global:
      enable-mdc: true
      enable-security-context: true
    pools:
      - type: CPU
        core-size: 4
        max-size: 8
        queue-capacity: 200
        keep-alive-seconds: 60
        thread-name-prefix: cpu-task-
        rejected-policy: CALLER_RUNS
      - type: IO
        core-size: 16
        max-size: 64
        queue-capacity: 1000
        keep-alive-seconds: 60
        thread-name-prefix: io-task-
        rejected-policy: CALLER_RUNS
```

## 参数规则

- `coreSize`：核心线程数，CPU 密集通常接近 CPU 核数，IO 密集可更高。
- `maxSize`：最大线程数，必须大于等于核心线程数。
- `queueCapacity`：大于 0 使用有界 `LinkedBlockingQueue`，小于等于 0 使用 `SynchronousQueue`。
- `keepAliveSeconds`：非核心线程空闲存活时间。
- `allowCoreThreadTimeOut`：是否允许核心线程回收。
- `prestartCoreThreads`：是否启动时预热核心线程。
- `threadNamePrefix`：线程名前缀，默认 `peach-pool-`。
- `rejectedPolicy`：`ABORT`、`CALLER_RUNS`、`DISCARD`、`DISCARD_OLDEST`。

## 构建验证

```bash
mvn -f "peach-component/peach-threadpool/pom.xml" test
mvn -f "peach-component/peach-threadpool/pom.xml" -DskipTests package
```
