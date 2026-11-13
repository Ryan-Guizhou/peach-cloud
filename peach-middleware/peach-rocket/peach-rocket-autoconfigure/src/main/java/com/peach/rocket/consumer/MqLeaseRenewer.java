package com.peach.rocket.consumer;

import com.peach.rocket.autoconfigure.PeachRocketProperties;
import com.peach.rocket.exception.MqException;
import com.peach.rocket.idempotent.MqIdempotentClaim;
import com.peach.rocket.idempotent.MqIdempotentContext;
import com.peach.rocket.idempotent.MqIdempotentStore;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 容器管理的单线程续租时钟，待续租数量受消费并发预算限制。
 *
 * <p>失租后禁止成功确认，但不能撤销业务已经发生的数据库或外部副作用。
 * 时钟使用单独的平台线程，避免把周期等待放进业务虚拟线程。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 15:00
 */
@Slf4j
public final class MqLeaseRenewer implements AutoCloseable {

    private final MqIdempotentStore store;
    private final PeachRocketProperties properties;
    private final Semaphore capacity;
    private final ScheduledThreadPoolExecutor scheduler;

    public MqLeaseRenewer(MqIdempotentStore store, PeachRocketProperties properties) {
        this.store = store;
        this.properties = properties;
        this.capacity = new Semaphore(properties.getConsumer().getMaxInflight());
        this.scheduler = new ScheduledThreadPoolExecutor(1, task -> {
            Thread thread = new Thread(task, "peach-mq-lease-renewal");
            thread.setDaemon(true);
            return thread;
        });
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
        scheduler.setContinueExistingPeriodicTasksAfterShutdownPolicy(false);
    }

    /**
     * 仅为声明支持续租的 Store 建立有界任务；必须在 Handler 结束后关闭。
     *
     * @param context 当前幂等记录
     * @param claim 当前认领凭据
     * @return 可检查并释放的租约句柄
     */
    public Lease open(MqIdempotentContext context, MqIdempotentClaim claim) {
        if (!store.supportsLeaseRenewal()) {
            if (properties.isProduction()) {
                throw new MqException("Production idempotency store requires lease renewal");
            }
            return Lease.NOOP;
        }
        if (!capacity.tryAcquire()) {
            throw new MqException("MQ lease renewal capacity reached");
        }
        AtomicBoolean valid = new AtomicBoolean(true);
        Runnable renew = () -> {
            try {
                if (!store.renew(context, claim) && valid.compareAndSet(true, false)) {
                    log.warn("[mq-lease] renewal rejected; consumer will not confirm success");
                }
            } catch (RuntimeException ex) {
                if (valid.compareAndSet(true, false)) {
                    log.warn("[mq-lease] renewal failed; consumer will not confirm success. errorType={}",
                            ex.getClass().getName());
                }
            }
        };
        long interval = Math.max(1, properties.getJdbc().getProcessingLease().toMillis() / 3);
        try {
            ScheduledFuture<?> future = scheduler.scheduleWithFixedDelay(
                    renew, interval, interval, TimeUnit.MILLISECONDS);
            return new Lease() {
                private final AtomicBoolean closed = new AtomicBoolean();

                @Override
                public void checkValid() {
                    renew.run();
                    if (!valid.get()) {
                        throw new MqException("MQ processing lease was lost");
                    }
                }

                @Override
                public void close() {
                    if (closed.compareAndSet(false, true)) {
                        future.cancel(false);
                        capacity.release();
                    }
                }
            };
        } catch (RuntimeException ex) {
            capacity.release();
            throw new MqException("MQ lease renewal scheduler is unavailable", ex);
        }
    }

    /**
     * 关闭时钟并有界等待，SDK Consumer 应先停止准入并排空。
     */
    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(properties.getConsumer().getShutdownDrainTimeout().toMillis(),
                    TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            scheduler.shutdownNow();
        }
    }

    /**
     * 当前消费的租约句柄；close 幂等，不拥有业务事务。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/11/13 15:00
     */
    public interface Lease extends AutoCloseable {

        Lease NOOP = new Lease() {
            @Override
            public void checkValid() {
            }

            @Override
            public void close() {
            }
        };

        void checkValid();

        @Override
        void close();
    }
}
