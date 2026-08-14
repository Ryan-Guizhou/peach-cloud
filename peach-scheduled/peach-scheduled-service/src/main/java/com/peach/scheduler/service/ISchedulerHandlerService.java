package com.peach.scheduler.service;

import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.vo.SchedulerHandlerVO;

import java.util.List;

/**
 * 调度 Handler 注册服务接口。
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
     * 查询当前在线 Handler。
     *
     * @return 在线 Handler 列表
     */
    List<SchedulerHandlerVO> list();
}
