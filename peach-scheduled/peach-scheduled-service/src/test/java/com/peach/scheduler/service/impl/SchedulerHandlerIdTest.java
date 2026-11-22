package com.peach.scheduler.service.impl;

import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.entity.SchedulerHandlerDO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * 验证 Handler 主键可存入当前 MySQL BIGINT 字段。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 13:20
 */
class SchedulerHandlerIdTest {
    @Test
    void shouldGeneratePositiveSignedLongId() {
        SchedulerHandlerDao dao = mock(SchedulerHandlerDao.class);
        HandlerRegistrationDTO request = new HandlerRegistrationDTO();
        request.setApplicationName("peach-scheduler-quickstart");
        request.setInstanceId("demo-instance");
        HandlerRegistrationDTO.Item handler = new HandlerRegistrationDTO.Item();
        handler.setHandlerName("demoCleanupJob");
        request.getHandlers().add(handler);

        new SchedulerHandlerServiceImpl(dao, mock(com.peach.scheduler.dao.SchedulerJobDao.class),
                mock(com.peach.scheduler.dao.SchedulerJobVersionDao.class), new com.fasterxml.jackson.databind.ObjectMapper()).register(request);

        ArgumentCaptor<SchedulerHandlerDO> captured = ArgumentCaptor.forClass(SchedulerHandlerDO.class);
        verify(dao).upsert(captured.capture());
        assertThat(captured.getValue().getId()).matches("[0-9]{1,19}");
        assertThat(Long.parseLong(captured.getValue().getId())).isPositive();
    }
}
