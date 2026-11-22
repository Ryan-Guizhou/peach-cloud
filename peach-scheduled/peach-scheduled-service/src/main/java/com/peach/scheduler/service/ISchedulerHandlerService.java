package com.peach.scheduler.service;

import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.qo.SchedulerHandlerQO;
import com.peach.scheduled.vo.SchedulerHandlerVO;

import java.util.List;

/**
 * IScheduler处理器服务类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
public interface ISchedulerHandlerService {

    /**
     * 注册或刷新业务服务上报的 Handler。
     *
     * @param request Handler 注册请求
     */
    void register(HandlerRegistrationDTO request);

    /**
     * 将心跳超时的 Handler 标记为离线。
     */
    void markStaleHandlersOffline();

    /**
     * 筛选 Handler 注册记录。
     * @param query 查询条件
     * @return 匹配的 Handler
     */
    List<SchedulerHandlerVO> list(SchedulerHandlerQO query);

}
