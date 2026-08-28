package com.peach.openfeign.support;

import com.peach.openfeign.config.PeachOpenfeignProperties;
import feign.RetryableException;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * PeachOpenFeign重试策略。
 * <p>策略只对配置允许的 HTTP 方法、状态码和异常类型生效，避免对写操作或
 * 非瞬时故障做隐式重复提交。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachOpenfeignRetryPolicy {

    private final PeachOpenfeignProperties.RetryOptions retry;

    private final Set<String> methods = new HashSet<String>();

    private final Set<Integer> statuses = new HashSet<Integer>();

    private final Set<String> exceptions = new HashSet<String>();

    public PeachOpenfeignRetryPolicy(PeachOpenfeignProperties properties) {
        this.retry = properties.getRetry();
        if (retry.getMethods() != null) {
            for (String method : retry.getMethods()) {
                if (method != null) {
                    methods.add(method.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        if (retry.getStatuses() != null) {
            statuses.addAll(retry.getStatuses());
        }
        if (retry.getExceptions() == null) {
            return;
        }
        for (String exception : retry.getExceptions()) {
            if (exception != null && !exception.isBlank()) {
                exceptions.add(exception.trim());
            }
        }
    }

    public boolean isEnabled() {
        return retry.isEnabled();
    }

    public int getMaxAttempts() {
        return Math.max(1, retry.getMaxAttempts());
    }

    public long getInitialIntervalMillis() {
        return Math.max(0L, retry.getInitialIntervalMillis());
    }

    public long getMaxIntervalMillis() {
        return Math.max(getInitialIntervalMillis(), retry.getMaxIntervalMillis());
    }

    public double getMultiplier() {
        return retry.getMultiplier() <= 0d ? 1.0d : retry.getMultiplier();
    }

    public Set<String> getMethods() {
        return methods;
    }

    public Set<Integer> getStatuses() {
        return statuses;
    }

    public boolean canRetryStatus(String method, int status) {
        return isEnabled()
                && methods.contains(normalizeMethod(method))
                && statuses.contains(status);
    }

    public boolean canRetryException(Throwable throwable) {
        if (!isEnabled() || throwable == null) {
            return false;
        }
        if (throwable instanceof RetryableException) {
            return true;
        }
        Class<?> current = throwable.getClass();
        while (current != null) {
            if (exceptions.contains(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    private String normalizeMethod(String method) {
        return method == null ? "" : method.trim().toUpperCase(Locale.ROOT);
    }
}
