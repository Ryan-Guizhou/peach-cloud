package com.peach.sample.email.retry;

import com.peach.email.retry.RetryPolicy;
import lombok.extern.slf4j.Slf4j;

import jakarta.mail.MessagingException;
import jakarta.mail.SendFailedException;

/**
 * Fixed延迟重试策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@Slf4j
public class FixedDelayRetryPolicy implements RetryPolicy {

    private final int maxAttempts;      // 最大重试次数

    private final long delayMillis;     // 固定间隔时间（毫秒）

    private static final int DEFAULT_MAX_ATTEMPTS = 3;
    private static final long DEFAULT_DELAY_MILLIS = 1000;


    public FixedDelayRetryPolicy() {
        this(DEFAULT_MAX_ATTEMPTS, DEFAULT_DELAY_MILLIS);
    }

    public FixedDelayRetryPolicy(int maxAttempts, long delayMillis) {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be >= 1");
        }
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delayMillis must be >= 0");
        }
        this.maxAttempts = maxAttempts;
        this.delayMillis = delayMillis;
        log.info("Init FixedDelayRetryPolicy: maxAttempts={}, delayMillis={}", maxAttempts, delayMillis);
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    @Override
    public long computeDelayMillis(int attempt) {
        // 固定延迟，不随重试次数增加
        return delayMillis;
    }

    @Override
    public boolean isRetryable(Throwable error) {
        return !(error instanceof SendFailedException);
    }
}
