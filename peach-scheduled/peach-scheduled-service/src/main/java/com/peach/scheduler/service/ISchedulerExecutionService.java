package com.peach.scheduler.service;

import com.peach.scheduled.vo.SchedulerExecutionVO;
import com.peach.scheduled.qo.SchedulerExecutionQO;
import com.peach.scheduler.transport.JobExecutionResultEvent;

import java.util.List;

/**
 * IScheduler执行服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ISchedulerExecutionService {

    /**
     * 原子抢占执行实例租约。
     *
     * @param executionId 执行实例 ID
     * @param executorInstance 执行器实例标识
     * @return 抢占成功返回 true
     */
    boolean claim(String executionId, String executorInstance);

    /**
     * 处理执行结果事件。
     *
     * @param event 执行结果事件
     */
    void processResult(JobExecutionResultEvent event);

    /**
     * 人工重新投递等待重试的执行实例。
     *
     * @param executionId 执行实例 ID
     * @param operatorId 操作人 ID
     * @param reason 操作原因
     * @return 重新投递成功返回 true
     */
    boolean retry(String executionId, String operatorId, String reason);

    /**
     * 取消尚未开始业务执行的执行实例。
     *
     * @param executionId 执行实例 ID
     * @param operatorId 操作人 ID
     * @param reason 操作原因
     * @return 取消后的执行实例
     */
    SchedulerExecutionVO cancel(String executionId, String operatorId, String reason);

    /**
     * 查询执行实例列表。
     *
     * @param query 查询条件
     * @return 执行实例列表
     */
    List<SchedulerExecutionVO> list(SchedulerExecutionQO query);

    /**
     * 根据执行实例 ID 查询详情。
     *
     * @param executionId 执行实例 ID
     * @return 执行实例
     */
    SchedulerExecutionVO get(String executionId);
}
