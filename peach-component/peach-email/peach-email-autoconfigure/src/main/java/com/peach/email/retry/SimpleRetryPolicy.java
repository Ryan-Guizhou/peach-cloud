package com.peach.email.retry;



import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:10
 * @Description: 简单的重试策略
 */
@Slf4j
public class SimpleRetryPolicy implements RetryPolicy{

    private final int maxAttempts;

    private final long delayMillis;

    public SimpleRetryPolicy(int maxAttempts, long delayMillis) {
        this.maxAttempts = maxAttempts;
        this.delayMillis = delayMillis;
        log.info("Init SimpleRetryPolicy: maxAttempts={}, delayMillis={}", maxAttempts, delayMillis);
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public long computeDelayMillis(int attempt) {
        return delayMillis * (1L << (attempt - 1));
    }

    @Override
    public boolean isRetryable(Throwable error) {
        if (error instanceof SendFailedException) {
            return false; // 地址/内容错误通常不可重试
        }
        if (error instanceof MessagingException){
            return true; // 网络/连接错误可重试
        }
        return true;
    }
}
