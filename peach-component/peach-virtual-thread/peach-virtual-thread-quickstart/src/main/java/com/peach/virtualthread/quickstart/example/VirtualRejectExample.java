package com.peach.virtualthread.quickstart.example;

import com.peach.virtualthread.annotation.VirtualGroup;
import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.exception.VirtualTaskRejectReason;
import com.peach.virtualthread.exception.VirtualTaskRejectedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 在 {@code burst} 组（{@code max-concurrency=1}、{@code max-pending=0}、{@code REJECT}）上演示容量耗尽立即拒绝。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Service
public class VirtualRejectExample {

    public static final String GROUP = "burst";

    private static final long AWAIT_SECONDS = 3L;

    private final VirtualExecutorService burstExecutor;

    public VirtualRejectExample(@VirtualGroup(GROUP) VirtualExecutorService burstExecutor) {
        this.burstExecutor = burstExecutor;
    }

    /**
     * 先占满 Admission，再提交第二条任务，期望立即收到 {@link VirtualTaskRejectedException}。
     *
     * @return 拒绝观察结果
     */
    public RejectObservation rejectWhenCapacityFull() {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        long rejectedBefore = burstExecutor.snapshot().rejected();

        Future<String> held = burstExecutor.submit(() -> {
            started.countDown();
            if (!release.await(5L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("held burst task was not released");
            }
            return "held";
        });

        await(started, "reject demo start");
        VirtualTaskRejectedException rejected = expectReject();
        release.countDown();
        awaitHeld(held);

        long rejectedDelta = burstExecutor.snapshot().rejected() - rejectedBefore;
        log.info("reject finished, group={}, reason={}, rejectedDelta={}",
                rejected.getGroup(), rejected.getReason(), rejectedDelta);
        return new RejectObservation(rejected.getGroup(), rejected.getReason(), rejectedDelta);
    }

    private VirtualTaskRejectedException expectReject() {
        try {
            burstExecutor.submit(() -> "overflow");
        } catch (VirtualTaskRejectedException ex) {
            return ex;
        }
        throw new IllegalStateException("expected VirtualTaskRejectedException when burst capacity is full");
    }

    private static void awaitHeld(Future<String> held) {
        try {
            held.get(AWAIT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception ex) {
            throw new IllegalStateException("held burst task failed", ex);
        }
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
     * REJECT 背压的观察结果。
     */
    public static final class RejectObservation {

        private final String group;
        private final VirtualTaskRejectReason reason;
        private final long rejectedDelta;

        public RejectObservation(String group, VirtualTaskRejectReason reason, long rejectedDelta) {
            this.group = group;
            this.reason = reason;
            this.rejectedDelta = rejectedDelta;
        }

        public String getGroup() {
            return group;
        }

        public VirtualTaskRejectReason getReason() {
            return reason;
        }

        public long getRejectedDelta() {
            return rejectedDelta;
        }
    }
}
