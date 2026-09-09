package com.peach.virtualthread.registry;

import com.peach.virtualthread.api.VirtualExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 虚拟线程分组执行器注册中心与 Spring 关闭协调器。
 *
 * <p>注册中心只维护应用启动期已经创建完成的静态业务组执行器，提供按业务组查询、提交任务以及
 * 容器关闭时统一执行 graceful shutdown 的入口。注册中心不会在运行期创建、删除、替换或调整
 * 业务组容量。</p>
 *
 * <p>该类本身不保存业务任务和任务结果。所有 {@code submit/execute} 调用都会直接委托给目标
 * {@link VirtualExecutorService}，因此不会绕过对应业务组的并发、Pending、背压和 Permit 管理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:20
 */
public final class VirtualExecutorRegistry implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(VirtualExecutorRegistry.class);
    private static final Duration DEFAULT_SHUTDOWN_AWAIT = Duration.ofSeconds(30);

    private final Map<String, VirtualExecutorService> executors;
    private final long shutdownAwaitNanos;

    /**
     * 创建执行器注册中心。
     *
     * @param executors 已注册业务组执行器
     * @param shutdownAwait Spring 容器关闭时等待所有执行器自然终止的最长时间；为 null 时使用 30 秒
     * @throws NullPointerException executors 或其中任一执行器为 null 时抛出
     * @throws IllegalArgumentException 业务组重复、关闭等待时间为负数或无法安全转换为纳秒时抛出
     */
    public VirtualExecutorRegistry(List<VirtualExecutorService> executors, Duration shutdownAwait) {
        Objects.requireNonNull(executors, "executors");
        Map<String, VirtualExecutorService> byGroup = new LinkedHashMap<>();
        for (VirtualExecutorService executor : executors) {
            VirtualExecutorService value = Objects.requireNonNull(executor, "executor");
            String group = requireGroupName(value.groupName());
            VirtualExecutorService previous = byGroup.putIfAbsent(group, value);
            if (previous != null) {
                throw new IllegalArgumentException("Duplicate virtual executor group: " + group);
            }
        }
        this.executors = Map.copyOf(byGroup);
        this.shutdownAwaitNanos = toShutdownAwaitNanos(shutdownAwait == null
                ? DEFAULT_SHUTDOWN_AWAIT : shutdownAwait);
    }

    /**
     * 按业务组获取执行器。
     *
     * @param group 业务组名称
     * @return 对应业务组执行器
     * @throws IllegalArgumentException group 为空或业务组不存在时抛出
     */
    public VirtualExecutorService get(String group) {
        String groupName = requireGroupName(group);
        VirtualExecutorService executor = executors.get(groupName);
        if (executor == null) {
            throw new IllegalArgumentException("Unknown virtual thread group: " + groupName);
        }
        return executor;
    }

    /**
     * 按业务组提交有返回值任务。
     *
     * <p>该入口用于兼容传统线程池管理器的调用习惯，实际任务仍由目标业务组执行器管理。</p>
     *
     * @param group 业务组名称
     * @param task 待执行任务
     * @param <T> 返回值类型
     * @return 任务 Future
     */
    public <T> Future<T> submit(String group, Callable<T> task) {
        return get(group).submit(task);
    }

    /**
     * 按业务组提交无返回值任务。
     *
     * @param group 业务组名称
     * @param task 待执行任务
     * @return 任务 Future
     */
    public Future<Void> submit(String group, Runnable task) {
        return get(group).submit(task, null);
    }

    /**
     * 按业务组提交无返回值任务，并指定 Future 成功完成时的返回值。
     *
     * @param group 业务组名称
     * @param task 待执行任务
     * @param result 任务成功完成后由 Future 返回的结果
     * @param <T> 返回值类型
     * @return 任务 Future
     */
    public <T> Future<T> submit(String group, Runnable task, T result) {
        return get(group).submit(task, result);
    }

    /**
     * 按业务组直接执行无返回值任务。
     *
     * <p>该方法没有 Future 承接业务异常，未捕获异常会交由
     * {@code VirtualTaskExceptionHandler} 处理后继续进入线程 uncaught exception 流程。</p>
     *
     * @param group 业务组名称
     * @param task 待执行任务
     */
    public void execute(String group, Runnable task) {
        get(group).execute(task);
    }

    /**
     * 获取全部业务组名称。
     *
     * @return 不可变业务组名称集合
     */
    public Set<String> groupNames() {
        return executors.keySet();
    }

    /**
     * 获取全部受管执行器。
     *
     * @return 不可变执行器列表
     */
    public List<VirtualExecutorService> all() {
        return List.copyOf(executors.values());
    }

    /**
     * Spring 容器销毁时统一关闭所有业务组执行器。
     *
     * <p>先调用 {@link VirtualExecutorService#shutdown()} 允许已准入任务自然完成，再共享一个全局
     * grace period 逐个等待。超过总等待时间仍未终止的执行器会进入 {@code shutdownNow()}。</p>
     *
     * @throws InterruptedException 当前关闭线程在等待期间被中断时抛出
     */
    @Override
    public void destroy() throws InterruptedException {
        if (executors.isEmpty()) {
            return;
        }
        log.info("Shutting down virtual thread executors. groups={}", executors.keySet());
        for (VirtualExecutorService executor : executors.values()) {
            executor.shutdown();
        }

        long startedNanos = System.nanoTime();
        try {
            for (VirtualExecutorService executor : executors.values()) {
                if (executor.isTerminated()) {
                    continue;
                }
                long elapsedNanos = System.nanoTime() - startedNanos;
                long remainingNanos = Math.max(0L, shutdownAwaitNanos - Math.max(0L, elapsedNanos));
                if (!executor.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS)) {
                    log.warn("Virtual thread executor exceeded shutdown grace period. group={}, gracePeriodNanos={}",
                            executor.groupName(), shutdownAwaitNanos);
                    executor.shutdownNow();
                }
            }
        } catch (InterruptedException ex) {
            log.warn("Virtual thread shutdown coordination was interrupted. Forcing remaining executors to stop.");
            for (VirtualExecutorService executor : executors.values()) {
                if (!executor.isTerminated()) {
                    executor.shutdownNow();
                }
            }
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    private static String requireGroupName(String group) {
        if (group == null || group.isBlank()) {
            throw new IllegalArgumentException("Virtual thread group must not be blank");
        }
        return group;
    }

    private static long toShutdownAwaitNanos(Duration duration) {
        if (duration.isNegative()) {
            throw new IllegalArgumentException("shutdownAwait must not be negative");
        }
        try {
            return duration.toNanos();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException("shutdownAwait exceeds supported duration range", ex);
        }
    }
}
