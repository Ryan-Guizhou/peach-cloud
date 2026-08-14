package com.peach.scheduler.dao;

import com.peach.common.PeachDao;
import com.peach.common.annoation.MybatisDao;
import com.peach.scheduled.entity.SchedulerHandlerDO;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Indexed;

import java.util.List;

/**
 * 调度 Handler 注册信息数据访问接口。
 *
 * <p>用于维护业务服务上报的 Handler 能力、实例标识和心跳状态。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
@MybatisDao
public interface SchedulerHandlerDao extends PeachDao<SchedulerHandlerDO, SchedulerHandlerDO> {

    /**
     * 新增或刷新 Handler 注册信息。
     *
     * @param handler Handler 注册数据库对象
     * @return 受影响行数
     */
    int upsert(SchedulerHandlerDO handler);

    /**
     * 根据业务应用和 Handler 名称查询注册信息。
     *
     * @param applicationName 业务应用名称
     * @param handlerName Handler 名称
     * @return Handler 注册信息，不存在时返回 null
     */
    SchedulerHandlerDO selectOne(@Param("applicationName") String applicationName,
                                 @Param("handlerName") String handlerName);

    /**
     * 查询当前被认为在线的 Handler 注册信息。
     *
     * @return 在线 Handler 列表
     */
    List<SchedulerHandlerDO> selectOnline();

    /**
     * 将超过指定心跳时间窗口的 Handler 标记为离线。
     *
     * @param seconds 心跳超时秒数
     * @return 受影响行数
     */
    int markOfflineBefore(@Param("seconds") int seconds);
}
