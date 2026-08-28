package com.peach.openfeign.support;

import com.peach.openfeign.exception.PeachFeignRetryExhaustedException;
import feign.RetryableException;
import feign.Retryer;

/**
 * PeachOpenFeign重试器。
 * <p>基于 {@link PeachOpenfeignRetryPolicy} 控制最大次数和退避间隔。
 * Feign {@link Retryer} 接口要求实现 {@code clone()} 以隔离每次请求的重试状态；本类通过
 * 手动复制字段满足契约（Sonar S2975/S1182 已在根 {@code pom.xml} 多条件忽略中按文件配置）。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12
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
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
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

    /** 复制当前重试进度，供 Feign 为新请求创建独立 {@link Retryer} 实例。 */
    @Override
    public Retryer clone() {
        PeachOpenfeignRetryer copy = new PeachOpenfeignRetryer(retryPolicy);
        copy.attempt = this.attempt;
        copy.intervalMillis = this.intervalMillis;
        return copy;
    }
}
