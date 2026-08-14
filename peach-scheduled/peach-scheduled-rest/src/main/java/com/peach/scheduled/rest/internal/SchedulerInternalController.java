package com.peach.scheduled.rest.internal;

import org.springframework.stereotype.Indexed;

import cn.dev33.satoken.same.SaSameUtil;
import com.peach.common.response.Response;
import com.peach.scheduled.dto.ExecutionClaimDTO;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduler.service.ISchedulerExecutionService;
import com.peach.scheduler.service.ISchedulerHandlerService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * Scheduler 服务间内部接口。
 *
 * <p>用于业务执行器原子抢占 execution 租约以及上报 Handler 能力。
 * 这些接口不依赖用户登录态，但每次调用都会显式校验 Peach Same-Token，禁止匿名调用。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@RestController
@RequestMapping("/internal/scheduler")
@Indexed
public class SchedulerInternalController {

    private final ISchedulerExecutionService executionService;
    private final ISchedulerHandlerService handlerService;

    /**
     * 创建 Scheduler 内部接口。
     *
     * @param executionService 调度执行实例服务
     * @param handlerService Handler 注册服务
     */
    public SchedulerInternalController(ISchedulerExecutionService executionService,
                                       ISchedulerHandlerService handlerService) {
        this.executionService = executionService;
        this.handlerService = handlerService;
    }

    /**
     * 原子抢占指定执行实例的执行租约。
     *
     * @param id 执行实例标识
     * @param data 租约抢占请求
     * @return 抢占结果
     */
    @PostMapping("/executions/{id}/claim")
    public Response claim(@PathVariable String id, @Valid @RequestBody ExecutionClaimDTO data) {
        requireServiceIdentity();
        return Response.success(executionService.claim(id, data.getExecutorInstance()));
    }

    /**
     * 注册或刷新业务服务实例的 Handler 能力。
     *
     * @param data Handler 注册请求
     * @return 注册结果
     */
    @PostMapping("/handlers/register")
    public Response register(@Valid @RequestBody HandlerRegistrationDTO data) {
        requireServiceIdentity();
        handlerService.register(data);
        return Response.success();
    }

    /**
     * 校验当前内部请求的服务间 Same-Token。
     */
    private void requireServiceIdentity() {
        SaSameUtil.checkCurrentRequestToken();
    }
}
