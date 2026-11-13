package com.peach.transaction.support;

import com.peach.transaction.api.GlobalTransactionExecutor;
import com.peach.transaction.api.GlobalTransactionOperations;
import com.peach.transaction.api.TransactionOptions;
import com.peach.transaction.config.PeachTransactionProperties;

import java.util.List;
import java.util.concurrent.Callable;

/**
 * 按明确的 Provider 名称选择全局事务引擎，不支持静默回退。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public class DefaultGlobalTransactionOperations implements GlobalTransactionOperations {

    private final GlobalTransactionExecutor executor;

    public DefaultGlobalTransactionOperations(List<GlobalTransactionExecutor> candidates,
                                              PeachTransactionProperties properties) {
        List<GlobalTransactionExecutor> matched = candidates.stream()
                .filter(candidate -> properties.getProvider().equals(candidate.provider()))
                .toList();
        if (matched.size() != 1) {
            throw new IllegalStateException("Expected exactly one transaction provider '"
                    + properties.getProvider() + "', found " + matched.size());
        }
        if (!matched.getFirst().supports(properties.getMode())) {
            throw new IllegalStateException("Transaction provider " + properties.getProvider()
                    + " does not support " + properties.getMode());
        }
        this.executor = matched.getFirst();
    }

    @Override
    public <T> T execute(TransactionOptions options, Callable<T> action) throws Exception {
        if (options == null || action == null) {
            throw new IllegalArgumentException("Transaction options and action are required");
        }
        if (!executor.supports(options.mode())) {
            throw new IllegalArgumentException("Unsupported transaction mode: " + options.mode());
        }
        return executor.execute(options, action);
    }
}
