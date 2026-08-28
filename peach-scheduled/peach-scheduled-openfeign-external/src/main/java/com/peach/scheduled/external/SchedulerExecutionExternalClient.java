package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduled.dto.ExecutionClaimDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调度执行External客户端。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@FeignClient(
        name = "peach-scheduler",
        contextId = "schedulerExecutionExternalClient",
        fallbackFactory = SchedulerExecutionExternalClientFallbackFactory.class
)
@Indexed
public interface SchedulerExecutionExternalClient {
    /**
     * 调度模块说明。
     *
     * @param executionId execution Id。
     * @param request request。
     * @return 执行结果。
     */
    @PostMapping("/internal/scheduler/executions/{id}/claim")
    Response claim(@PathVariable("id") String executionId, @RequestBody ExecutionClaimDTO request);
}
