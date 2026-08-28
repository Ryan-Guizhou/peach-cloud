package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerExecutionAttemptDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.time.LocalDateTime;

/**
 * 调度执行Attempt数据访问。
 * <p>每次实际业务执行对应一条 attempt 记录，用于保留重试历史和失败摘要。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerExecutionAttemptDao extends PeachDao<SchedulerExecutionAttemptDO, SchedulerExecutionAttemptDO> {

    /**
     * 写入一次执行尝试的开始记录。
     *
     * @param executionId 执行实例标识
     * @param attemptNo 尝试序号
     * @param executorInstance 执行器实例标识
     * @param startTime 实际开始时间
     * @return 受影响行数
     */
    int insertStart(@Param("executionId") String executionId,
                    @Param("attemptNo") int attemptNo,
                    @Param("executorInstance") String executorInstance,
                    @Param("startTime") LocalDateTime startTime);

    /**
     * 完成指定执行尝试并记录结果摘要。
     *
     * @param executionId 执行实例标识
     * @param attemptNo 尝试序号
     * @param state 尝试结束状态
     * @param finishTime 实际结束时间
     * @param durationMs 执行耗时，单位毫秒
     * @param errorType 失败类型
     * @param errorMessage 脱敏后的失败摘要
     * @return 受影响行数
     */
    int complete(@Param("executionId") String executionId,
                 @Param("attemptNo") int attemptNo,
                 @Param("state") String state,
                 @Param("finishTime") LocalDateTime finishTime,
                 @Param("durationMs") long durationMs,
                 @Param("errorType") String errorType,
                 @Param("errorMessage") String errorMessage);
}
