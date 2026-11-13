package com.peach.rocket.consumer;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 进程内动态 Consumer 共享的准入与排空控制。
 *
 * <p>使用有界信号量限制正在执行的 Handler 数量，不额外创建线程或消息缓存队列。
 * 停机后拒绝新任务；已进入的任务可在超时范围内完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
final class MqConsumerAdmission {

    private final Semaphore permits;
    private final long acquireTimeoutMs;
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition drained = lock.newCondition();
    private boolean accepting;
    private int active;

    MqConsumerAdmission(int maxInflight, Duration acquireTimeout) {
        if (maxInflight < 1 || acquireTimeout == null || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("Invalid RocketMQ consumer admission limits");
        }
        this.permits = new Semaphore(maxInflight);
        this.acquireTimeoutMs = acquireTimeout.toMillis();
    }

    /**
     * 开启准入；在 Spring 容器启动或重启动态消费者前调用。
     */
    void open() {
        lock.lock();
        try {
            accepting = true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 在有界超时时间内获取执行额度；停机或容量耗尽时返回 false。
     *
     * @return 本次消息是否已获得准入额度
     */
    boolean tryEnter() {
        lock.lock();
        try {
            if (!accepting) {
                return false;
            }
        } finally {
            lock.unlock();
        }
        try {
            if (!permits.tryAcquire(acquireTimeoutMs, TimeUnit.MILLISECONDS)) {
                return false;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
        lock.lock();
        try {
            if (!accepting) {
                permits.release();
                return false;
            }
            active++;
            return true;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 释放业务 Handler 执行额度，失败/异常路径同样必须调用。
     */
    void leave() {
        lock.lock();
        try {
            active--;
            if (active == 0) {
                drained.signalAll();
            }
        } finally {
            lock.unlock();
            permits.release();
        }
    }

    /**
     * 停止新的业务消息进入。执行中的任务继续保有额度。
     */
    void close() {
        lock.lock();
        try {
            accepting = false;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 等待进入执行阶段的任务排空；超时或线程中断不会无限阻塞停机。
     *
     * @param timeout 最大等待时间
     * @return true 表示已经排空
     */
    boolean awaitDrained(Duration timeout) {
        long remaining = timeout.toNanos();
        lock.lock();
        try {
            while (active > 0 && remaining > 0) {
                try {
                    remaining = drained.awaitNanos(remaining);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
            return active == 0;
        } finally {
            lock.unlock();
        }
    }

    int active() {
        lock.lock();
        try {
            return active;
        } finally {
            lock.unlock();
        }
    }
}
