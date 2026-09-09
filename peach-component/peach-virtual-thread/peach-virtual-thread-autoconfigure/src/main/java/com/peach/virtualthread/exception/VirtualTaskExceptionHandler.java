package com.peach.virtualthread.exception;

/**
 * {@code execute(Runnable)} 未承接业务异常处理器。
 *
 * <p>{@code submit()} 与 CompletableFuture 的异常由 Future 自身承接，不通过该接口重复处理。
 * 自定义实现不得假定可以改变 Permit 生命周期；资源清理由执行器 finally 统一完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
@FunctionalInterface
public interface VirtualTaskExceptionHandler {

    /**
     * 处理 execute(Runnable) 未承接异常。
     *
     * @param context 安全任务上下文
     * @param throwable 原始业务异常
     */
    void handle(VirtualTaskExceptionContext context, Throwable throwable);
}
