
# Redisson 分布式锁详解与使用场景（含案例）

> 本文面向 **Java / Spring / 分布式系统工程实践**，系统性介绍 Redisson 中最常用的几类锁：  
> **可重入锁、读写锁、公平锁、非公平锁**，并给出**典型业务使用场景与示例代码**。

---

## 一、Redisson 锁体系概览

Redisson 是基于 Redis 的分布式工具库，其锁具有以下共性特征：

- 基于 **Redis + Lua**，保证原子性
- 支持 **可重入**
- 支持 **自动续期（WatchDog）**
- 支持 **公平 / 非公平 / 读写 / 联锁 / 信号量**
- 无侵入，适合 Spring / 微服务架构

常用锁类型一览：

| 锁类型 | 接口 |
|------|------|
| 可重入锁 | `RLock` |
| 读写锁 | `RReadWriteLock` |
| 公平锁 | `getFairLock()` |
| 非公平锁 | `getLock()`（默认） |

---

## 二、可重入锁（Reentrant Lock）

### 1. 概念

**可重入锁**指同一线程在持有锁的情况下，可以多次获取同一把锁而不会死锁。

Redisson 的所有锁 **默认都是可重入的**。

---

### 2. 使用方式

```java
RLock lock = redissonClient.getLock("order-lock");
lock.lock();
try {
    // 业务逻辑
    methodA();
} finally {
    lock.unlock();
}
```

```java
public void methodA() {
    RLock lock = redissonClient.getLock("order-lock");
    lock.lock(); // 可重入
    try {
        // 子方法逻辑
    } finally {
        lock.unlock();
    }
}
```

---

### 3. 底层原理（简述）

- Redis Hash 结构：`key -> threadId : count`
- 同一线程重复加锁：`count +1`
- 解锁时递减，直到为 0 才真正释放

---

### 4. 使用场景

✅ **推荐场景**

- AOP 分布式锁
- 方法嵌套调用
- 事务方法内再次加锁
- 微服务幂等控制

❌ **不适合场景**

- 需要严格顺序（应使用公平锁）

---

## 三、非公平锁（默认锁）

### 1. 概念

**非公平锁**：谁先抢到锁谁执行，不保证请求顺序。

```java
RLock lock = redissonClient.getLock("lock");
```

---

### 2. 特点

| 特性 | 说明 |
|----|----|
| 是否排队 | ❌ |
| 吞吐量 | ⭐⭐⭐⭐⭐ |
| 延迟 | 低 |
| 饥饿风险 | 有 |
| 默认推荐 | ✔ |

---

### 3. 示例场景

#### 示例：接口防并发提交

```java
RLock lock = redissonClient.getLock("submit:" + userId);
if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
    try {
        // 提交逻辑
    } finally {
        lock.unlock();
    }
}
```

---

### 4. 适用场景

- 高并发接口
- 性能敏感业务
- 允许“先抢先执行”
- 绝大多数业务场景（**首选**）

---

## 四、公平锁（Fair Lock）

### 1. 概念

**公平锁**保证获取锁的顺序与请求顺序一致（FIFO）。

```java
RLock fairLock = redissonClient.getFairLock("fair-lock");
```

---

### 2. 底层实现

- Redis List / ZSet 维护等待队列
- 每次加锁：入队 → 判断是否轮到自己 → 出队
- Redis 操作明显增加

---

### 3. 特点对比

| 维度 | 公平锁 |
|----|----|
| 顺序保证 | ✔ |
| 吞吐量 | ⭐⭐ |
| Redis 压力 | 高 |
| 延迟 | 高 |
| 饥饿问题 | 无 |

---

### 4. 示例场景

#### 示例：金融清算 / 账务顺序处理

```java
RLock fairLock = redissonClient.getFairLock("account-lock");
fairLock.lock();
try {
    // 顺序敏感的资金处理
} finally {
    fairLock.unlock();
}
```

---

### 5. 使用建议

⚠️ **99% 的业务不需要公平锁**  
公平锁 = 顺序保证 + 性能代价

---

## 五、读写锁（RReadWriteLock）

### 1. 概念

读写锁适用于 **读多写少** 的场景：

- 读锁：共享
- 写锁：互斥

```java
RReadWriteLock rwLock = redissonClient.getReadWriteLock("rw-lock");
```

---

### 2. 行为规则

| 当前锁 | 允许 | 阻塞 |
|----|----|----|
| 读锁 | 多读 | 写 |
| 写锁 | 单写 | 读 + 写 |

---

### 3. 示例代码

#### 读操作

```java
RLock readLock = rwLock.readLock();
readLock.lock();
try {
    // 查询缓存 / 配置
} finally {
    readLock.unlock();
}
```

#### 写操作

```java
RLock writeLock = rwLock.writeLock();
writeLock.lock();
try {
    // 更新配置 / 刷新缓存
} finally {
    writeLock.unlock();
}
```

---

### 4. 底层结构（简化）

```text
rw-lock
 ├── mode = read | write
 ├── readers = { threadId : count }
 ├── writer = threadId
```

---

### 5. 典型使用场景

✅ **强烈推荐**

| 场景 | 原因 |
|----|----|
| 配置中心 | 读多写少 |
| 本地缓存 | 高并发读 |
| 字典表 | 写少 |
| 规则热更新 | 写操作互斥 |

❌ **不适合**

- 写操作频繁
- 长时间持锁

---

### 6. 重要注意点

⚠️ Redisson 的读写锁 **不是公平的**  
可能出现 **写锁饥饿** 问题

解决方案：
- 外层加公平锁
- 或控制读锁并发

---

## 六、锁类型选型建议（实战总结）

| 业务场景 | 推荐锁 |
|----|----|
| 普通并发控制 | 非公平可重入锁 |
| 方法嵌套 | 可重入锁 |
| 读多写少 | 读写锁 |
| 顺序强一致 | 公平锁 |
| 高并发接口 | 非公平锁 |

---

## 七、最佳实践总结

1. **默认使用非公平可重入锁**
2. **读多写少必须考虑读写锁**
3. **公平锁慎用**
4. **锁粒度一定要小**
5. **避免长事务持锁**
6. **能不用锁就不用锁**

---

## 八、结语

Redisson 的锁设计非常成熟，但**选错锁比不用锁更可怕**。  
在实际项目中，应结合：

- 并发量
- 顺序要求
- 性能指标
- 业务容忍度

进行综合选择。
