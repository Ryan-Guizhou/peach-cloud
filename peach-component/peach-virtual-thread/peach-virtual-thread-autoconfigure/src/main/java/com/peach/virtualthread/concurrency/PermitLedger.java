package com.peach.virtualthread.concurrency;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 单任务 Permit 所有权账本。
 *
 * <p>账本只跟踪 Admission 与 Execution 两种许可。任务成功获取许可后先写入账本，
 * 释放时通过 CAS 清除所有权位，只有成功清除的线程才能调用 {@link Semaphore#release()}。
 * 该约束用于防止 success、cancel、interrupt、shutdown 等竞态路径重复归还许可导致并发上限漂移。</p>
 *
 * <p>注意：Future 完成或取消本身不能提前释放 Execution Permit；只有实际运行该任务的线程
 * 退出执行路径后才能归还真实执行容量。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:40
 */
public final class PermitLedger {

    private static final int ADMISSION = 1;
    private static final int EXECUTION = 1 << 1;

    private final Semaphore admissionSemaphore;
    private final Semaphore executionSemaphore;
    private final AtomicInteger owned = new AtomicInteger();

    /**
     * 创建任务 Permit 账本。
     *
     * @param admissionSemaphore 准入许可信号量
     * @param executionSemaphore 执行许可信号量
     * @throws NullPointerException 任一信号量为 null 时抛出
     */
    public PermitLedger(Semaphore admissionSemaphore, Semaphore executionSemaphore) {
        this.admissionSemaphore = Objects.requireNonNull(admissionSemaphore, "admissionSemaphore");
        this.executionSemaphore = Objects.requireNonNull(executionSemaphore, "executionSemaphore");
    }

    /**
     * 标记已经成功获取 Admission Permit。
     *
     * @throws IllegalStateException 当前任务已经持有 Admission Permit 时抛出
     */
    public void markAdmissionAcquired() {
        markOwned(ADMISSION, "Admission permit is already owned by this task");
    }

    /**
     * 标记已经成功获取 Execution Permit。
     *
     * @throws IllegalStateException 当前任务已经持有 Execution Permit 时抛出
     */
    public void markExecutionAcquired() {
        markOwned(EXECUTION, "Execution permit is already owned by this task");
    }

    /**
     * 是否仍持有 Admission Permit。
     *
     * @return true 表示当前任务仍拥有 Admission Permit
     */
    public boolean ownsAdmission() {
        return (owned.get() & ADMISSION) != 0;
    }

    /**
     * 是否仍持有 Execution Permit。
     *
     * @return true 表示当前任务仍拥有 Execution Permit
     */
    public boolean ownsExecution() {
        return (owned.get() & EXECUTION) != 0;
    }

    /**
     * 仅在首次释放时真正归还 Admission Permit。
     *
     * @return true 表示本次调用完成了真实释放，false 表示该 Permit 已被其他路径释放
     */
    public boolean releaseAdmission() {
        if (clearOwned(ADMISSION)) {
            admissionSemaphore.release();
            return true;
        }
        return false;
    }

    /**
     * 仅在首次释放时真正归还 Execution Permit。
     *
     * @return true 表示本次调用完成了真实释放，false 表示该 Permit 已被其他路径释放
     */
    public boolean releaseExecution() {
        if (clearOwned(EXECUTION)) {
            executionSemaphore.release();
            return true;
        }
        return false;
    }

    private void markOwned(int bit, String message) {
        while (true) {
            int current = owned.get();
            if ((current & bit) != 0) {
                throw new IllegalStateException(message);
            }
            if (owned.compareAndSet(current, current | bit)) {
                return;
            }
        }
    }

    private boolean clearOwned(int bit) {
        while (true) {
            int current = owned.get();
            if ((current & bit) == 0) {
                return false;
            }
            if (owned.compareAndSet(current, current & ~bit)) {
                return true;
            }
        }
    }
}
