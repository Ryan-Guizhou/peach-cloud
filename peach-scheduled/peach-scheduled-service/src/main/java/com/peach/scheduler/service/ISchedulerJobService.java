package com.peach.scheduler.service;

import com.peach.scheduled.common.JobEvent;
import com.peach.scheduled.dto.SchedulerJobSaveDTO;
import com.peach.scheduled.vo.SchedulerJobVO;
import com.peach.scheduled.qo.SchedulerJobQO;

import java.util.List;

/**
 * IScheduler任务服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ISchedulerJobService {

    /**
     * 创建草稿任务。
     *
     * @param data 任务定义请求
     * @param operatorId 操作人 ID
     * @return 创建后的任务定义
     */
    SchedulerJobVO create(SchedulerJobSaveDTO data, String operatorId);

    /**
     * 更新任务定义。
     *
     * @param jobId 任务 ID
     * @param data 任务定义请求
     * @param operatorId 操作人 ID
     * @return 更新后的任务定义
     */
    SchedulerJobVO update(String jobId, SchedulerJobSaveDTO data, String operatorId);

    /**
     * 查询任务列表。
     *
     * @param query 查询条件
     * @return 任务列表
     */
    List<SchedulerJobVO> list(SchedulerJobQO query);

    /**
     * 根据主键查询任务。
     *
     * @param jobId 任务 ID
     * @return 任务定义
     */
    SchedulerJobVO get(String jobId);

    /**
     * 执行任务生命周期状态迁移。
     *
     * @param jobId 任务 ID
     * @param event 状态机事件
     * @param operatorId 操作人 ID
     * @return 状态迁移后的任务定义
     */
    SchedulerJobVO transition(String jobId, JobEvent event, String operatorId);
}
