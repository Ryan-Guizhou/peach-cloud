package com.peach.scheduler.service.impl;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.service.ISchedulerHandlerService;
import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.entity.SchedulerHandlerDO;
import com.peach.scheduled.vo.SchedulerHandlerVO;
import org.springframework.beans.BeanUtils;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度 Handler 注册服务实现。
 *
 * <p>负责接收业务实例 Handler 能力上报、刷新心跳状态并向管理端提供 VO 列表。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerHandlerServiceImpl implements ISchedulerHandlerService {
    private final SchedulerHandlerDao handlerDao;

    /**
     * 创建相关对象。
     *
     * @param handlerDao 参数说明
     */
    public SchedulerHandlerServiceImpl(SchedulerHandlerDao handlerDao) {
        this.handlerDao = handlerDao;
    }

    /**
     * 注册相关能力。
     *
     * @param request 参数说明
     */
    @Transactional
    @Override
    public void register(HandlerRegistrationDTO request) {
        for (HandlerRegistrationDTO.Item item : request.getHandlers()) {
            SchedulerHandlerDO handler = new SchedulerHandlerDO();
            handler.setApplicationName(request.getApplicationName());
            handler.setHandlerName(item.getHandlerName());
            handler.setDescription(item.getDescription());
            handler.setInstanceId(request.getInstanceId());
            handlerDao.upsert(handler);
        }
    }

    /**
     * 更新相关状态。
     */
    @Scheduled(fixedDelayString = "${peach.scheduler.service.handler-offline-scan-ms:60000}")
    @Override
    public void markStaleHandlersOffline() {
        handlerDao.markOfflineBefore(180);
    }

    /**
     * 获取相关数据。
     *
     * @return 返回结果
     */
    @Override
    public List<SchedulerHandlerVO> list() {
        List<SchedulerHandlerDO> handlers = handlerDao.selectOnline();
        java.util.ArrayList<SchedulerHandlerVO> result = new java.util.ArrayList<SchedulerHandlerVO>(handlers.size());
        for (SchedulerHandlerDO handler : handlers) {
            SchedulerHandlerVO vo = new SchedulerHandlerVO();
            BeanUtils.copyProperties(handler, vo);
            result.add(vo);
        }
        return result;
    }
}
