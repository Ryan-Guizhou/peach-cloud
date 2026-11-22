package com.peach.scheduled.rest.internal;

import com.peach.scheduled.qo.SchedulerHandlerQO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduler.service.ISchedulerHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调度 Handler 注册管理。
 * <p>用于管理端查看业务服务上报的 Handler 能力和在线状态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Slf4j
@Indexed
@RestController
@RequestMapping("/scheduler/handlers")
@Tag(name = "调度 Handler 注册管理", description = "调度 Handler 注册管理")
@RequiredArgsConstructor
public class SchedulerHandlerController {

    private final ISchedulerHandlerService schedulerHandlerService;

    @GetMapping
    @Operation(summary = "查询调度 Handler 列表")
    public Response list(SchedulerHandlerQO query) {
        return Response.success(schedulerHandlerService.list(query));
    }
}
