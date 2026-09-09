package com.peach.virtualthread.concurrency;

import com.peach.virtualthread.config.BackpressurePolicy;
import com.peach.virtualthread.exception.VirtualTaskRejectReason;
import com.peach.virtualthread.exception.VirtualTaskRejectedException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 单个业务组的静态并发控制器。
 *
 * <p>准入容量（Admission）等于 {@code maxConcurrency + maxPending}，用于在创建虚拟线程之前
 * 限制进入当前业务组的活动任务总数；执行容量（Execution）等于 {@code maxConcurrency}，用于
 * 限制真正进入业务逻辑的任务数量。无法立即获得执行许可的已准入任务进入 Pending 状态。</p>
 *
 * <p>控制器只负责许可获取，不直接释放许可。所有许可统一由任务对应的 {@link PermitLedger}
 * exactly-once 归还，避免成功、取消、中断和关闭等竞态路径导致信号量漂移。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:40
 */
public final class GroupConcurrencyController {

    private final String group;
    private final int maxConcurrency;
    private final int maxPending;
    private final BackpressurePolicy backpressure;
    private final long acquireTimeoutNanos;
    private final Semaphore admissionSemaphore;
    private final Semaphore executionSemaphore;

    /**
     * 创建静态业务组并发控制器。
     *
     * @param group 业务组名称
     * @param maxConcurrency 最大执行并发数
     * @param maxPending 最大 Pending 数量
     * @param backpressure Admission 容量耗尽后的背压策略
     * @param acquireTimeout BLOCK 策略最大等待时间
     * @throws NullPointerException group 或 backpressure 为 null 时抛出
     * @throws IllegalArgumentException 并发边界非法、BLOCK 超时时间非法或 Duration 溢出时抛出
     */
    public GroupConcurrencyController(String group,
                                      int maxConcurrency,
                                      int maxPending,
                                      BackpressurePolicy backpressure,
                                      Duration acquireTimeout) {
        this.group = Objects.requireNonNull(group, "group");
        this.maxConcurrency = maxConcurrency;
        this.maxPending = maxPending;
        this.backpressure = Objects.requireNonNull(backpressure, "backpressure");
        this.acquireTimeoutNanos = toAcquireTimeoutNanos(acquireTimeout);
        long maxInFlight = (long) maxConcurrency + maxPending;
        if (maxConcurrency <= 0 || maxPending < 0 || maxInFlight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid virtual thread capacity for group=" + group);
        }
        if (backpressure == BackpressurePolicy.BLOCK && acquireTimeoutNanos <= 0L) {
            throw new IllegalArgumentException("BLOCK backpressure requires a positive acquire timeout: " + group);
        }
        this.admissionSemaphore = new Semaphore((int) maxInFlight, false);
        this.executionSemaphore = new Semaphore(maxConcurrency, false);
    }

    /**
     * 获取 Admission Permit。
     *
     * <p>REJECT 策略立即尝试；BLOCK 策略在提交线程上进行有界、可中断等待。被中断时恢复
     * interrupt 标志后以标准 RejectedExecutionException 子类向上传播。</p>
     *
     * @return 已持有 Admission Permit 的任务账本
     * @throws VirtualTaskRejectedException 容量耗尽、等待超时或线程被中断时抛出
     */
    public PermitLedger acquireAdmission() {
        boolean acquired;
        if (backpressure == BackpressurePolicy.REJECT) {
            acquired = admissionSemaphore.tryAcquire();
            if (!acquired) {
                throw rejected(VirtualTaskRejectReason.CAPACITY_FULL,
                        "Virtual thread group capacity is full. group=" + group);
            }
        } else {
            try {
                acquired = admissionSemaphore.tryAcquire(acquireTimeoutNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new VirtualTaskRejectedException(group, VirtualTaskRejectReason.INTERRUPTED,
                        "Interrupted while waiting for virtual thread admission. group=" + group, ex);
            }
            if (!acquired) {
                throw rejected(VirtualTaskRejectReason.ACQUIRE_TIMEOUT,
                        "Timed out while waiting for virtual thread admission. group=" + group);
            }
        }

        PermitLedger ledger = new PermitLedger(admissionSemaphore, executionSemaphore);
        ledger.markAdmissionAcquired();
        return ledger;
    }

    /**
     * 非阻塞尝试预占 Execution Permit。
     *
     * <p>提交阶段先尝试预占执行许可，只有无法预占的已准入任务才进入 PENDING，从而严格保证
     * Pending 数量不会超过 {@code maxPending}。</p>
     *
     * @param ledger 当前任务 Permit 账本
     * @return true 表示已经预占 Execution Permit
     */
    public boolean tryAcquireExecution(PermitLedger ledger) {
        Objects.requireNonNull(ledger, "ledger");
        if (!executionSemaphore.tryAcquire()) {
            return false;
        }
        ledger.markExecutionAcquired();
        return true;
    }

    /**
     * 在已准入虚拟线程中等待 Execution Permit。
     *
     * <p>该等待发生在数量已被 maxPending 严格限制的虚拟线程中，不阻塞业务请求线程；等待仍然
     * 是可中断的，以便 Future.cancel(true) 或 shutdownNow() 能够及时结束 Pending 任务。</p>
     *
     * @param ledger 当前任务 Permit 账本
     * @throws InterruptedException 等待期间虚拟线程被中断时抛出
     */
    public void acquireExecutionInterruptibly(PermitLedger ledger) throws InterruptedException {
        Objects.requireNonNull(ledger, "ledger");
        executionSemaphore.acquire();
        ledger.markExecutionAcquired();
    }

    /**
     * 获取最大执行并发数。
     *
     * @return 最大执行并发数
     */
    public int maxConcurrency() {
        return maxConcurrency;
    }

    /**
     * 获取最大 Pending 任务数。
     *
     * @return 最大 Pending 任务数
     */
    public int maxPending() {
        return maxPending;
    }

    /**
     * 获取当前业务组可接受的最大活动任务总数。
     *
     * @return {@code maxConcurrency + maxPending}
     */
    public int maxInFlight() {
        return maxConcurrency + maxPending;
    }

    /**
     * 获取当前剩余准入许可数量，仅用于测试和只读诊断。
     *
     * @return 剩余准入许可数量
     */
    public int availableAdmissionPermits() {
        return admissionSemaphore.availablePermits();
    }

    /**
     * 获取当前剩余执行许可数量，仅用于测试和只读诊断。
     *
     * @return 剩余执行许可数量
     */
    public int availableExecutionPermits() {
        return executionSemaphore.availablePermits();
    }

    private static long toAcquireTimeoutNanos(Duration acquireTimeout) {
        if (acquireTimeout == null) {
            return 0L;
        }
        try {
            return acquireTimeout.toNanos();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("acquireTimeout exceeds supported duration range", ex);
        }
    }

    private VirtualTaskRejectedException rejected(VirtualTaskRejectReason reason, String message) {
        return new VirtualTaskRejectedException(group, reason, message);
    }
}
