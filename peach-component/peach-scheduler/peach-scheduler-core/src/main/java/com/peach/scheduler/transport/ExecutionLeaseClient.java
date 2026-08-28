package com.peach.scheduler.transport;

/**
 * 执行Lease客户端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ExecutionLeaseClient {

    /**
     * 调度模块说明。
     *
     * @param executionId execution Id。
     * @param executorInstance executor Instance。
     * @return 执行结果。
     */
    boolean claim(String executionId, String executorInstance);
}
