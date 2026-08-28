package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduled.dto.ExecutionClaimDTO;
import com.peach.scheduler.transport.ExecutionLeaseClient;

/**
 * Feign执行Lease客户端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class FeignExecutionLeaseClient implements ExecutionLeaseClient {
    private final SchedulerExecutionExternalClient client;

    /**
     * 创建实例。
     *
     * @param client client。
     */
    public FeignExecutionLeaseClient(SchedulerExecutionExternalClient client) {
        this.client = client;
    }

    /**
     * 接口实现。
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
