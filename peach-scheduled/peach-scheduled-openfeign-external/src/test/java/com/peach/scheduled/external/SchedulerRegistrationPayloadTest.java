package com.peach.scheduled.external;

import com.peach.scheduler.config.JobRegistrationDefinition;
import com.peach.scheduler.config.PeachSchedulerProperties;
import com.peach.scheduler.core.JobDescriptor;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * 验证注册载荷使用任务注解身份及配置中心调度定义。
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 15:00
 */
class SchedulerRegistrationPayloadTest {
    @Test
    void declaredTaskWithoutNacosDefinitionMustFailClearly() {
        var registry = mock(JobRegistry.class);
        when(registry.descriptors()).thenReturn(List.of(new JobDescriptor("handler", "description", "job-code", "Job name")));
        var properties = new PeachSchedulerProperties();
        properties.getExecutor().setApplicationName("app");
        var client = mock(SchedulerHandlerExternalClient.class);
        assertThatThrownBy(() -> new SchedulerHandlerRegistrationInitializer(registry, client, properties)
                .afterSingletonsInstantiated()).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("configured registration definition");
        verifyNoInteractions(client);
    }

    @Test
    void shouldReportCompleteDefinitionInsteadOfPlaceholder() {
        var registry = mock(JobRegistry.class);
        when(registry.descriptors()).thenReturn(List.of(new JobDescriptor("handler", "description", "job-code", "Job name")));
        var properties = new PeachSchedulerProperties();
        properties.getExecutor().setApplicationName("app");
        var definition = new JobRegistrationDefinition();
        definition.setScheduleType("FIXED_INTERVAL");
        definition.setIntervalSeconds(60L);
        definition.setTimeZone("Asia/Shanghai");
        definition.setMisfirePolicy("SKIP");
        definition.setConcurrencyPolicy("DISALLOW");
        definition.setTimeoutMs(30000L);
        definition.setMaxAttempts(1);
        definition.setRetryIntervalSeconds(10);
        definition.setParametersJson("{}");
        properties.getExecutor().getJobs().put("handler", definition);
        var client = mock(SchedulerHandlerExternalClient.class);
        new SchedulerHandlerRegistrationInitializer(registry, client, properties).afterSingletonsInstantiated();
        var captured = ArgumentCaptor.forClass(HandlerRegistrationDTO.class);
        verify(client).register(captured.capture());
        var task = captured.getValue().getHandlers().getFirst().getTask();
        assertThat(task.getJobCode()).isEqualTo("job-code");
        assertThat(task.getJobName()).isEqualTo("Job name");
        assertThat(task.getApplicationName()).isEqualTo("app");
        assertThat(task.getIntervalSeconds()).isEqualTo(60L);
        assertThat(task.getCronExpression()).isNull();
        assertThat(task.getTimeoutMs()).isEqualTo(30000L);
        assertThat(task.getRetryIntervalSeconds()).isEqualTo(10);
    }
}
