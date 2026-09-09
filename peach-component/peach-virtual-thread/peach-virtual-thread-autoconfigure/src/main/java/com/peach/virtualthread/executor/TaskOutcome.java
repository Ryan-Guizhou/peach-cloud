package com.peach.virtualthread.executor;

/**
 * 执行器内部任务结果探针。
 *
 * <p>FutureTask 和 Starter 便捷 CompletableFuture 任务通过该接口向执行器暴露失败/取消结果，
 * 避免为了观测状态改变 JDK Future 的异常传播语义。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 15:05
 */
interface TaskOutcome {

    /**
     * 获取任务执行阶段捕获到的原始异常。
     *
     * @return 原始异常；任务未失败时返回 null
     */
    Throwable failure();

    /**
     * 判断对应 Future 是否已经取消。
     *
     * @return true 表示任务 Future 已取消
     */
    boolean cancelled();
}
