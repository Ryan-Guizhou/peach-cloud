package com.peach.rocket.producer;

import com.peach.rocket.exception.MqException;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 本地生产者并发准入，不额外增加线程池或消息缓存队列。
 *
 * <p>Permit 具备幂等释放语义，避免 SDK 回调与发送异常竞态时重复释放配额。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13
 */
public final class MqProducerAdmission {
    private final Semaphore permits;
    private final long waitNanos;
    private final int capacity;
    private volatile boolean accepting = true;

    /**
     * @param maxInflight 同一 Producer 实例最大执行中请求数
     * @param acquireTimeout 获取额度允许等待的最长时间
     */
    public MqProducerAdmission(int maxInflight, Duration acquireTimeout) {
        if (maxInflight < 1 || acquireTimeout == null || acquireTimeout.isNegative()) {
            throw new IllegalArgumentException("Invalid RocketMQ producer admission limits");
        }
        permits = new Semaphore(maxInflight, true);
        capacity = maxInflight;
        waitNanos = acquireTimeout.toNanos();
    }

    /**
     * 有界获取发送额度。
     *
     * @param mode 发送模式（仅用于诊断日志）
     * @return 需要在完成发送后关闭的令牌
     */
    public Permit acquire(String mode) {
        try {
            if (!accepting) {
                throw new MqException("RocketMQ producer is stopping");
            }
            if (!permits.tryAcquire(waitNanos, TimeUnit.NANOSECONDS)) {
                throw new MqException("RocketMQ producer inflight limit reached, mode=" + mode);
            }
            if (!accepting) {
                permits.release();
                throw new MqException("RocketMQ producer is stopping");
            }
            return new Permit(permits);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new MqException("Interrupted while acquiring RocketMQ producer permit", ex);
        }
    }

    /** 禁止新发送，并在有界时间内等待所有 Permit 归还。 */
    public boolean closeAndDrain(Duration timeout) {
        accepting = false;
        try {
            if (!permits.tryAcquire(capacity, timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                return false;
            }
            permits.release(capacity);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** 生命周期重启时重新开放，不创建新的 SDK 资源。 */
    public void open() { accepting = true; }

    /**
     * 幂等额度令牌，回调与同步异常均可调用 close()。
     */
    public static final class Permit implements AutoCloseable {
        private final Semaphore semaphore;
        private final AtomicBoolean released = new AtomicBoolean();

        private Permit(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                semaphore.release();
            }
        }
    }
}
