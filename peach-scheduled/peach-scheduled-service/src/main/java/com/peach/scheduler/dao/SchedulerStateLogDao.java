package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerStateLogDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 调度State日志数据访问。
 * <p>记录 Job 和 Execution 每次状态机迁移的前后状态、事件和操作人。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerStateLogDao extends PeachDao<SchedulerStateLogDO, SchedulerStateLogDO> {

    /**
     * 写入一条状态迁移日志。
     *
     * @param aggregateType 聚合类型
     * @param aggregateId 聚合标识
     * @param fromState 来源状态
     * @param event 状态机事件
     * @param toState 目标状态
     * @param operatorId 操作人标识
     * @param remark 迁移备注
     * @return 受影响行数
     */
    int insert(@Param("aggregateType") String aggregateType,
               @Param("aggregateId") String aggregateId,
               @Param("fromState") String fromState,
               @Param("event") String event,
               @Param("toState") String toState,
               @Param("operatorId") String operatorId,
               @Param("remark") String remark);
}
