package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerOperationLogDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

/**
 * 调度Operation日志数据访问。
 * <p>记录人工重试、人工取消等高风险管理操作及操作原因。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerOperationLogDao extends PeachDao<SchedulerOperationLogDO, SchedulerOperationLogDO> {

    /**
     * 写入一条成功完成的人工操作审计记录。
     *
     * @param operation 操作类型
     * @param targetType 操作目标类型
     * @param targetId 操作目标标识
     * @param operatorId 操作人标识
     * @param reason 操作原因
     * @return 受影响行数
     */
    int insertSuccess(@Param("operation") String operation,
                      @Param("targetType") String targetType,
                      @Param("targetId") String targetId,
                      @Param("operatorId") String operatorId,
                      @Param("reason") String reason);
}
