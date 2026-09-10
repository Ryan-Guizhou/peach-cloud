# peach-threadpool

[English](README.en-US.md) | 中文

最后更新时间：2026-09-10
artifactId：`peach-threadpool`
类型：存量线程池兼容模块

## 模块定位

`peach-threadpool` 是存量线程池兼容模块，当前仅为已有代码保留 `ThreadPoolManager`、`@AsyncExecuted` 和 `peach.threadpool` 配置契约。新建或实质修改的阻塞 IO 异步能力应迁移到 `peach-virtual-thread`，通过 `VirtualExecutorService`、`VirtualExecutorRegistry` 或 `@VirtualGroup` 按资源组执行。

本模块不再作为新业务异步能力推荐入口，也不承载 Java 21 虚拟线程的容量隔离、背压、取消和优雅关闭治理。

## 子模块

| 子模块 | 职责 |
| --- | --- |
| `peach-threadpool-autoconfigure` | 自动配置、配置属性、线程池管理器、注解切面 |
| `peach-threadpool-starter` | 对业务模块暴露的 starter |

## 核心对象

| 对象 | 说明 |
| --- | --- |
| `ThreadPoolProperties` | 绑定 `peach.threadpool` 配置 |
| `GlobalProperties` | MDC/Micrometer 与 SecurityContext 传递开关，默认值均为 `true` |
| `PoolProperties` | 单个线程池参数 |
| `ThreadPoolManager` | 获取、提交和执行任务 |
| `TaskWrapper` | 任务上下文包装 |
| `NamedThreadFactory` | 线程名前缀控制 |
| `@AsyncExecuted` | 方法级 AOP 注解，包名当前为 `com.peach.threadpool.annoation.AsyncExecuted` |

## 存量接入方式

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-threadpool-starter</artifactId>
</dependency>
```

配置示例：

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
        keep-alive-seconds: 60
        thread-name-prefix: io-task-
        rejected-policy: CALLER_RUNS
```

## 使用示例

以下示例仅用于维护旧调用点；新代码不要复制。

```java
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final ThreadPoolManager threadPoolManager;

    public void submitTask(Runnable task) {
        threadPoolManager.execute(PoolType.IO, task);
    }
}
```

注解方式：

```java
@AsyncExecuted(type = PoolType.IO, timeoutMs = 3000)
public CompletableFuture<String> loadRemote() {
    return CompletableFuture.completedFuture("ok");
}
```

## 当前语义

- `@AsyncExecuted` 当前只切方法注解，不切类注解。
- 普通返回值路径会提交到线程池后等待 `Future.get()`，不是 fire-and-forget。
- 需要真正异步返回时优先使用 `CompletableFuture`，并检查调用链是否符合预期。
- `timeoutMs > 0` 限制等待结果的时间，不等于可靠取消底层任务。
- 拒绝策略支持 `ABORT`、`CALLER_RUNS`、`DISCARD`、`DISCARD_OLDEST`。
- `enable-mdc=true` 时会传播 Micrometer ThreadLocal 与完整 MDC，包括 requestId、traceId 和 spanId；任务完成后恢复工作线程原上下文。
- `PoolType.VIRTUAL` 仅是旧模块中的存量枚举能力，不具备 `peach-virtual-thread` 的分组并发、Pending 背压和受管取消语义。

## 边界与限制

- IO 任务不要误用 CPU 池，CPU 密集任务不要无限扩大线程数。
- 队列容量过大可能掩盖过载并导致延迟堆积。
- 关闭 MDC/Micrometer 传播前，需要确认异步 Trace 和日志关联不受影响。
- 关闭 SecurityContext 传递前，需要确认权限上下文不受影响。
- 自调用不会经过 Spring AOP，注解可能不生效。
- 新业务阻塞 IO 异步不要继续新增 `ThreadPoolManager` 或 `@AsyncExecuted`。

## 构建与验证

```bash
mvn -f "peach-component/peach-threadpool/pom.xml" test
mvn -f "peach-component/peach-threadpool/pom.xml" clean package -DskipTests -Pdevelopment
mvn -pl peach-component/peach-threadpool -am clean package -DskipTests -Pdevelopment
```

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| 注解不生效 | 是否标在方法上；调用是否经过 Spring 代理 | 避免自调用，确认 Bean 被 Spring 管理 |
| 调用线程仍在等待 | 方法是否普通返回值 | 使用 `CompletableFuture` 或直接提交任务 |
| 队列堆积 | `queueCapacity`、线程数、任务耗时 | 调整池参数，增加监控和拒绝策略 |
| 异步日志缺少 requestId/traceId | `enable-mdc` 是否开启；任务是否通过 `ThreadPoolManager` 提交 | 开启上下文传播，不要绕过受管线程池 |
| 权限上下文丢失 | SecurityContext 传递是否开启 | 检查 `enable-security-context` |
| 新功能需要阻塞 IO 异步 | 是否仍在新增 `ThreadPoolManager` | 改用 `peach-virtual-thread-starter` 并配置业务 group |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
