package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduled.qo.SchedulerJobQO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 调度任务数据访问接口。
 *
 * <p>基础 CRUD 继承 PeachDao，行锁读取和乐观锁更新通过显式 SQL 完成。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerJobDao extends PeachDao<SchedulerJobDO, SchedulerJobDO> {

    /**
     * 根据任务主键查询任务定义。
     *
     * @param id 任务主键
     * @return 任务定义，不存在时返回 null
     */
    SchedulerJobDO selectById(@Param("id") Long id);

    /**
     * 根据稳定任务编码查询任务定义。
     *
     * @param jobCode 任务编码
     * @return 任务定义，不存在时返回 null
     */
    SchedulerJobDO selectByCode(@Param("jobCode") String jobCode);

    /**
     * 根据任务主键加行锁读取任务定义。
     *
     * @param id 任务主键
     * @return 已加行锁的任务定义
     */
    SchedulerJobDO selectByIdForUpdate(@Param("id") Long id);

    /**
     * 新增调度任务定义。
     *
     * @param job 任务数据库对象     */
    void insert(SchedulerJobDO job);

    /**
     * 修改任务可配置定义并更新调度版本。
     *
     * @param job 任务数据库对象
     * @return 受影响行数
     */
    int updateDefinition(SchedulerJobDO job);

    /**
     * 使用来源状态和乐观锁版本更新任务生命周期状态。
     *
     * @param id 任务主键
     * @param fromState 来源状态
     * @param toState 目标状态
     * @param version 当前乐观锁版本
     * @param modifierId 修改人标识
     * @return 受影响行数
     */
    int updateState(@Param("id") Long id,
                    @Param("fromState") String fromState,
                    @Param("toState") String toState,
                    @Param("version") Long version,
                    @Param("modifierId") String modifierId);

    /**
     * 标记指定调度版本已成功同步到调度引擎。
     *
     * @param id 任务主键
     * @param scheduleVersion 调度定义版本
     * @return 受影响行数
     */
    int markSyncSuccess(@Param("id") Long id, @Param("scheduleVersion") Long scheduleVersion);

    /**
     * 记录指定调度版本同步失败及失败摘要。
     *
     * @param id 任务主键
     * @param scheduleVersion 调度定义版本
     * @param error 脱敏后的同步失败摘要
     * @return 受影响行数
     */
    int markSyncFailure(@Param("id") Long id,
                        @Param("scheduleVersion") Long scheduleVersion,
                        @Param("error") String error);

    /**
     * 查询等待同步到调度引擎的任务定义。
     *
     * @param limit 最大返回数量
     * @return 待同步任务列表
     */
    List<SchedulerJobDO> selectPendingSync(@Param("limit") int limit);

    /**
     * 根据管理端查询条件分页查询任务定义。
     *
     * @param query 查询条件
     * @return 任务定义列表
     */
    List<SchedulerJobDO> selectPage(SchedulerJobQO query);
}
