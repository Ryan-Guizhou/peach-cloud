package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerJobVersionDO;
import org.springframework.stereotype.Indexed;
import com.peach.scheduled.entity.SchedulerJobDO;

/**
 * 调度任务Version数据访问。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerJobVersionDao extends PeachDao<SchedulerJobVersionDO, SchedulerJobVersionDO> {

    /**
     * 写入当前任务定义的不可变版本快照。
     *
     * @param job 当前任务定义
     * @return 受影响行数
     */
    int insertSnapshot(SchedulerJobDO job);
}
