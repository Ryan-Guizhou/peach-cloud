package com.peach.openfeign.support;

import com.peach.openfeign.exception.PeachFeignRetryExhaustedException;
import feign.RetryableException;
import feign.Retryer;

/**
 * Peach OpenFeign 有界重试器。
 *
 * <p>基于 {@link PeachOpenfeignRetryPolicy} 控制最大次数和退避间隔。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachOpenfeignRetryer implements Retryer {

    private final PeachOpenfeignRetryPolicy retryPolicy;

    private int attempt = 1;

    private long intervalMillis;

    public PeachOpenfeignRetryer(PeachOpenfeignRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        this.intervalMillis = retryPolicy.getInitialIntervalMillis();
    }

    @Override
    public void continueOrPropagate(RetryableException exception) {
        if (!retryPolicy.canRetryException(exception) || attempt >= retryPolicy.getMaxAttempts()) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            throw new PeachFeignRetryExhaustedException(null, null, "Feign retry exhausted", exception);
        }
        attempt++;
        if (intervalMillis > 0L) {
            try {
                Thread.sleep(intervalMillis);
            } catch (InterruptedException interruptedException) {
                Thread.currentThread().interrupt();
                throw exception;
            }
        }
        intervalMillis = Math.min(retryPolicy.getMaxIntervalMillis(),
                Math.round(intervalMillis * retryPolicy.getMultiplier()));
    }

    @Override
    public Retryer clone() {
        return new PeachOpenfeignRetryer(retryPolicy);
    }
}
