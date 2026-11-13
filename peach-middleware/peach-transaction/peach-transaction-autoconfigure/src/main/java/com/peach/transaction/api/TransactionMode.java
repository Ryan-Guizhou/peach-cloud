package com.peach.transaction.api;

/**
 * 全局事务模式。首批真实支持 AT，其他模式必须显式拒绝，不能静默降级。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
public enum TransactionMode {
    AT,
    TCC,
    SAGA,
    XA
}
