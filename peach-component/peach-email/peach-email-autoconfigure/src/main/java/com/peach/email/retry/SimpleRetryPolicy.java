package com.peach.email.retry;

import jakarta.mail.SendFailedException;
import lombok.extern.slf4j.Slf4j;

/**
 * 简单重试策略。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:10
 */
@Slf4j
public class SimpleRetryPolicy implements RetryPolicy{

    private static final long MAX_RETRY_DELAY_MILLIS = 300_000L;

    private final int maxAttempts;

    private final long delayMillis;

    public SimpleRetryPolicy(int maxAttempts, long delayMillis) {
        if (maxAttempts < 1 || delayMillis < 0L) {
            throw new IllegalArgumentException("SMTP retry attempts must be positive and delay nonnegative");
        }
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
        if (attempt < 1) {
            throw new IllegalArgumentException("SMTP retry attempt must be positive");
        }
        long delay = Math.min(delayMillis, MAX_RETRY_DELAY_MILLIS);
        for (int i = 1; i < attempt && delay < MAX_RETRY_DELAY_MILLIS; i++) {
            delay = Math.min(MAX_RETRY_DELAY_MILLIS,
                    delay > MAX_RETRY_DELAY_MILLIS / 2 ? MAX_RETRY_DELAY_MILLIS : delay * 2);
        }
        return delay;
    }

    @Override
    public boolean isRetryable(Throwable error) {
        return !(error instanceof SendFailedException);
    }
}
