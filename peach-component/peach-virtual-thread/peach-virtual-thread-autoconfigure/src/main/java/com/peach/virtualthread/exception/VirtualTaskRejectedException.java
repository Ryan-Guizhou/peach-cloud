package com.peach.virtualthread.exception;

import java.util.concurrent.RejectedExecutionException;

/**
 * 虚拟线程任务拒绝异常。
 *
 * <p>继承 {@link RejectedExecutionException}，保证与 JDK {@code ExecutorService} 生态兼容。
 * 异常只携带安全的业务组和枚举原因，不保存 Runnable、Callable 或业务参数。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/9 14:40
 */
public class VirtualTaskRejectedException extends RejectedExecutionException {

    private static final long serialVersionUID = 1L;

    private final String group;
    private final VirtualTaskRejectReason reason;

    /**
     * 创建任务拒绝异常。
     *
     * @param group 业务组名称
     * @param reason 拒绝原因
     * @param message 安全异常信息
     */
    public VirtualTaskRejectedException(String group, VirtualTaskRejectReason reason, String message) {
        super(message);
        this.group = group;
        this.reason = reason;
    }

    /**
     * 创建带原始原因的任务拒绝异常。
     *
     * @param group 业务组名称
     * @param reason 拒绝原因
     * @param message 安全异常信息
     * @param cause 原始异常
     */
    public VirtualTaskRejectedException(String group, VirtualTaskRejectReason reason, String message, Throwable cause) {
        super(message, cause);
        this.group = group;
        this.reason = reason;
    }

    /**
     * 获取发生拒绝的业务组。
     *
     * @return 业务组名称
     */
    public String getGroup() {
        return group;
    }

    /**
     * 获取结构化拒绝原因。
     *
     * @return 拒绝原因
     */
    public VirtualTaskRejectReason getReason() {
        return reason;
    }
}
