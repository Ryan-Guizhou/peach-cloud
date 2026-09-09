package com.peach.virtualthread.executor;

import com.peach.virtualthread.api.VirtualExecutorService;
import com.peach.virtualthread.concurrency.GroupConcurrencyController;
import com.peach.virtualthread.concurrency.PermitLedger;
import com.peach.virtualthread.config.BackpressurePolicy;
import com.peach.virtualthread.exception.VirtualTaskExceptionContext;
import com.peach.virtualthread.exception.VirtualTaskExceptionHandler;
import com.peach.virtualthread.exception.VirtualTaskRejectReason;
import com.peach.virtualthread.exception.VirtualTaskRejectedException;
import com.peach.virtualthread.state.VirtualExecutorState;
import com.peach.virtualthread.state.VirtualGroupSnapshot;
import com.peach.virtualthread.task.TaskControl;
import com.peach.virtualthread.task.TaskRegistry;
import com.peach.virtualthread.task.VirtualTaskSnapshot;
import com.peach.virtualthread.task.VirtualTaskState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 默认分组虚拟线程执行器。
 *
 * <p>每个已接受任务对应一个 JDK 21 虚拟线程。准入许可（Admission Permit）在创建线程前限制
 * 当前业务组的活动任务总量，执行许可（Execution Permit）限制真正进入业务逻辑的并发任务数。
 * 执行器不池化虚拟线程，也不实现传统 {@code ThreadPoolExecutor} 的核心线程、最大线程和阻塞队列模型。</p>
 *
 * <p>线程安全边界：准入许可和执行许可统一由 {@link PermitLedger} exactly-once 释放；任务状态使用 CAS；
 * 活动任务注册表使用 ConcurrentHashMap；生命周期等待使用 Condition，不进行 busy-spin。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public final class DefaultVirtualExecutorService extends AbstractExecutorService implements VirtualExecutorService {

    private static final Logger log = LoggerFactory.getLogger(DefaultVirtualExecutorService.class);

    private final String group;
    private final int maxConcurrency;
    private final int maxPending;
    private final BackpressurePolicy backpressure;
    private final GroupConcurrencyController concurrency;
    private final VirtualTaskExceptionHandler exceptionHandler;
    private final ThreadFactory threadFactory;
    private final TaskRegistry registry = new TaskRegistry();
    private final AtomicReference<VirtualExecutorState> state = new AtomicReference<>(VirtualExecutorState.RUNNING);
    private final AtomicInteger running = new AtomicInteger();
    private final AtomicInteger pending = new AtomicInteger();
    private final AtomicInteger inFlight = new AtomicInteger();
    private final LongAdder submitted = new LongAdder();
    private final LongAdder completed = new LongAdder();
    private final LongAdder failed = new LongAdder();
    private final LongAdder cancelled = new LongAdder();
    private final LongAdder rejected = new LongAdder();
    private final ReentrantLock terminationLock = new ReentrantLock();
    private final Condition terminatedCondition = terminationLock.newCondition();

    /**
     * 创建默认业务组虚拟线程执行器。
     *
     * @param group 业务组名称
     * @param maxConcurrency 最大执行并发数
     * @param maxPending 最大 Pending 数量
     * @param backpressure 背压策略
     * @param acquireTimeout BLOCK Admission 等待时间
     * @param threadNamePrefix 线程名称前缀
     * @param exceptionHandler execute(Runnable) 未承接异常处理器
     */
    public DefaultVirtualExecutorService(String group,
                                         int maxConcurrency,
                                         int maxPending,
                                         BackpressurePolicy backpressure,
                                         Duration acquireTimeout,
                                         String threadNamePrefix,
                                         VirtualTaskExceptionHandler exceptionHandler) {
        this(group, maxConcurrency, maxPending, backpressure, acquireTimeout, exceptionHandler,
                Thread.ofVirtual()
                        .name(Objects.requireNonNull(threadNamePrefix, "threadNamePrefix") + group + "-", 0)
                        .inheritInheritableThreadLocals(false)
                        .factory());
    }

    /**
     * 创建使用指定 ThreadFactory 的执行器，主要用于框架内部验证线程创建失败等边界。
     */
    DefaultVirtualExecutorService(String group,
                                  int maxConcurrency,
                                  int maxPending,
                                  BackpressurePolicy backpressure,
                                  Duration acquireTimeout,
                                  VirtualTaskExceptionHandler exceptionHandler,
                                  ThreadFactory threadFactory) {
        this.group = Objects.requireNonNull(group, "group");
        this.maxConcurrency = maxConcurrency;
        this.maxPending = maxPending;
        this.backpressure = Objects.requireNonNull(backpressure, "backpressure");
        this.exceptionHandler = Objects.requireNonNull(exceptionHandler, "exceptionHandler");
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory");
        this.concurrency = new GroupConcurrencyController(group, maxConcurrency, maxPending,
                backpressure, acquireTimeout);
        log.info("Virtual thread executor initialized. group={}, maxConcurrency={}, maxPending={}, maxInFlight={}, "
                        + "backpressure={}, acquireTimeout={}",
                group, maxConcurrency, maxPending, (long) maxConcurrency + maxPending, backpressure, acquireTimeout);
    }

    /**
     * 获取当前执行器所属业务组名称。
     *
     * @return 业务组名称
     */
    @Override
    public String groupName() {
        return group;
    }

    /**
     * 提交有返回值任务。
     *
     * <p>显式覆盖该方法是为了让业务侧能够像使用传统 {@code ExecutorService} 一样直接提交任务。
     * 实际 Future 创建仍由 {@link #newTaskFor(Callable)} 统一完成，因此不会绕过任务状态、取消联动、
     * Permit 账本和业务组并发控制。</p>
     *
     * @param task 待执行任务
     * @param <T> 返回值类型
     * @return 受管 Future
     */
    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(task);
    }

    /**
     * 提交无返回值任务。
     *
     * @param task 待执行任务
     * @return 受管 Future
     */
    @Override
    public Future<?> submit(Runnable task) {
        return super.submit(task);
    }

    /**
     * 提交无返回值任务并指定 Future 成功完成时的结果。
     *
     * @param task 待执行任务
     * @param result 任务成功完成后由 Future 返回的结果
     * @param <T> 返回值类型
     * @return 受管 Future
     */
    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return super.submit(task, result);
    }

    /**
     * 使用当前业务组异步执行 Supplier，并返回与任务取消状态联动的 CompletableFuture。
     *
     * <p>业务异常不会在执行器内部吞掉或替换，而是以原始 cause 完成 Future；
     * {@code cancel(true)} 会向实际虚拟线程发出中断请求，但 Permit 只在线程真正退出后归还。</p>
     *
     * @param supplier 待执行 Supplier
     * @param <T> 返回值类型
     * @return 受管 CompletableFuture
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException supplier 为 null 时抛出
     */
    @Override
    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        ManagedCompletableTask<T> task = new ManagedCompletableTask<>(supplier);
        execute(task);
        return task.future();
    }

    /**
     * 使用当前业务组异步执行无返回值任务。
     *
     * @param runnable 待执行任务
     * @return 受管 CompletableFuture
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException runnable 为 null 时抛出
     */
    @Override
    public CompletableFuture<Void> runAsync(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return supplyAsync(() -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 获取当前业务组只读运行快照。
     *
     * @return 不参与并发控制的运行快照
     */
    @Override
    public VirtualGroupSnapshot snapshot() {
        return new VirtualGroupSnapshot(group, state.get(), maxConcurrency, maxPending,
                running.get(), pending.get(), inFlight.get(), submitted.sum(), completed.sum(),
                failed.sum(), cancelled.sum(), rejected.sum());
    }

    /**
     * 获取当前业务组活动任务的安全快照。
     *
     * @return 不包含 Runnable、Callable、业务参数和返回结果的快照列表
     */
    @Override
    public List<VirtualTaskSnapshot> activeTasks() {
        return registry.snapshots();
    }

    /**
     * 直接执行无返回值任务。
     *
     * <p>任务会先经过准入和执行容量控制。由于该入口没有 Future 承接业务异常，未捕获异常会先交给
     * {@link VirtualTaskExceptionHandler} 处理，再继续进入虚拟线程的 uncaught exception 流程。</p>
     *
     * @param command 待执行任务
     * @throws VirtualTaskRejectedException 执行器已关闭、容量已满或 BLOCK 等待失败时抛出
     * @throws NullPointerException command 为 null 时抛出
     */
    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command, "command");
        ensureAccepting();

        PermitLedger ledger;
        try {
            ledger = concurrency.acquireAdmission();
        } catch (VirtualTaskRejectedException ex) {
            rejected.increment();
            // 拒绝属于预期背压结果，避免在容量打满时逐任务写日志；调用方异常与 rejected 指标均可观测。
            throw ex;
        }

        boolean executionReserved = concurrency.tryAcquireExecution(ledger);
        long taskId = registry.nextTaskId();
        TaskControl control = new TaskControl(taskId, group, ledger, System.nanoTime());
        boolean pendingCounted = !executionReserved;
        if (pendingCounted) {
            if (!control.markPending()) {
                cleanupBeforeStart(control, true, false);
                throw new IllegalStateException("Failed to mark virtual task pending: " + taskId);
            }
            pending.incrementAndGet();
        }
        if (command instanceof ManagedFutureTask<?> futureTask) {
            futureTask.attachControl(control);
        } else if (command instanceof ManagedCompletableTask<?> completableTask) {
            completableTask.attachControl(control);
        }

        registry.register(control);
        inFlight.incrementAndGet();

        // inFlight 必须先增加再二次检查生命周期，避免 shutdown 看到 0 后提前进入 TERMINATED。
        if (state.get() != VirtualExecutorState.RUNNING) {
            cleanupRejectedReservation(control, pendingCounted);
            rejectShutdown();
        }

        submitted.increment();
        Thread thread;
        try {
            boolean pendingAtSubmission = pendingCounted;
            thread = threadFactory.newThread(() -> runAccepted(command, control, pendingAtSubmission));
            if (thread == null) {
                throw new IllegalStateException("Virtual thread factory returned null. group=" + group);
            }
            control.attachRunner(thread);
            thread.start();
        } catch (Throwable throwable) {
            if (control.markFailed()) {
                failed.increment();
            }
            cleanupBeforeStart(control, pendingCounted, true);
            throw throwable;
        }
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new ManagedFutureTask<>(callable);
    }

    @Override
    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new ManagedFutureTask<>(runnable, value);
    }

    private void runAccepted(Runnable command, TaskControl control, boolean pendingAtSubmission) {
        boolean pendingCounted = pendingAtSubmission;
        boolean runningCounted = false;
        try {
            if (control.state() == VirtualTaskState.CANCELLED || state.get() == VirtualExecutorState.STOP) {
                control.cancel(true);
                cancelManagedCommand(command, true);
                return;
            }

            if (!control.permitLedger().ownsExecution()) {
                try {
                    concurrency.acquireExecutionInterruptibly(control.permitLedger());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    control.cancel(true);
                    cancelManagedCommand(command, true);
                    return;
                }
                if (pendingCounted) {
                    pending.decrementAndGet();
                    pendingCounted = false;
                }
            }

            if (control.state() == VirtualTaskState.CANCELLED || state.get() == VirtualExecutorState.STOP) {
                control.cancel(true);
                cancelManagedCommand(command, true);
                return;
            }
            if (!control.markRunning()) {
                return;
            }
            running.incrementAndGet();
            runningCounted = true;

            try {
                command.run();
            } catch (Throwable throwable) {
                if (control.markFailed()) {
                    failed.increment();
                }
                handleDirectExecuteFailure(command, control, throwable);
                throwUnchecked(throwable);
                return;
            }

            TaskOutcome outcome = command instanceof TaskOutcome candidate ? candidate : null;
            if (control.state() == VirtualTaskState.CANCELLED || (outcome != null && outcome.cancelled())) {
                control.cancel(false);
            } else if (outcome != null && outcome.failure() != null) {
                if (control.markFailed()) {
                    failed.increment();
                }
            } else if (control.markCompleted()) {
                completed.increment();
            }
        } finally {
            if (runningCounted) {
                running.decrementAndGet();
            }
            if (pendingCounted) {
                pending.decrementAndGet();
            }
            if (control.state() == VirtualTaskState.CANCELLED) {
                cancelled.increment();
            }

            /*
             * Execution Permit 只能在实际 runner 退出时归还。Future.cancel() 即使已经让 Future 进入
             * CANCELLED，也不能提前释放真实执行容量，否则不响应 interrupt 的底层阻塞调用会让
             * 实际业务并发突破 maxConcurrency。
             */
            control.permitLedger().releaseExecution();
            control.permitLedger().releaseAdmission();
            registry.remove(control);
            inFlight.decrementAndGet();
            tryTerminateIfIdle();
        }
    }

    /**
     * 当执行器在业务任务真正开始前主动终止任务时，同步结束对应 Future，避免留下永久未完成的句柄。
     *
     * <p>该方法只改变 Future 终态或发出中断请求，不释放任何 Permit；真实并发容量仍由 runner
     * 的 finally 清理路径统一归还。</p>
     */
    private static void cancelManagedCommand(Runnable command, boolean mayInterruptIfRunning) {
        if (command instanceof ManagedFutureTask<?> futureTask) {
            futureTask.cancel(mayInterruptIfRunning);
        } else if (command instanceof ManagedCompletableTask<?> completableTask) {
            completableTask.cancel(mayInterruptIfRunning);
        }
    }

    private void handleDirectExecuteFailure(Runnable command, TaskControl control, Throwable throwable) {
        if (command instanceof TaskOutcome) {
            return;
        }
        try {
            Thread runner = control.runner();
            exceptionHandler.handle(new VirtualTaskExceptionContext(
                    group,
                    control.taskId(),
                    runner == null ? null : runner.getName()), throwable);
        } catch (Throwable handlerFailure) {
            throwable.addSuppressed(handlerFailure);
        }
    }

    private void cleanupRejectedReservation(TaskControl control, boolean pendingCounted) {
        if (pendingCounted) {
            pending.decrementAndGet();
        }
        control.permitLedger().releaseExecution();
        control.permitLedger().releaseAdmission();
        registry.remove(control);
        inFlight.decrementAndGet();
        tryTerminateIfIdle();
    }

    private void cleanupBeforeStart(TaskControl control, boolean pendingCounted, boolean acceptedSubmission) {
        if (pendingCounted) {
            pending.decrementAndGet();
        }
        control.permitLedger().releaseExecution();
        control.permitLedger().releaseAdmission();
        registry.remove(control);
        if (acceptedSubmission) {
            inFlight.decrementAndGet();
        }
        tryTerminateIfIdle();
    }

    private void ensureAccepting() {
        if (state.get() != VirtualExecutorState.RUNNING) {
            rejectShutdown();
        }
    }

    private void rejectShutdown() {
        rejected.increment();
        throw new VirtualTaskRejectedException(group, VirtualTaskRejectReason.EXECUTOR_SHUTDOWN,
                "Virtual thread executor is not accepting new tasks. group=" + group);
    }

    /**
     * 发起优雅关闭。
     *
     * <p>调用后拒绝新任务，但已准入的 Running/Pending 任务继续执行，直到所有活动任务退出后
     * 执行器进入 TERMINATED。</p>
     */
    @Override
    public void shutdown() {
        if (state.compareAndSet(VirtualExecutorState.RUNNING, VirtualExecutorState.SHUTDOWN)) {
            log.info("Virtual thread executor shutting down. group={}, running={}, pending={}, inFlight={}",
                    group, running.get(), pending.get(), inFlight.get());
            tryTerminateIfIdle();
        }
    }

    /**
     * 发起立即关闭并请求中断所有活动虚拟线程。
     *
     * <p>Pending 的受管 Future 会被同步到取消终态，避免调用方永久等待；Running 任务收到中断后
     * 是否立即退出仍取决于业务代码和底层客户端是否响应 interrupt。Permit 不会因调用此方法提前释放。</p>
     *
     * @return 空列表；本实现不存在传统 BlockingQueue 中尚未绑定执行线程的 Runnable
     */
    @Override
    public List<Runnable> shutdownNow() {
        boolean transitionedToStop = false;
        while (true) {
            VirtualExecutorState current = state.get();
            if (current == VirtualExecutorState.TERMINATED || current == VirtualExecutorState.STOP) {
                break;
            }
            if (state.compareAndSet(current, VirtualExecutorState.STOP)) {
                transitionedToStop = true;
                break;
            }
        }
        if (transitionedToStop) {
            log.info("Virtual thread executor stopping immediately. group={}, running={}, pending={}, inFlight={}",
                    group, running.get(), pending.get(), inFlight.get());
            for (TaskControl control : registry.controls()) {
                control.cancel(true);
            }
        }
        tryTerminateIfIdle();
        // 已准入任务都有独立虚拟线程承载，不存在传统 BlockingQueue 中可直接返回给调用方的未启动 Runnable。
        return List.of();
    }

    /**
     * 判断执行器是否已经开始关闭。
     *
     * @return 非 RUNNING 状态返回 true
     */
    @Override
    public boolean isShutdown() {
        return state.get() != VirtualExecutorState.RUNNING;
    }

    /**
     * 判断执行器是否已经完全终止。
     *
     * @return TERMINATED 状态返回 true
     */
    @Override
    public boolean isTerminated() {
        return state.get() == VirtualExecutorState.TERMINATED;
    }

    /**
     * 在指定时间内等待执行器进入 TERMINATED。
     *
     * @param timeout 最大等待时间
     * @param unit 时间单位
     * @return true 表示在超时前已经终止
     * @throws InterruptedException 等待线程被中断时抛出
     */
    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long nanos = unit.toNanos(timeout);
        terminationLock.lockInterruptibly();
        try {
            while (state.get() != VirtualExecutorState.TERMINATED) {
                if (nanos <= 0L) {
                    return false;
                }
                nanos = terminatedCondition.awaitNanos(nanos);
            }
            return true;
        } finally {
            terminationLock.unlock();
        }
    }

    private void tryTerminateIfIdle() {
        VirtualExecutorState current = state.get();
        if ((current != VirtualExecutorState.SHUTDOWN && current != VirtualExecutorState.STOP)
                || inFlight.get() != 0) {
            return;
        }
        if (!state.compareAndSet(current, VirtualExecutorState.TERMINATED)) {
            return;
        }
        terminationLock.lock();
        try {
            terminatedCondition.signalAll();
        } finally {
            terminationLock.unlock();
        }
        log.info("Virtual thread executor terminated. group={}, submitted={}, completed={}, failed={}, cancelled={}, rejected={}",
                group, submitted.sum(), completed.sum(), failed.sum(), cancelled.sum(), rejected.sum());
    }

    int runningCount() {
        return running.get();
    }

    int pendingCount() {
        return pending.get();
    }

    int inFlightCount() {
        return inFlight.get();
    }

    long submittedCount() {
        return submitted.sum();
    }

    long completedCount() {
        return completed.sum();
    }

    long failedCount() {
        return failed.sum();
    }

    long cancelledCount() {
        return cancelled.sum();
    }

    long rejectedCount() {
        return rejected.sum();
    }

    private static void throwUnchecked(Throwable throwable) {
        if (throwable instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (throwable instanceof Error error) {
            throw error;
        }
        throw new IllegalStateException("Runnable threw a checked exception", throwable);
    }
}
