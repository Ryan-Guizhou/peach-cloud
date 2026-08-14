package com.peach.scheduled.rest.internal;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduler.service.ISchedulerHandlerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调度 Handler 注册信息管理接口。
 *
 * <p>用于管理端查看业务服务上报的 Handler 能力和在线状态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@RestController
@RequestMapping("/scheduler/handlers")
@Tag(name = "调度 Handler 注册管理", description = "调度 Handler 注册管理")
@Indexed
public class SchedulerHandlerController {

    private final ISchedulerHandlerService schedulerHandlerService;

    /**
     * 创建调度 Handler 管理接口。
     *
     * @param schedulerHandlerService Handler 注册服务
     */
    public SchedulerHandlerController(ISchedulerHandlerService schedulerHandlerService) {
        this.schedulerHandlerService = schedulerHandlerService;
    }

    /**
     * 查询当前在线的 Handler 列表。
     *
     * @return Handler 列表响应
     */
    @GetMapping
    @Operation(summary = "查询调度 Handler 列表")
    public Response list() {
        return Response.success(schedulerHandlerService.list());
    }
}
