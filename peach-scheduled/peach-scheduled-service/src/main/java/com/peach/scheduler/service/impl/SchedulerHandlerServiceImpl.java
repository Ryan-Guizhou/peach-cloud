package com.peach.scheduler.service.impl;

import org.springframework.stereotype.Indexed;

import com.peach.common.unique.UniqueIdFacade;
import com.peach.scheduler.service.ISchedulerHandlerService;
import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerJobVersionDao;
import com.peach.scheduled.entity.SchedulerJobDO;
import com.peach.scheduled.common.JobState;
import com.peach.scheduled.common.SyncStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;

import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.entity.SchedulerHandlerDO;
import com.peach.scheduled.vo.SchedulerHandlerVO;
import org.springframework.beans.BeanUtils;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

/**
 * 调度处理器服务实现类。
 * <p>负责接收业务实例 Handler 能力上报、刷新心跳状态并向管理端提供 VO 列表。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerHandlerServiceImpl implements ISchedulerHandlerService {

    private final SchedulerHandlerDao handlerDao;

    private final SchedulerJobDao jobDao;

    private final SchedulerJobVersionDao jobVersionDao;
    private final ObjectMapper objectMapper;

    /**
     * 创建实例。
     *
     * @param handlerDao handler Dao。
     * @param jobDao 任务定义 DAO
     * @param jobVersionDao 任务版本 DAO
     */
    public SchedulerHandlerServiceImpl(SchedulerHandlerDao handlerDao, SchedulerJobDao jobDao,
                                       SchedulerJobVersionDao jobVersionDao, ObjectMapper objectMapper) {
        this.handlerDao = handlerDao;
        this.jobDao = jobDao;
        this.jobVersionDao = jobVersionDao;
        this.objectMapper = objectMapper;
    }

    /**
     * 注册相关能力。
     *
     * @param request request。
     */
    @Transactional
    @Override
    public void register(HandlerRegistrationDTO request) {
        for (HandlerRegistrationDTO.Item item : request.getHandlers()) {
            SchedulerHandlerDO handler = new SchedulerHandlerDO();
            // Handler 主键是 BIGINT，不能依赖可能返回 UUID 的全局默认生成器。
            handler.setId(UniqueIdFacade.generateSnowflakeId());
            handler.setApplicationName(request.getApplicationName());
            handler.setHandlerName(item.getHandlerName());
            handler.setDescription(item.getDescription());
            handler.setInstanceId(request.getInstanceId());
            handlerDao.upsert(handler);
            registerDefaultJob(request.getApplicationName(), item);
        }
    }

    private void registerDefaultJob(String applicationName, HandlerRegistrationDTO.Item item) {
        var definition = item.getTask();
        if (definition == null) return;
        if (!applicationName.equals(definition.getApplicationName())
                || !item.getHandlerName().equals(definition.getHandlerName())) {
            throw new IllegalArgumentException("Registered task identity must match its Handler");
        }
        String code = definition.getJobCode();
        SchedulerJobDO existing = jobDao.selectByCode(code);
        if (existing != null) {
            if (!applicationName.equals(existing.getApplicationName())
                    || !item.getHandlerName().equals(existing.getHandlerName())) {
                throw new IllegalArgumentException("Job code belongs to another Handler");
            }
            return;
        }
        SchedulerJobServiceImpl.validateDefinition(definition, objectMapper);
        SchedulerJobDO job = new SchedulerJobDO();
        BeanUtils.copyProperties(definition, job, "startAt");
        if (definition.getStartAt() != null && !definition.getStartAt().isBlank()) {
            job.setStartAt(java.time.LocalDateTime.parse(definition.getStartAt()));
        }
        job.setId(UniqueIdFacade.generateSnowflakeId());
        job.setState(JobState.DRAFT);
        job.setScheduleVersion(1L);
        job.setSyncStatus(SyncStatus.SYNCED);
        job.setVersion(0L);
        job.setCreatorId("handler-registration");
        job.setModifierId("handler-registration");
        jobDao.insertRegistered(job);
        SchedulerJobDO stored = jobDao.selectByCodeForUpdate(code);
        if (stored == null || !applicationName.equals(stored.getApplicationName())
                || !item.getHandlerName().equals(stored.getHandlerName())) {
            throw new IllegalArgumentException("Job code belongs to another Handler");
        }
        if (stored != null && job.getId().equals(stored.getId())) {
            jobVersionDao.insertSnapshot(stored);
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
     * 按管理条件查询 Handler，包括离线记录。
     * @param query 查询条件
     * @return 匹配的 Handler
     */
    @Override
    public List<SchedulerHandlerVO> list(com.peach.scheduled.qo.SchedulerHandlerQO query) {
        List<SchedulerHandlerDO> handlers = handlerDao.selectFiltered(query);
        List<SchedulerHandlerVO> result = new ArrayList<SchedulerHandlerVO>(handlers.size());
        for (SchedulerHandlerDO handler : handlers) {
            SchedulerHandlerVO vo = new SchedulerHandlerVO();
            BeanUtils.copyProperties(handler, vo);
            result.add(vo);
        }
        return result;
    }
}
