# peach-redission

[English](README.en-US.md) | 中文

最后更新时间：2026-07-15
artifactId：`peach-redission`
类型：Redisson 中间件聚合模块

## 模块定位

`peach-redission` 基于 `RedissonClient` 提供分布式锁、延迟队列、可分段扩容布隆过滤器和防重复执行。业务模块应按能力依赖 starter，不直接依赖 autoconfigure。

仓库当前 artifactId、包名及部分公共类型沿用 `redission`、`distrbuted` 等历史拼写。使用时必须按源码拼写引用；新增 API 不继续复制这些拼写。

## 模块导航

```text
peach-middleware/peach-redission
├── peach-redission-common
│   ├── RedissionCommonAutoconfigure
│   ├── RedissionDataHandle
│   ├── LocalCacheLock
│   └── LockInfoHandleFactory
├── peach-redission-distributedlock-autoconfigure
│   ├── DistrbutedLock                 # 注解
│   ├── DistrbutedLockAspect
│   ├── DistrbutedLockerManager
│   ├── DistrbutedLockerFactory
│   └── DistributedLocker / LockType
├── peach-redission-distributedlock-starter
├── peach-redission-delayqueue-autoconfigure
│   ├── DelayQueueContext              # 生产入口
│   ├── ConsumerTask                   # 消费契约
│   ├── DelayQueueProperties
│   ├── ReliableDelayConsumerQueue
│   └── DeadLetterQueueManager
├── peach-redission-delayqueue-starter
├── peach-redission-bloomfilter-autoconfigure
│   ├── BloomFilterService
│   ├── SegmentedBloomFilterService
│   ├── BloomFilterProperties
│   └── spi                            # 命名、编解码、扩容策略
├── peach-redission-bloomfilter-starter
├── peach-redission-repeat-autoconfigure
│   ├── RepeatLimit                    # 注解
│   └── RepeatExecuteLimitAspect
└── peach-redission-repeat-starter
```

## 前置配置

所有能力依赖 `RedissonClient`。默认客户端由 `peach-redis-common` 的 `RedisConfig` 创建，因此应先配置 `peach.redis`：

```yaml
peach:
  redis:
    mode: standalone
    host: ${PEACH_REDIS_NODES:127.0.0.1:6379}
    password: ${PEACH_REDIS_PASSWORD:}
    database: 0
    redisson:
      enabled: true
```

如果业务自行提供 `RedissonClient`，应确保容器中只有一个可明确注入的实例。各 autoconfigure POM 中部分运行时依赖当前标记为 optional；独立接入时必须通过依赖树确认 `peach-redis-common`、AOP 和 Redisson 依赖均已进入应用。

## 分布式锁

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-distributedlock-starter</artifactId>
</dependency>
```

```java
@DistrbutedLock(
        name = "order:pay",
        keys = {"#orderId"},
        lockType = LockType.REENTRANT,
        waitTime = 5,
        timeUnit = TimeUnit.SECONDS
)
public void pay(Long orderId) {
    // 业务处理
}
```

| 注解属性 | 默认值 | 说明 |
| --- | --- | --- |
| `keys` | 必填 | 参与锁名计算的 SpEL 表达式 |
| `name` | 空 | 业务前缀 |
| `lockType` | `REENTRANT` | 锁类型 |
| `waitTime` | `10` | 获取锁最大等待时长 |
| `timeUnit` | `SECONDS` | 时间单位 |
| `lockTimeoutStrategy` | `FAIL` | 获取锁超时处理策略 |
| `customLockTimeoutStrategy` | 空 | 自定义策略 Bean 名称 |

注解通过 Spring AOP 生效，类内自调用不会经过切面。锁只约束并发执行，不替代数据库唯一约束、状态机校验或业务幂等。

虽然注解声明允许标在类型上，当前切面只匹配方法注解；类级 `@DistrbutedLock` 不会生效。`RedissionWriteLocker` 的 `getLock()` 行为与其他 locker 也不一致，使用写锁前必须补充专项测试。

## 延迟队列

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-delayqueue-starter</artifactId>
</dependency>
```

生产消息：

```java
delayQueueContext.sendMessage(
        "order-timeout",
        payload,
        30,
        TimeUnit.MINUTES
);
```

注册消费者：

```java
@Component
public class OrderTimeoutTask implements ConsumerTask {
    @Override
    public void execute(String content) {
        orderService.closeExpiredOrder(content);
    }

    @Override
    public String topic() {
        return "order-timeout";
    }
}
```

配置前缀为 `peach.delay.queue`：

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `core-pool-size` / `maximum-pool-size` | `4` / `4` | 消费线程数 |
| `work-queue-size` | `256` | 消费线程池队列容量 |
| `isolation-region-count` | `5` | topic 分区数，生产者与消费者必须一致 |
| `max-retry-attempts` | `3` | 消费失败最大重试次数 |
| `retry-interval-millis` | `5000` | 重试间隔 |
| `use-reliable-queue` | `true` | 使用可靠消费队列 |
| `max-dead-letter-queue-size` | `10000` | 死信上限 |
| `dead-letter-message-retention-hours` | `168` | 死信保留时间 |

延迟到期不等于精确执行时刻；线程池排队、Redis 延迟和业务耗时都会产生偏差。消费者必须幂等，并对重试、死信、积压和任务耗时建立监控。

`use-reliable-queue=true` 是当前实现路径选择，不构成“不丢不重”的承诺：取消息与写 processing 状态并非原子操作，多实例恢复可能重复，线程池拒绝和进程退出也需要专项验证。死信上限计数保存在 JVM 内存，多实例和重启后不等于 Redis 中的全局准确数量。

## 布隆过滤器

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-bloomfilter-starter</artifactId>
</dependency>
```

```yaml
peach:
  redis:
    bloom:
      enabled: true
      key-prefix: bloom
      initial-capacity: 1000000
      false-positive-probability: 0.001
      load-factor: 0.9
      scale-factor: 2.0
      max-segments: 32
      default-namespace: default
      enable-local-cache: true
```

```java
bloomFilterService.add("blocked-user", userId);
boolean mightExist = bloomFilterService.mightContain("blocked-user", userId);
```

`BloomFilterService` 支持命名空间初始化、单条/批量写入、跨段查询、状态查询与清理。可通过 JDK SPI 覆盖 `KeyNamingStrategy`、`CodecProvider`、`BloomScalePolicy`；自动配置选择发现到的第一个实现。

布隆过滤器只能回答“可能存在”或“一定不存在”，不能用于授权、余额、唯一性等要求绝对准确的最终判断。`clear(namespace)` 是破坏性操作，应限制调用权限。

当前默认开启本地段列表缓存，但节点间没有扩容同步，其他节点可能因缓存旧段列表产生假阴性；分段计数 key 也未按段隔离，状态和扩容统计可能失真。修复并完成多节点测试前，不得将该实现用于要求“绝无漏判”的前置拦截。

## 防重复执行

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>peach-redission-repeat-starter</artifactId>
</dependency>
```

```java
@RepeatLimit(
        name = "order:submit",
        keys = {"#request.requestId"},
        durationTime = 30,
        message = "请勿重复提交"
)
public Long submit(OrderRequest request) {
    return orderService.create(request);
}
```

`durationTime` 单位为秒。值大于 0 时，方法成功后写入防重标记；值为 0 时仅限制并发执行，不保留成功标记。该能力依赖 `LocalCacheLock`、`RedissionDataHandle` 和分布式锁相关 Bean。

防重复 key 必须包含稳定业务标识和必要的租户/用户隔离维度，不能使用随机值，也不能把敏感参数原文拼入 Redis key。

当前切面会吞掉防重成功标记写入异常，业务可能已返回成功但后续防重失效；与锁注解相同，类级 `@RepeatLimit` 当前不会被切面匹配。

## 扩展方式

- 自定义 `RedissonClient`：统一连接、codec、TLS 与监控配置。
- 自定义 `LockInfoHandle`：扩展锁名解析，但需注册到 `LockInfoHandleFactory`。
- 自定义 `BloomFilterService` Bean：覆盖默认分段实现。
- 布隆 SPI：在 `META-INF/services/<SPI 全限定名>` 注册实现。
- 延迟队列：实现多个 `ConsumerTask` Bean，以唯一 topic 区分任务。

布隆三个 SPI 不是 Spring Bean 覆盖；存在多个 `ServiceLoader` 实现时当前直接选择第一个，选择顺序不稳定，部署包中应保证每类 SPI 只有一个目标实现。

## 运行机制

```text
RedisConfig ──> RedissonClient
                   │
                   ├── @DistrbutedLock ──> AOP ──> DistributedLocker
                   ├── DelayQueueContext ──> 分区延迟队列 ──> ConsumerTask
                   ├── BloomFilterService ──> 分段 RBloomFilter
                   └── @RepeatLimit ──> 本地锁 + 分布式锁 + 成功标记
```

## 生产边界

`REQUIRED`：

- 锁、防重、队列和布隆 key 必须带业务命名空间；多租户场景必须隔离租户。
- 锁内业务必须控制耗时，并保证异常时释放锁。
- 延迟任务消费者必须幂等，死信必须有告警和人工/自动补偿路径。
- Redis 节点、密码、payload 和完整业务 key 不得输出到日志。
- 将 `peach.localLock.durationTime` 当作兼容风险处理：metadata 标为秒，但当前源码按小时使用；修复前不要依赖该配置做精确回收。

`PREFERRED`：

- 锁等待时间小于上游请求超时，并对获取失败做明确业务处理。
- 延迟消息只保存完成任务所需的最小数据，避免存储敏感快照和大对象。
- 布隆参数根据预估容量、可接受 FPP 和内存预算计算后压测。

`LEGACY_COMPATIBLE`：

- `redission`、`DistrbutedLock`、`distrbutedlock`、`annoation` 等拼写按现有二进制兼容，不作为新命名范式。
- “可靠队列”、布隆本地缓存和 DLQ 上限均只描述当前实现意图，不代表分布式强保证。

`FORBIDDEN`：

- 用分布式锁替代数据库约束或事务。
- 用布隆过滤器的肯定结果直接做权限、安全或资金决策。
- 假设延迟消息绝不重复、绝不丢失或准时到毫秒。
- 用可变字段、随机值或未隔离租户的字段生成锁和防重 key。

## 构建与验证

```bash
mvn -f "peach-middleware/peach-redission/pom.xml" clean package -DskipTests -Pdevelopment
node scripts/check-utf8.mjs
git diff --check
```

当前聚合模块未发现测试源码；构建通过不能证明锁、队列、布隆过滤器或防重复的分布式语义正确。

## 排障指南

| 现象 | 检查点 | 处理方式 |
| --- | --- | --- |
| `RedissonClient` 未注入 | `peach.redis` 配置、enabled、依赖树 | 启用默认客户端或提供自定义 Bean |
| 锁注解不生效 | 是否经过 Spring AOP；是否类内自调用 | 从代理 Bean 外部调用并检查切面 Bean |
| 锁持续等待或频繁失败 | key 粒度、waitTime、业务耗时 | 缩小临界区并对超时显式降级 |
| 自定义超时策略不生效 | 策略名和 Bean 是否匹配 | 核对 `customLockTimeoutStrategy` |
| 延迟队列不消费 | `ConsumerTask`、topic、分区数、Redisson 连接 | 检查 Bean 注册和启动日志 |
| 延迟任务反复失败 | payload、重试次数、死信状态 | 修复消费逻辑并安全补偿死信 |
| 布隆误判升高 | 容量、FPP、段数和负载 | 查看 `status`，调整参数或重建命名空间 |
| 防重复误拦截 | SpEL、业务 key、durationTime | 修正隔离维度并清理错误标记 |


## 项目约定

- 后端文档统一遵循当前 peach-cloud 基线：Java 21、Spring Boot 3.5.4、Spring Cloud 2025.0.0、Spring Cloud Alibaba 2025.0.0.0。
- 前端文档仅适用于 peach-cloud-front，该目录是独立的 Vue 3 + Vite + TypeScript 工程，不属于 Maven reactor。
- 源码、脚本、SQL 和 Markdown 均保持 UTF-8 无 BOM；不要把 	arget/、.flattened-pom.xml、依赖缓存或 IDE 文件写入源码结构。
- README 中的命令、类名、配置项和示例必须能从当前仓库验证；不得写入真实密钥、token、私钥、生产密码、签名 URL 或完整敏感报文。
