package com.peach.transaction.seata;

import com.peach.transaction.api.GlobalTransactionExecutor;
import com.peach.transaction.api.TransactionMode;
import com.peach.transaction.api.TransactionOptions;
import io.seata.core.context.RootContext;
import io.seata.tm.api.GlobalTransaction;
import io.seata.tm.api.GlobalTransactionContext;

import java.util.concurrent.Callable;

/**
 * Seata AT 全局事务 SPI。仅在当前线程无 XID 时创建全局事务；
 * 已有 XID 时加入当前事务，不擅自提交上游事务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public class SeataGlobalTransactionExecutor implements GlobalTransactionExecutor {

    @Override
    public String provider() {
        return "seata";
    }

    @Override
    public boolean supports(TransactionMode mode) {
        return mode == TransactionMode.AT;
    }

    @Override
    public <T> T execute(TransactionOptions options, Callable<T> action) throws Exception {
        if (options == null || !supports(options.mode()) || action == null) {
            throw new IllegalArgumentException("Only Seata AT is supported with valid options/action");
        }
        if (RootContext.inGlobalTransaction()) {
            return action.call();
        }
        GlobalTransaction transaction = GlobalTransactionContext.getCurrentOrCreate();
        transaction.begin(options.timeoutMillis(), options.name());
        try {
            T result = action.call();
            transaction.commit();
            return result;
        } catch (Exception | Error failure) {
            try {
                transaction.rollback();
            } catch (Exception rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw failure;
        }
    }
}
