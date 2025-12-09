package com.peach.email.retry;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:07
 * @Description: 重试策略接口
 */
public interface RetryPolicy {

    /**
     * 获取最大尝试次数(含首次发送)
     * @return
     */
    int getMaxAttempts();

    /**
     * 按尝试次数计算延迟（毫秒）
     * @param attempt 尝试次数（从0开始）
     * @return 延迟毫秒数
     */
    long computeDelayMillis(int attempt);

    /**
     * 错误是否可重试
     */
    boolean isRetryable(Throwable e);
}
