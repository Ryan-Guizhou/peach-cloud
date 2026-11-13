package com.peach.transaction.api;

import org.springframework.util.StringUtils;

/**
 * 单次全局事务参数，超时单位毫秒；不会自动传播到异步线程。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public record TransactionOptions(String name, TransactionMode mode, int timeoutMillis) {

    public TransactionOptions {
        if (!StringUtils.hasText(name) || name.length() > 128) {
            throw new IllegalArgumentException("Transaction name must contain 1-128 nonblank characters");
        }
        if (mode == null) {
            throw new IllegalArgumentException("Transaction mode is required");
        }
        if (timeoutMillis <= 0 || timeoutMillis > 600_000) {
            throw new IllegalArgumentException("Transaction timeout must be within 1-600000 ms");
        }
    }
}
