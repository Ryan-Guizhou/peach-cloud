package com.peach.transaction.seata;

import com.peach.transaction.api.TransactionMode;
import com.peach.transaction.api.TransactionOptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Provider 模式边界测试，不依赖真实 TC。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
class SeataGlobalTransactionExecutorTest {

    @Test
    void onlyAtIsSupported() {
        SeataGlobalTransactionExecutor executor = new SeataGlobalTransactionExecutor();
        assertTrue(executor.supports(TransactionMode.AT));
        assertFalse(executor.supports(TransactionMode.TCC));
        assertFalse(executor.supports(TransactionMode.SAGA));
        assertFalse(executor.supports(TransactionMode.XA));
    }

    @Test
    void unsupportedModeIsRejectedBeforeOpeningTransaction() {
        SeataGlobalTransactionExecutor executor = new SeataGlobalTransactionExecutor();
        TransactionOptions options = new TransactionOptions("saga", TransactionMode.SAGA, 1000);
        assertThrows(IllegalArgumentException.class, () -> executor.execute(options, () -> null));
    }
}
