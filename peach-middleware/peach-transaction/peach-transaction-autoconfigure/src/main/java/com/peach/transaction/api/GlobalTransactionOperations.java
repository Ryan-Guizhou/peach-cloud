package com.peach.transaction.api;

import java.util.concurrent.Callable;

/**
 * 业务侧编程式事务入口；默认业务也可直接使用 Seata @GlobalTransactional。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public interface GlobalTransactionOperations {
    <T> T execute(TransactionOptions options, Callable<T> action) throws Exception;
}
