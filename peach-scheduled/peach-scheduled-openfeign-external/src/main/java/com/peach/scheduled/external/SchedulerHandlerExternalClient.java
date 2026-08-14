package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调度模块相关说明。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@FeignClient(
        name = "peach-scheduler",
        contextId = "schedulerHandlerExternalClient",
        fallbackFactory = SchedulerHandlerExternalClientFallbackFactory.class
)
@Indexed
public interface SchedulerHandlerExternalClient {
    /**
     * 注册相关能力。
     *
     * @param request 参数说明
     * @return 返回结果
     */
    @PostMapping("/internal/scheduler/handlers/register")
    Response register(@RequestBody HandlerRegistrationDTO request);
}
