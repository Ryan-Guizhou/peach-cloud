package com.peach.setting.launch.scheduler;

import com.peach.scheduler.core.JobRegistry;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证测试 Handler 注册名称和无业务副作用的成功结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/21 22:00
 */
class SettingSchedulerDemoJobTest {
    @Test
    void shouldRegisterAndExecuteDemo() {
        JobRegistry registry = new JobRegistry();
        registry.register(new SettingSchedulerDemoJob());
        assertThat(registry.getRequired("settingSchedulerDemo").execute(null).isSuccess()).isTrue();
        assertThat(registry.descriptors()).hasSize(1);
    }

    @Test
    void shouldDelegateCommandsToManagedExecutor() {
        var executor = org.mockito.Mockito.mock(com.peach.scheduler.core.PeachJobExecutor.class);
        var command = new com.peach.scheduler.transport.JobExecutionCommand(
                "test-execution", "test-job", "peach-setting", "settingSchedulerDemo", "{}", 30000, 1, "test-trace");
        new SettingSchedulerDemoConsumer(executor).handle(command, null);
        org.mockito.Mockito.verify(executor).execute(command);
    }

    @Test
    void shouldOnlyExposeHandlerWhenDemoIsEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(SettingSchedulerDemoJob.class)
                .run(context -> assertThat(context).doesNotHaveBean(SettingSchedulerDemoJob.class));
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withPropertyValues("peach.scheduler.demo.enabled=true")
                .withUserConfiguration(SettingSchedulerDemoJob.class)
                .run(context -> assertThat(context).hasSingleBean(SettingSchedulerDemoJob.class));
    }
}
