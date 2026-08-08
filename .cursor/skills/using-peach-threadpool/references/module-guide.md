# peach-threadpool 模块参考

本文记录当前线程池 starter 的入口和已知语义。注解行为、默认值和上下文传递必须以当前源码与测试为准。

## 模块导航

```text
peach-component/peach-threadpool/
├── pom.xml                                # 聚合模块
├── README.md
├── peach-threadpool-autoconfigure/
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/peach/threadpool/
│       │   ├── ThreadPoolAutoConfigure.java
│       │   ├── annoation/AsyncExecuted.java
│       │   ├── config/                    # ThreadPoolProperties 等配置模型
│       │   ├── core/                      # PoolType/TaskWrapper/Aspect
│       │   └── manager/ThreadPoolManager.java
│       └── resources/META-INF/
└── peach-threadpool-starter/
    └── pom.xml                            # 业务接入依赖
```

当前模块没有 example 子模块；不要在文档中编造示例路径。导航时忽略 `target/` 和 `.flattened-pom.xml`。

## 可验证入口

- `ThreadPoolProperties`：`peach.threadpool` 配置绑定，包含 global 与 pools。
- `ThreadPoolManager`：线程池查找、提交与执行入口。
- `PoolType`：CPU、IO、CACHED、SCHEDULED 等池类型。
- `TaskWrapper`：MDC/SecurityContext 等任务上下文包装。
- `@AsyncExecuted`：当前包名为 `annoation` 的方法级 AOP 注解。
- `ThreadPoolAspect`：注解真实执行、等待和超时语义来源。

## REQUIRED

- 业务异步使用 `ThreadPoolManager` 或 `@AsyncExecuted`，禁止 `new Thread` 和未托管 `Executors`。
- 按任务类型选择 PoolType；线程名前缀必须能定位业务域。
- 队列容量、最大线程、拒绝策略和超时必须有可解释的容量依据。
- 检查 AOP 自调用、异常传播、Future/CompletableFuture 链路和关闭生命周期。
- 关闭 MDC/SecurityContext 传递前评估日志追踪与权限上下文风险。

## PREFERRED

- 关键任务使用可观测的背压或快速失败策略，不静默丢弃。
- IO 与 CPU 任务隔离；定时任务使用 SCHEDULED 池。
- 真正异步返回优先显式 `CompletableFuture` 契约，并验证目标线程池和异常处理。
- 对队列长度、活跃线程、拒绝和执行耗时建立指标。

## LEGACY_COMPATIBLE

- `annoation` 拼写、现有配置字段和池类型属于 API 兼容约束，新代码引用时保持正确包名但不扩散到新命名。
- 普通返回值路径可能提交后立即 `Future.get()`，这只是当前实现语义，不应描述为 fire-and-forget。
- `timeoutMs` 限制等待时间，不保证底层任务可靠取消。

## FORBIDDEN

- 无界队列或超大队列掩盖过载。
- 关键任务使用 DISCARD/DISCARD_OLDEST 却不记录和补偿。
- 吞掉异步异常、忽略 Future 或假设类级注解自动切全部方法。
- 把 ThreadLocal 请求对象无边界传入长期任务。

## 配置示意

```yaml
peach:
  threadpool:
    global:
      enable-mdc: true
      enable-security-context: true
    pools:
      - type: IO
        core-size: 16
        max-size: 64
        queue-capacity: 1000
        thread-name-prefix: storage-io-
        rejected-policy: CALLER_RUNS
```

## 验证

```bash
mvn -f "peach-component/peach-threadpool/pom.xml" test
mvn -f "peach-component/peach-threadpool/pom.xml" -DskipTests package
node scripts/check-utf8.mjs
```
