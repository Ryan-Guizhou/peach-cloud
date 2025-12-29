# Repeat Execute Limit Starter 使用说明

## 一、背景与设计目标

在分布式系统中，以下问题极为常见：

- 用户重复提交表单
- 消息重复消费
- 定时任务并发执行
- 接口在短时间内被多次调用

**Repeat Execute Limit Starter** 的设计目标是：

> 在保证性能的前提下，防止同一业务在指定时间窗口内被重复执行。

该 Starter 基于 **AOP + 本地锁 + Redisson 分布式锁 + Redis 幂等标记** 实现，具备生产级稳定性。

---

## 二、核心设计思想

采用 **三层防护模型**：

```
Redis 成功标记（逻辑幂等）
        ↑
分布式锁（跨 JVM / 跨节点）
        ↑
本地 JVM 锁（高并发快速失败）
```

### 设计原则

- 宁可拒绝请求，也不排队等待
- 成功后才写入幂等标记
- 本地锁优先，降低 Redis 压力
- 锁与业务 Key 强绑定

---

## 三、Starter 核心组件说明

### 1. RepeatExecuteLimitAspect

切面入口，拦截所有被 `@RepeatLimit` 注解的方法。

核心职责：

- 生成业务唯一锁 Key
- 校验 Redis 幂等标记
- 协调本地锁与分布式锁
- 控制业务执行与结果标记

---

### 2. @RepeatLimit 注解

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RepeatLimit {

    String name();

    String[] keys();

    long durationTime() default 0;

    String message() default "重复请求，请稍后再试";
}
```

#### 参数说明

| 参数 | 说明 |
|----|----|
| name | 业务名称 |
| keys | 构建唯一 Key 的参数表达式 |
| durationTime | 成功后的幂等有效期（秒） |
| message | 重复执行提示信息 |

---

### 3. LockInfoHandle & Factory

用于统一管理锁 Key 的生成规则，确保：

- 不同业务 Key 不冲突
- 参数变更即生成不同锁

---

### 4. LocalCacheLock

基于 `ReentrantLock` 的本地缓存锁。

作用：

- 防止同 JVM 内高并发请求同时抢分布式锁
- 降低 Redis 网络与锁竞争压力

---

### 5. DistributedLocker（Redisson）

默认使用 **公平锁（Fair Lock）**。

特性：

- 防止请求饥饿
- 保证获取锁的顺序性
- `tryLock(0)` 不等待，立即失败

---

### 6. RedissionDataHandle

对 Redis 操作的统一抽象，主要用于：

- 幂等标记读取
- 成功标记写入（带 TTL）

---

## 四、完整执行流程

```
请求进入
  ↓
Redis 是否存在 SUCCESS_FLAG
  ↓ 否
获取本地 ReentrantLock
  ↓ 成功
尝试获取 Redisson 分布式锁
  ↓ 成功
再次校验 Redis SUCCESS_FLAG
  ↓
执行业务方法
  ↓
写入 SUCCESS_FLAG（带 TTL）
  ↓
释放分布式锁
  ↓
释放本地锁
```

---

## 五、使用示例

### 1. 引入 Starter

```xml
<dependency>
    <groupId>com.peach</groupId>
    <artifactId>repeat-execute-limit-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

### 2. 方法防重复执行

```java
@RepeatLimit(
    name = "order_submit",
    keys = {"#orderId"},
    durationTime = 60,
    message = "订单正在处理中，请勿重复提交"
)
public void submitOrder(Long orderId) {
    // 核心业务逻辑
}
```

效果：

- 同一个 `orderId`，60 秒内只能成功执行一次
- 并发请求立即失败，不阻塞

---

## 六、典型使用场景

| 场景 | 说明 |
|----|----|
| 防重复提交 | 表单、按钮 |
| 支付回调 | 防止重复通知 |
| 消息消费 | 防止重复消费 |
| 定时任务 | 防止并发执行 |
| 状态流转 | 防止状态错乱 |

---

## 七、异常与边界说明

- 业务执行异常：不会写入 SUCCESS_FLAG
- Redis 写入失败：仅记录日志，不影响主流程
- 锁释放：全部在 finally 中完成，避免死锁

---

## 八、设计优势总结

- 高性能：本地锁 + tryLock
- 高可用：Redis 故障不影响 JVM 内互斥
- 高扩展：支持公平锁 / 非公平锁切换
- 高一致性：双重 Redis 校验

---

## 九、最佳实践建议

- `keys` 必须能唯一标识业务
- 幂等时间窗口不宜过长
- 不适合长事务或强一致场景
- 推荐与业务异常体系结合使用

---

## 十、结语

Repeat Execute Limit Starter 是一套 **工程级防重复执行解决方案**，
适合在微服务、高并发、分布式场景中作为基础能力长期复用。
