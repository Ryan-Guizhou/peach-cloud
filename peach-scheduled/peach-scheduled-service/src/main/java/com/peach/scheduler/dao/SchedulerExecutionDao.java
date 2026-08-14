package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerExecutionDO;
import com.peach.scheduled.qo.SchedulerExecutionQO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 调度执行实例数据访问接口。
 *
 * <p>执行实例包含原子 Claim、租约、恢复和状态机更新等并发语义，
 * 所有状态写入均使用受限 SQL 和乐观锁，不通过通用 CRUD 直接修改。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerExecutionDao extends PeachDao<SchedulerExecutionDO, SchedulerExecutionDO> {

    /**
     * 根据执行实例标识查询当前持久化状态。
     *
     * @param executionId 执行实例标识
     * @return 执行实例，不存在时返回 null
     */
    SchedulerExecutionDO selectById(@Param("executionId") String executionId);

    /**
     * 新增一次逻辑执行实例。
     *
     * @param execution 执行实例数据库对象
     * @return 受影响行数
     */
    int insertIgnore(SchedulerExecutionDO execution);

    /**
     * 使用来源状态和乐观锁版本执行普通状态迁移。
     *
     * @param executionId 执行实例标识
     * @param fromState 来源状态
     * @param toState 目标状态
     * @param version 当前乐观锁版本
     * @param errorType 失败类型
     * @param errorMessage 脱敏后的失败摘要
     * @return 受影响行数
     */
    int updateState(@Param("executionId") String executionId,
                    @Param("fromState") String fromState,
                    @Param("toState") String toState,
                    @Param("version") Long version,
                    @Param("errorType") String errorType,
                    @Param("errorMessage") String errorMessage);

    /**
     * 原子抢占待执行实例并写入执行租约。
     *
     * @param executionId 执行实例标识
     * @param executorInstance 执行器实例标识
     * @param leaseUntil 租约失效时间
     * @param version 当前乐观锁版本
     * @return 抢占成功时受影响一行，否则为零
     */
    int claim(@Param("executionId") String executionId,
              @Param("executorInstance") String executorInstance,
              @Param("leaseUntil") LocalDateTime leaseUntil,
              @Param("version") Long version);

    /**
     * 完成一次运行中的执行实例并清理租约。
     *
     * @param executionId 执行实例标识
     * @param fromState 来源状态
     * @param toState 目标状态
     * @param version 当前乐观锁版本
     * @param executorInstance 当前租约持有者
     * @param finishTime 实际结束时间
     * @param durationMs 执行耗时，单位毫秒
     * @param errorType 失败类型
     * @param errorMessage 脱敏后的失败摘要
     * @return 受影响行数
     */
    int complete(@Param("executionId") String executionId,
                 @Param("fromState") String fromState,
                 @Param("toState") String toState,
                 @Param("version") Long version,
                 @Param("executorInstance") String executorInstance,
                 @Param("finishTime") LocalDateTime finishTime,
                 @Param("durationMs") Long durationMs,
                 @Param("errorType") String errorType,
                 @Param("errorMessage") String errorMessage);

    /**
     * 将失败执行实例转入等待重试并记录下次可重试时间。
     *
     * @param executionId 执行实例标识
     * @param version 当前乐观锁版本
     * @param nextRetryTime 下次允许重试时间
     * @param errorType 失败类型
     * @param errorMessage 脱敏后的失败摘要
     * @return 受影响行数
     */
    int scheduleRetry(@Param("executionId") String executionId,
                      @Param("version") Long version,
                      @Param("nextRetryTime") LocalDateTime nextRetryTime,
                      @Param("errorType") String errorType,
                      @Param("errorMessage") String errorMessage);

    /**
     * 将等待重试的执行实例重新转为已入队状态。
     *
     * @param executionId 执行实例标识
     * @param version 当前乐观锁版本
     * @return 受影响行数
     */
    int requeueRetry(@Param("executionId") String executionId, @Param("version") Long version);

    /**
     * 统计指定任务当前仍处于活跃状态的执行实例数量。
     *
     * @param jobId 任务主键
     * @return 活跃执行实例数量
     */
    int countActiveByJobId(@Param("jobId") Long jobId);

    /**
     * 查询因串行并发策略而暂缓分发的 CREATED 执行实例。
     *
     * @param limit 最大返回数量
     * @return 暂缓分发的执行实例列表
     */
    List<SchedulerExecutionDO> selectDeferredCreated(@Param("limit") int limit);

    /**
     * 查询租约已经过期的 RUNNING 执行实例。
     *
     * @param now 当前时间
     * @param limit 最大返回数量
     * @return 租约过期的执行实例列表
     */
    List<SchedulerExecutionDO> selectExpiredRunning(@Param("now") LocalDateTime now,
                                                    @Param("limit") int limit);

    /**
     * 查询已到达重试时间的执行实例。
     *
     * @param now 当前时间
     * @param limit 最大返回数量
     * @return 到期重试执行实例列表
     */
    List<SchedulerExecutionDO> selectDueRetries(@Param("now") LocalDateTime now,
                                                @Param("limit") int limit);

    /**
     * 根据管理端查询条件分页查询执行实例。
     *
     * @param query 查询条件
     * @return 执行实例列表
     */
    List<SchedulerExecutionDO> selectPage(SchedulerExecutionQO query);
}
