package com.peach.virtualthread.executor;

import com.peach.virtualthread.task.TaskControl;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Starter 便捷 CompletableFuture API 的内部受管任务。
 *
 * <p>该任务保持 {@link CompletableFuture} 的标准完成和异常传播语义，同时把取消请求同步到
 * {@link TaskControl}。这样 {@code future.cancel(true)} 不仅改变 Future 状态，也能中断处于
 * Pending 或 Running 状态的实际虚拟线程；真实 Permit 仍只由 runner 的清理路径归还。</p>
 *
 * <p>任务不会记录或包装业务参数、返回结果和原始异常，失败结果仅通过 CompletableFuture
 * 与 {@link TaskOutcome} 向调用方和执行器状态统计暴露。</p>
 *
 * @param <T> 任务结果类型
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
final class ManagedCompletableTask<T> implements Runnable, TaskOutcome {

    private final Supplier<T> supplier;
    private final AtomicReference<TaskControl> control = new AtomicReference<>();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final AtomicBoolean interruptOnCancel = new AtomicBoolean();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();
    private final ManagedCompletableFuture<T> future = new ManagedCompletableFuture<>();

    /**
     * 创建受管 CompletableFuture 任务。
     *
     * @param supplier 实际业务 Supplier
     */
    ManagedCompletableTask(Supplier<T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    /**
     * 绑定执行器为当前任务创建的控制对象。
     *
     * <p>取消请求可能早于控制对象绑定发生，因此绑定完成后必须补发之前已经记录的取消请求。</p>
     *
     * @param taskControl 当前任务控制对象
     */
    void attachControl(TaskControl taskControl) {
        Objects.requireNonNull(taskControl, "taskControl");
        if (!control.compareAndSet(null, taskControl)) {
            throw new IllegalStateException("ManagedCompletableTask control is already attached");
        }
        if (cancellationRequested.get()) {
            taskControl.cancel(interruptOnCancel.get());
        }
    }

    /**
     * 执行业务 Supplier，并将结果或原始异常写入受管 CompletableFuture。
     */
    @Override
    public void run() {
        if (future.isCancelled()) {
            return;
        }
        try {
            future.complete(supplier.get());
        } catch (Throwable throwable) {
            failure.compareAndSet(null, throwable);
            future.completeExceptionally(throwable);
        }
    }

    /**
     * 由执行器生命周期主动取消当前 CompletableFuture。
     *
     * @param mayInterruptIfRunning 是否允许中断已经开始执行的业务任务
     * @return true 表示本次调用成功把 Future 迁移到取消状态
     */
    boolean cancel(boolean mayInterruptIfRunning) {
        return future.cancel(mayInterruptIfRunning);
    }

    /**
     * 获取对业务调用方暴露的 CompletableFuture。
     *
     * @return 与当前受管任务绑定的 CompletableFuture
     */
    CompletableFuture<T> future() {
        return future;
    }

    /**
     * 获取任务执行期间捕获到的原始异常。
     *
     * @return 原始异常；任务未失败时返回 null
     */
    @Override
    public Throwable failure() {
        return failure.get();
    }

    /**
     * 判断受管 CompletableFuture 是否已经取消。
     *
     * @return true 表示 Future 已取消
     */
    @Override
    public boolean cancelled() {
        return future.isCancelled();
    }

    /**
     * 将 CompletableFuture 的取消请求同步给任务控制对象。
     *
     * <p>JDK 默认 CompletableFuture 不负责中断执行它的线程；Starter 在这里显式建立取消联动，
     * 但仍不在取消线程中释放任何 Permit，避免底层调用不响应中断时突破业务组并发上限。</p>
     */
    private final class ManagedCompletableFuture<V> extends CompletableFuture<V> {

        /**
         * 取消 Future，并把取消请求同步给当前任务控制对象。
         *
         * @param mayInterruptIfRunning 是否允许中断实际虚拟线程
         * @return true 表示 Future 成功进入取消状态
         */
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            boolean cancelled = super.cancel(mayInterruptIfRunning);
            if (!cancelled) {
                return false;
            }
            cancellationRequested.set(true);
            if (mayInterruptIfRunning) {
                interruptOnCancel.set(true);
            }
            TaskControl taskControl = control.get();
            if (taskControl != null) {
                taskControl.cancel(mayInterruptIfRunning);
            }
            return true;
        }
    }
}
