package com.peach.code.autoconfigure;

/**
 * 在持有数据库规则行锁期间协调序号的内部契约。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/23 10:45
 */
interface CodeSequenceCoordinator {

    /** 返回大于持久水位的候选值；调用者必须持久化提交后才能向业务暴露。 */
    long next(String tenantId, String codeType, long durable, long maximum);
}
