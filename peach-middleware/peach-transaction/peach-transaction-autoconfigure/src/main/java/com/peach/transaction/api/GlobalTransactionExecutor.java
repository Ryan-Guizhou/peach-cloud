package com.peach.transaction.api;

import java.util.concurrent.Callable;

/**
 * 可替换事务引擎 SPI。实现必须保证异常不被吞掉、不能将全局事务降级为本地事务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public interface GlobalTransactionExecutor {

    String provider();

    boolean supports(TransactionMode mode);

    <T> T execute(TransactionOptions options, Callable<T> action) throws Exception;
}
