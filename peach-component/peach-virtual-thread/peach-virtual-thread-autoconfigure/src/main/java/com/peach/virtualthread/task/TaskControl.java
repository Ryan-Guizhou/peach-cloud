package com.peach.virtualthread.task;

import com.peach.virtualthread.concurrency.PermitLedger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 单个虚拟线程任务的并发状态控制对象。
 *
 * <p>该对象只管理任务状态、runner 引用、时间戳和 Permit 所有权，不保存业务 Runnable、Callable
 * 或业务参数。状态迁移全部通过 CAS 完成，避免 success、failure、cancel 等竞态产生多个终态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:50
 */
public final class TaskControl {

    private final long taskId;
    private final String group;
    private final PermitLedger permitLedger;
    private final long submittedNanos;
    private final AtomicReference<VirtualTaskState> state = new AtomicReference<>(VirtualTaskState.CREATED);
    private final AtomicReference<Thread> runner = new AtomicReference<>();
    private final AtomicBoolean interruptRequested = new AtomicBoolean();
    private volatile long startedNanos;

    /**
     * 创建任务状态控制对象。
     *
     * @param taskId 任务 ID
     * @param group 业务组
     * @param permitLedger 当前任务 Permit 账本
     * @param submittedNanos 提交时间，基于 System.nanoTime
     */
    public TaskControl(long taskId, String group, PermitLedger permitLedger, long submittedNanos) {
        this.taskId = taskId;
        this.group = Objects.requireNonNull(group, "group");
        this.permitLedger = Objects.requireNonNull(permitLedger, "permitLedger");
        this.submittedNanos = submittedNanos;
    }

    /**
     * 将任务标记为等待 Execution Permit。
     *
     * @return true 表示状态由 CREATED 成功迁移到 PENDING
     */
    public boolean markPending() {
        return state.compareAndSet(VirtualTaskState.CREATED, VirtualTaskState.PENDING);
    }

    /**
     * 将任务标记为 RUNNING。
     *
     * <p>预占了 Execution Permit 的任务从 CREATED 进入 RUNNING；真正等待过许可的任务从
     * PENDING 进入 RUNNING。若任务已被取消则不会重新进入 RUNNING。</p>
     *
     * @return true 表示成功进入 RUNNING
     */
    public boolean markRunning() {
        while (true) {
            VirtualTaskState current = state.get();
            if (current != VirtualTaskState.CREATED && current != VirtualTaskState.PENDING) {
                return false;
            }
            if (state.compareAndSet(current, VirtualTaskState.RUNNING)) {
                startedNanos = System.nanoTime();
                return true;
            }
        }
    }

    /**
     * 将 RUNNING 任务标记为成功完成。
     *
     * @return true 表示 COMPLETED 成为唯一终态
     */
    public boolean markCompleted() {
        return state.compareAndSet(VirtualTaskState.RUNNING, VirtualTaskState.COMPLETED);
    }

    /**
     * 将任务标记为失败。
     *
     * <p>允许在 CREATED/PENDING/RUNNING 阶段进入 FAILED，以覆盖虚拟线程创建、启动以及业务
     * 执行阶段的失败。已进入其他终态时不会覆盖原终态。</p>
     *
     * @return true 表示 FAILED 成为唯一终态
     */
    public boolean markFailed() {
        while (true) {
            VirtualTaskState current = state.get();
            if (isTerminal(current)) {
                return false;
            }
            if (state.compareAndSet(current, VirtualTaskState.FAILED)) {
                return true;
            }
        }
    }

    /**
     * 取消任务。
     *
     * <p>对于 CREATED/PENDING 状态，即使调用方传入 {@code mayInterruptIfRunning=false}，也会
     * 中断仅用于等待 Execution Permit 的内部虚拟线程，因为此时业务 Runnable 尚未开始。
     * 对已经 RUNNING 的业务任务则严格遵循 mayInterruptIfRunning。</p>
     *
     * @param mayInterruptIfRunning 是否允许中断已经开始执行的业务任务
     * @return true 表示 CANCELLED 成为唯一终态
     */
    public boolean cancel(boolean mayInterruptIfRunning) {
        while (true) {
            VirtualTaskState current = state.get();
            if (isTerminal(current)) {
                return false;
            }
            boolean shouldInterrupt = current != VirtualTaskState.RUNNING || mayInterruptIfRunning;
            if (state.compareAndSet(current, VirtualTaskState.CANCELLED)) {
                if (shouldInterrupt) {
                    requestInterrupt();
                }
                return true;
            }
        }
    }

    /**
     * 绑定实际运行当前任务的虚拟线程。
     *
     * <p>如果取消/停止请求早于线程绑定发生，绑定后立即补发 interrupt，避免任务在 Pending
     * 阶段继续占用 Admission 容量。</p>
     *
     * @param thread 当前任务虚拟线程
     */
    public void attachRunner(Thread thread) {
        Objects.requireNonNull(thread, "thread");
        if (!runner.compareAndSet(null, thread)) {
            throw new IllegalStateException("Task runner is already attached: " + taskId);
        }
        if (interruptRequested.get()) {
            thread.interrupt();
        }
    }

    /**
     * 请求中断当前 runner；尚未绑定 runner 时记录请求，由 attachRunner 补发。
     */
    public void requestInterrupt() {
        interruptRequested.set(true);
        Thread thread = runner.get();
        if (thread != null) {
            thread.interrupt();
        }
    }

    /**
     * 生成当前任务的安全诊断快照。
     *
     * @return 不包含业务载荷的任务快照
     */
    public VirtualTaskSnapshot snapshot() {
        Thread thread = runner.get();
        long now = System.nanoTime();
        long base = startedNanos > 0L ? startedNanos : submittedNanos;
        return new VirtualTaskSnapshot(
                taskId,
                group,
                state.get(),
                thread == null ? 0L : thread.threadId(),
                thread == null ? null : thread.getName(),
                thread != null && thread.isVirtual(),
                thread == null ? null : thread.getState(),
                submittedNanos,
                startedNanos,
                Math.max(0L, now - base));
    }

    /**
     * 获取组内任务 ID。
     *
     * @return 任务 ID
     */
    public long taskId() {
        return taskId;
    }

    /**
     * 获取任务所属业务组。
     *
     * @return 业务组名称
     */
    public String group() {
        return group;
    }

    /**
     * 获取当前任务的 Permit 所有权账本。
     *
     * @return Permit 账本
     */
    public PermitLedger permitLedger() {
        return permitLedger;
    }

    /**
     * 获取当前任务状态。
     *
     * @return 当前原子状态
     */
    public VirtualTaskState state() {
        return state.get();
    }

    /**
     * 获取承载当前任务的虚拟线程。
     *
     * @return 已绑定 runner；尚未绑定时返回 null
     */
    public Thread runner() {
        return runner.get();
    }

    /**
     * 获取任务提交时间。
     *
     * @return 基于 {@link System#nanoTime()} 的提交时间
     */
    public long submittedNanos() {
        return submittedNanos;
    }

    /**
     * 获取任务开始执行业务逻辑的时间。
     *
     * @return 基于 {@link System#nanoTime()} 的开始时间；尚未运行时为 0
     */
    public long startedNanos() {
        return startedNanos;
    }

    /**
     * 判断任务是否已经进入互斥终态。
     *
     * @return true 表示状态为 COMPLETED、FAILED 或 CANCELLED
     */
    public boolean isTerminal() {
        return isTerminal(state.get());
    }

    private static boolean isTerminal(VirtualTaskState value) {
        return value == VirtualTaskState.COMPLETED
                || value == VirtualTaskState.FAILED
                || value == VirtualTaskState.CANCELLED;
    }
}
