package com.peach.scheduler.service.impl;

import com.peach.scheduler.dao.SchedulerHandlerDao;
import com.peach.scheduler.dao.SchedulerJobDao;
import com.peach.scheduler.dao.SchedulerJobVersionDao;
import com.peach.scheduled.common.JobState;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import com.peach.scheduled.entity.SchedulerJobDO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 验证自动任务登记的幂等性与管理状态保护。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 14:40
 */
class SchedulerTaskRegistrationTest {
    @Test
    void missingDefinitionShouldOnlyRefreshHeartbeat() {
        var jobs = mock(SchedulerJobDao.class);
        var handlers = mock(SchedulerHandlerDao.class);
        var request = request("app");
        request.getHandlers().getFirst().setTask(null);
        new SchedulerHandlerServiceImpl(handlers, jobs, mock(SchedulerJobVersionDao.class),
                new com.fasterxml.jackson.databind.ObjectMapper()).register(request);
        verify(handlers).upsert(any());
        verifyNoInteractions(jobs);
    }

    @Test
    void unconfiguredDraftMustNotBeEnabledOrScheduled() {
        SchedulerJobDao jobs = mock(SchedulerJobDao.class);
        var job = new SchedulerJobDO();
        job.setId("1");
        job.setState(JobState.DRAFT);
        job.setScheduleType("CRON");
        when(jobs.selectById("1")).thenReturn(job);
        var lifecycle = new com.peach.scheduler.service.SchedulerJobLifecycleService(jobs,
                mock(SchedulerJobVersionDao.class), mock(com.peach.scheduler.dao.SchedulerStateLogDao.class),
                new com.peach.scheduler.statemachine.JobStateMachineFactory());
        assertThatThrownBy(() -> lifecycle.transition("1", com.peach.scheduled.common.JobEvent.ENABLE, "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("Configure the job schedule");
        verify(jobs, never()).updateState(any(), any(), any(), any(), any());
        var provider = mock(com.peach.scheduler.provider.SchedulingProvider.class);
        when(jobs.selectPendingSync(100)).thenReturn(java.util.List.of(job));
        new com.peach.scheduler.service.SchedulerReconciler(jobs, provider).reconcile();
        verifyNoInteractions(provider);
    }

    @Test
    void heartbeatShouldCreateDraftOnceAndPreserveAdministrativeChanges() {
        SchedulerJobDao jobs = mock(SchedulerJobDao.class);
        SchedulerJobVersionDao versions = mock(SchedulerJobVersionDao.class);
        Map<String, SchedulerJobDO> stored = new HashMap<>();
        doAnswer(call -> {
            SchedulerJobDO job = call.getArgument(0);
            stored.putIfAbsent(job.getJobCode(), job);
            return null;
        }).when(jobs).insertRegistered(any());
        when(jobs.selectByCode(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        when(jobs.selectByCodeForUpdate(anyString())).thenAnswer(call -> stored.get(call.getArgument(0)));
        var service = new SchedulerHandlerServiceImpl(mock(SchedulerHandlerDao.class), jobs, versions, new com.fasterxml.jackson.databind.ObjectMapper());
        var request = request("app-one");
        service.register(request);
        assertThat(stored).hasSize(1);
        SchedulerJobDO job = stored.values().iterator().next();
        assertThat(job.getState()).isEqualTo(JobState.DRAFT);
        assertThat(job.getCronExpression()).isEqualTo("0 0 * * * ?");
        assertThat(job.getJobCode()).hasSizeLessThanOrEqualTo(64);
        job.setState(JobState.PAUSED);
        job.setCronExpression("0 * * * * ?");
        job.setDescription("managed description");
        service.register(request);
        assertThat(stored).hasSize(1);
        assertThat(job.getState()).isEqualTo(JobState.PAUSED);
        assertThat(job.getCronExpression()).isEqualTo("0 * * * * ?");
        assertThat(job.getDescription()).isEqualTo("managed description");
        job.setState(JobState.DELETED);
        service.register(request);
        assertThat(job.getState()).isEqualTo(JobState.DELETED);
        verify(versions, times(1)).insertSnapshot(any());
        service.register(request("app-two"));
        assertThat(stored).hasSize(2);
        verify(versions, times(2)).insertSnapshot(any());
    }

    private HandlerRegistrationDTO request(String application) {
        var request = new HandlerRegistrationDTO();
        request.setApplicationName(application);
        request.setInstanceId("instance-one");
        var item = new HandlerRegistrationDTO.Item();
        item.setHandlerName("handler");
        var task = new com.peach.scheduled.dto.SchedulerJobSaveDTO();
        task.setJobCode(application + "_task");
        task.setJobName("Registered task");
        task.setApplicationName(application);
        task.setHandlerName("handler");
        task.setScheduleType("CRON");
        task.setCronExpression("0 0 * * * ?");
        item.setTask(task);
        request.getHandlers().add(item);
        return request;
    }
}
