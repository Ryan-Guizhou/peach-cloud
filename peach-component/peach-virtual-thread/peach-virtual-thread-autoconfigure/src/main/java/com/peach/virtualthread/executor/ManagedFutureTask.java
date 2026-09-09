package com.peach.virtualthread.executor;

import com.peach.virtualthread.task.TaskControl;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 可与 {@link TaskControl} 联动取消的 FutureTask。
 *
 * <p>JDK FutureTask 在真正调用 run() 之前没有业务 runner 概念，而 V4 的 Pending 虚拟线程可能
 * 正在等待 Execution Permit。该实现将 Future 取消同步给 TaskControl，使 Pending 内部等待也能
 * 被及时中断，但仍不允许取消线程直接释放 Execution Permit。</p>
 *
 * @param <V> Future 返回值类型
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public final class ManagedFutureTask<V> extends FutureTask<V> implements TaskOutcome {

    private final AtomicReference<TaskControl> control = new AtomicReference<>();
    private final AtomicBoolean cancellationRequested = new AtomicBoolean();
    private final AtomicBoolean interruptOnCancel = new AtomicBoolean();
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    /**
     * 创建有返回值的受管 FutureTask。
     *
     * @param callable 实际业务 Callable
     */
    public ManagedFutureTask(Callable<V> callable) {
        super(callable);
    }

    /**
     * 创建无返回值任务对应的受管 FutureTask。
     *
     * @param runnable 实际业务 Runnable
     * @param result 任务成功完成后由 Future 返回的结果
     */
    public ManagedFutureTask(Runnable runnable, V result) {
        super(runnable, result);
    }

    /**
     * 绑定当前提交任务的 TaskControl。
     *
     * @param taskControl 任务控制对象
     */
    public void attachControl(TaskControl taskControl) {
        if (!control.compareAndSet(null, taskControl)) {
            throw new IllegalStateException("ManagedFutureTask control is already attached");
        }
        if (cancellationRequested.get()) {
            taskControl.cancel(interruptOnCancel.get());
        }
    }

    /**
     * 取消 Future 并把取消请求同步给任务控制对象。
     *
     * <p>该方法不直接释放 Permit；Permit 仍由实际 runner 的 finally 统一归还。</p>
     *
     * @param mayInterruptIfRunning 是否允许中断已经开始运行的任务
     * @return true 表示 Future 成功进入取消状态
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        boolean cancelled = super.cancel(mayInterruptIfRunning);
        if (cancelled) {
            cancellationRequested.set(true);
            if (mayInterruptIfRunning) {
                interruptOnCancel.set(true);
            }
            TaskControl taskControl = control.get();
            if (taskControl != null) {
                taskControl.cancel(mayInterruptIfRunning);
            }
        }
        return cancelled;
    }

    @Override
    protected void setException(Throwable throwable) {
        failure.compareAndSet(null, throwable);
        super.setException(throwable);
    }

    /**
     * 获取 FutureTask 捕获到的原始业务异常。
     *
     * @return 原始异常；任务未失败时返回 null
     */
    @Override
    public Throwable failure() {
        return failure.get();
    }

    /**
     * 判断 Future 是否已经取消。
     *
     * @return true 表示 Future 已取消
     */
    @Override
    public boolean cancelled() {
        return isCancelled();
    }
}
