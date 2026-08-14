package com.peach.scheduled.rest.internal;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.satoken.context.SecurityContextHolder;
import com.peach.satoken.context.UserContext;
import com.peach.scheduled.common.JobEvent;
import com.peach.scheduled.dto.SchedulerJobSaveDTO;
import com.peach.scheduled.qo.SchedulerJobQO;
import com.peach.scheduler.service.ISchedulerJobService;
import com.peach.scheduler.service.SchedulerCronService;
import com.peach.scheduler.service.SchedulerTriggerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 调度任务管理接口。
 *
 * <p>提供任务定义查询、创建、修改、生命周期操作、立即执行和 Cron 预览能力。
 * 所有写操作的操作人均从当前 Peach 安全上下文获取，禁止客户端伪造操作人标识。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Validated
@RestController
@RequestMapping("/scheduler/jobs")
@Tag(name = "调度任务管理", description = "调度任务管理")
@Indexed
public class SchedulerJobController {

    private final ISchedulerJobService schedulerJobService;
    private final SchedulerTriggerService schedulerTriggerService;
    private final SchedulerCronService schedulerCronService;

    /**
     * 创建调度任务管理接口。
     *
     * @param schedulerJobService 调度任务服务
     * @param schedulerTriggerService 任务触发服务
     * @param schedulerCronService Cron 校验和预览服务
     */
    public SchedulerJobController(ISchedulerJobService schedulerJobService,
                                  SchedulerTriggerService schedulerTriggerService,
                                  SchedulerCronService schedulerCronService) {
        this.schedulerJobService = schedulerJobService;
        this.schedulerTriggerService = schedulerTriggerService;
        this.schedulerCronService = schedulerCronService;
    }

    /**
     * 查询符合条件的调度任务列表。
     *
     * @param query 查询条件
     * @return 调度任务列表响应
     */
    @GetMapping
    @Operation(summary = "查询调度任务列表")
    public Response list(SchedulerJobQO query) {
        return Response.success(schedulerJobService.list(query));
    }

    /**
     * 查询指定调度任务详情。
     *
     * @param id 任务主键
     * @return 调度任务详情响应
     */
    @GetMapping("/{id}")
    @Operation(summary = "查询调度任务详情")
    public Response get(@PathVariable Long id) {
        return Response.success(schedulerJobService.get(id));
    }

    /**
     * 创建草稿状态的调度任务。
     *
     * @param data 任务定义请求
     * @return 创建结果
     */
    @PostMapping
    @Operation(summary = "创建调度任务")
    public Response create(@Valid @RequestBody SchedulerJobSaveDTO data) {
        return Response.success(schedulerJobService.create(data, currentUserId()));
    }

    /**
     * 修改调度任务定义。
     *
     * @param id 任务主键
     * @param data 任务定义请求
     * @return 修改结果
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新调度任务")
    public Response update(@PathVariable Long id, @Valid @RequestBody SchedulerJobSaveDTO data) {
        return Response.success(schedulerJobService.update(id, data, currentUserId()));
    }

    /**
     * 启用草稿或已停用的调度任务。
     *
     * @param id 任务主键
     * @return 状态迁移结果
     */
    @PostMapping("/{id}/enable")
    @Operation(summary = "启用调度任务")
    public Response enable(@PathVariable Long id) {
        return transition(id, JobEvent.ENABLE);
    }

    /**
     * 暂停已启用的调度任务。
     *
     * @param id 任务主键
     * @return 状态迁移结果
     */
    @PostMapping("/{id}/pause")
    @Operation(summary = "暂停调度任务")
    public Response pause(@PathVariable Long id) {
        return transition(id, JobEvent.PAUSE);
    }

    /**
     * 恢复已暂停的调度任务。
     *
     * @param id 任务主键
     * @return 状态迁移结果
     */
    @PostMapping("/{id}/resume")
    @Operation(summary = "恢复调度任务")
    public Response resume(@PathVariable Long id) {
        return transition(id, JobEvent.RESUME);
    }

    /**
     * 停用已启用或已暂停的调度任务。
     *
     * @param id 任务主键
     * @return 状态迁移结果
     */
    @PostMapping("/{id}/disable")
    @Operation(summary = "停用调度任务")
    public Response disable(@PathVariable Long id) {
        return transition(id, JobEvent.DISABLE);
    }

    /**
     * 通过任务状态机逻辑删除调度任务。
     *
     * @param id 任务主键
     * @return 状态迁移结果
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除调度任务")
    public Response delete(@PathVariable Long id) {
        return transition(id, JobEvent.DELETE);
    }

    /**
     * 立即创建一次人工触发执行实例，不修改原有调度计划。
     *
     * @param id 任务主键
     * @return 人工执行实例标识
     */
    @PostMapping("/{id}/run")
    @Operation(summary = "立即触发调度任务")
    public Response runNow(@PathVariable Long id) {
        return Response.success(schedulerTriggerService.triggerManual(id, currentUserId()));
    }

    /**
     * 预览 Quartz Cron 表达式未来触发时间。
     *
     * @param expression Quartz Cron 表达式
     * @param timeZone 调度时区
     * @param count 返回的未来触发时间数量
     * @return 未来触发时间列表
     */
    @GetMapping("/cron/preview")
    @Operation(summary = "预览 Quartz Cron 触发时间")
    public Response previewCron(@RequestParam String expression,
                                @RequestParam(defaultValue = "Asia/Shanghai") String timeZone,
                                @RequestParam(defaultValue = "5") int count) {
        return Response.success(schedulerCronService.preview(expression, timeZone, count));
    }

    private Response transition(Long id, JobEvent event) {
        return Response.success(schedulerJobService.transition(id, event, currentUserId()));
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
