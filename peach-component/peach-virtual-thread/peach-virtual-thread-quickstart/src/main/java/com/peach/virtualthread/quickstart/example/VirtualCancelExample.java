package com.peach.virtualthread.quickstart.example;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 使用受管 {@code supplyAsync} 提交可中断任务，再通过 {@code cancel(true)} 联动虚拟线程中断。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
public class VirtualCancelExample {

    private static final long AWAIT_SECONDS = 3L;

    private final VirtualExecutorService databaseExecutor;

    public VirtualCancelExample(@VirtualGroup("database") VirtualExecutorService databaseExecutor) {
        this.databaseExecutor = databaseExecutor;
    }

    /**
     * 启动一个阻塞任务后立刻取消，等待 runner 退出并确认 Future 进入 cancelled。
     *
     * @return 取消观察结果
     */
    public CancelObservation cancelManagedSupply() {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch exited = new CountDownLatch(1);
        CompletableFuture<String> future = databaseExecutor.supplyAsync(() -> {
            started.countDown();
            try {
                Thread.sleep(30_000L);
                return "should-not-complete";
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", ex);
            } finally {
                exited.countDown();
            }
        });

        await(started, "cancel demo start");
        boolean requested = future.cancel(true);
        await(exited, "cancelled runner exit");
        log.info("managed cancel finished, requested={}, futureCancelled={}", requested, future.isCancelled());
        return new CancelObservation(requested, future.isCancelled());
    }

    private static void await(CountDownLatch latch, String step) {
        try {
            if (!latch.await(AWAIT_SECONDS, TimeUnit.SECONDS)) {
                throw new IllegalStateException(step + " timed out");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(step + " interrupted", ex);
        }
    }

    /**
     * 受管 {@link CompletableFuture#cancel(boolean)} 的观察结果。
     */
    public static final class CancelObservation {

        private final boolean cancelRequested;
        private final boolean futureCancelled;

        public CancelObservation(boolean cancelRequested, boolean futureCancelled) {
            this.cancelRequested = cancelRequested;
            this.futureCancelled = futureCancelled;
        }

        public boolean isCancelRequested() {
            return cancelRequested;
        }

        public boolean isFutureCancelled() {
            return futureCancelled;
        }
    }
}
