package com.peach.virtualthread.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认虚拟线程未承接异常处理器。
 *
 * <p>仅记录安全标识和原始异常，不记录 Runnable、Callable、业务参数或返回结果。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
public class DefaultVirtualTaskExceptionHandler implements VirtualTaskExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultVirtualTaskExceptionHandler.class);

    /**
     * 记录没有被 Future 承接的 {@code execute(Runnable)} 原始异常。
     *
     * @param context 不包含业务载荷的安全任务上下文
     * @param throwable 原始业务异常
     */
    @Override
    public void handle(VirtualTaskExceptionContext context, Throwable throwable) {
        log.error("Unhandled virtual task exception. group={}, taskId={}, thread={}",
                context.group(), context.taskId(), context.threadName(), throwable);
    }
}
