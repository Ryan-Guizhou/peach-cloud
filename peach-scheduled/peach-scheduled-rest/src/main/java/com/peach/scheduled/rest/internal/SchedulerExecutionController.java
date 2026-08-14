package com.peach.scheduled.rest.internal;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.scheduled.qo.SchedulerExecutionQO;
import com.peach.scheduler.service.ISchedulerExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 调度执行实例管理接口。
 *
 * <p>提供执行历史查询、详情查询、人工重试和人工取消能力。
 * 人工操作必须携带原因并由服务层写入操作审计；RUNNING 状态不提供伪取消。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@RestController
@RequestMapping("/scheduler/executions")
@Tag(name = "调度执行管理", description = "调度执行管理")
@Indexed
public class SchedulerExecutionController {

    private final ISchedulerExecutionService schedulerExecutionService;

    /**
     * 创建调度执行实例管理接口。
     *
     * @param schedulerExecutionService 调度执行实例服务
     */
    public SchedulerExecutionController(ISchedulerExecutionService schedulerExecutionService) {
        this.schedulerExecutionService = schedulerExecutionService;
    }

    /**
     * 查询调度执行实例列表。
     *
     * @param query 查询条件
     * @return 执行实例列表响应
     */
    @GetMapping
    @Operation(summary = "查询调度执行列表")
    public Response list(SchedulerExecutionQO query) {
        return Response.success(schedulerExecutionService.list(query));
    }

    /**
     * 查询指定执行实例详情。
     *
     * @param id 执行实例标识
     * @return 执行实例详情响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询调度执行详情")
    public Response get(@PathVariable String id) {
        return Response.success(schedulerExecutionService.get(id));
    }

    /**
     * 人工重新投递等待重试的执行实例。
     *
     * @param id 执行实例标识
     * @param reason 人工重试原因
     * @return 重试受理结果
     */
    @PostMapping("/{id}/retry")
    @Operation(summary = "人工重试调度执行")
    public Response retry(@PathVariable String id, @RequestParam String reason) {
        return Response.success(schedulerExecutionService.retry(id, currentUserId(), reason));
    }

    /**
     * 人工取消尚未开始业务执行的执行实例。
     *
     * @param id 执行实例标识
     * @param reason 人工取消原因
     * @return 取消后的执行实例
     */
    @PostMapping("/{id}/cancel")
    @Operation(summary = "人工取消调度执行")
    public Response cancel(@PathVariable String id, @RequestParam String reason) {
        return Response.success(schedulerExecutionService.cancel(id, currentUserId(), reason));
    }

    private String currentUserId() {
        UserContext context = SecurityContextHolder.get();
        String userId = context == null ? null : context.getUserId();
        if (StringUtils.isBlank(userId)) {
            throw new IllegalStateException("Authenticated scheduler operator is required");
        }
        return userId;
    }
}
