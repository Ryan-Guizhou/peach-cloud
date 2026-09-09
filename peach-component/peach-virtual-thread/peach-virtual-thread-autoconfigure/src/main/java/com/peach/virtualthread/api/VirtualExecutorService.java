package com.peach.virtualthread.api;

import com.peach.virtualthread.state.VirtualGroupSnapshot;
import com.peach.virtualthread.task.VirtualTaskSnapshot;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Supplier;

/**
 * Peach 虚拟线程分组执行器标准接口。
 *
 * <p>接口继承 {@link ExecutorService}，业务可以直接使用 execute、submit、invokeAll、invokeAny
 * 以及 {@link CompletableFuture} 的标准 Executor 参数。额外方法仅提供组标识、便捷
 * CompletableFuture 提交和只读状态查询。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public interface VirtualExecutorService extends ExecutorService {

    /**
     * 获取业务组名称。
     *
     * @return 业务组名称
     */
    String groupName();

    /**
     * 提交有返回值任务。
     *
     * <p>该方法与 {@link ExecutorService#submit(Callable)} 语义一致，任务会经过当前业务组的
     * 并发上限、Pending 上限和背压控制，异常通过返回的 {@link Future} 保留并向调用方传播。</p>
     *
     * @param task 待执行任务
     * @param <T> 返回值类型
     * @return 受当前业务组管理的 Future
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException task 为 null 时抛出
     */
    @Override
    <T> Future<T> submit(Callable<T> task);

    /**
     * 提交无返回值任务。
     *
     * @param task 待执行任务
     * @return 受当前业务组管理的 Future
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException task 为 null 时抛出
     */
    @Override
    Future<?> submit(Runnable task);

    /**
     * 提交无返回值任务并指定 Future 成功完成时的返回值。
     *
     * @param task 待执行任务
     * @param result 任务成功完成后由 Future 返回的结果
     * @param <T> 返回值类型
     * @return 受当前业务组管理的 Future
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException task 为 null 时抛出
     */
    @Override
    <T> Future<T> submit(Runnable task, T result);

    /**
     * 使用当前业务组异步执行 Supplier。
     *
     * <p>返回值是 Starter 受管的 {@link CompletableFuture}。调用 {@code cancel(true)} 时会把
     * 取消请求同步给实际虚拟线程，但 Permit 仍只在 runner 真正退出后归还。</p>
     *
     * @param supplier 待执行 Supplier
     * @param <T> 返回值类型
     * @return 与当前业务组任务生命周期关联的 CompletableFuture
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException supplier 为 null 时抛出
     */
    <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier);

    /**
     * 使用当前业务组异步执行 Runnable。
     *
     * <p>取消、拒绝和 Permit 生命周期与 {@link #supplyAsync(Supplier)} 一致。</p>
     *
     * @param runnable 待执行 Runnable
     * @return 与当前业务组任务生命周期关联的 CompletableFuture
     * @throws java.util.concurrent.RejectedExecutionException 执行器已关闭或业务组容量不足时抛出
     * @throws NullPointerException runnable 为 null 时抛出
     */
    CompletableFuture<Void> runAsync(Runnable runnable);

    /**
     * 获取业务组运行快照。
     *
     * @return 当前只读快照
     */
    VirtualGroupSnapshot snapshot();

    /**
     * 获取当前活动任务安全快照。
     *
     * @return 活动任务快照列表
     */
    List<VirtualTaskSnapshot> activeTasks();
}
