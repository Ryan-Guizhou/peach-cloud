package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduled.dto.ExecutionClaimDTO;
import com.peach.scheduler.transport.ExecutionLeaseClient;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class FeignExecutionLeaseClient implements ExecutionLeaseClient {
    private final SchedulerExecutionExternalClient client;

    /**
     * 创建相关对象。
     *
     * @param client 参数说明
     */
    public FeignExecutionLeaseClient(SchedulerExecutionExternalClient client) {
        this.client = client;
    }

    /**
     * 继承接口定义。
     */
    @Override
    public boolean claim(String executionId, String executorInstance) {
        ExecutionClaimDTO request = new ExecutionClaimDTO();
        request.setExecutorInstance(executorInstance);
        Response response = client.claim(executionId, request);
        if (response == null || !response.isSuccess()) return false;
        Object data = response.getData();
        return Boolean.TRUE.equals(data) || "true".equalsIgnoreCase(String.valueOf(data));
    }
}
