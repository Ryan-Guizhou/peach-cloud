package com.peach.auth.launch.scheduler;

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
class AuthSchedulerDemoJobTest {
    @Test
    void shouldRegisterAndExecuteDemo() {
        JobRegistry registry = new JobRegistry();
        registry.register(new AuthSchedulerDemoJob());
        assertThat(registry.getRequired("authSchedulerDemo").execute(null).isSuccess()).isTrue();
        assertThat(registry.descriptors()).hasSize(1);
    }

    @Test
    void shouldDelegateCommandsToManagedExecutor() {
        var executor = org.mockito.Mockito.mock(com.peach.scheduler.core.PeachJobExecutor.class);
        var command = new com.peach.scheduler.transport.JobExecutionCommand(
                "test-execution", "test-job", "peach-auth", "authSchedulerDemo", "{}", 30000, 1, "test-trace");
        new AuthSchedulerDemoConsumer(executor).handle(command, null);
        org.mockito.Mockito.verify(executor).execute(command);
    }

    @Test
    void shouldOnlyExposeHandlerWhenDemoIsEnabled() {
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withUserConfiguration(AuthSchedulerDemoJob.class)
                .run(context -> assertThat(context).doesNotHaveBean(AuthSchedulerDemoJob.class));
        new org.springframework.boot.test.context.runner.ApplicationContextRunner()
                .withPropertyValues("peach.scheduler.demo.enabled=true")
                .withUserConfiguration(AuthSchedulerDemoJob.class)
                .run(context -> assertThat(context).hasSingleBean(AuthSchedulerDemoJob.class));
    }
}
