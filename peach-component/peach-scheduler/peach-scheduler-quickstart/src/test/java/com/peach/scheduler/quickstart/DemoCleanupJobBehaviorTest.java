package com.peach.scheduler.quickstart;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.quickstart.runner.SchedulerDemoRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 DemoCleanupJob 可直接执行并返回成功，以及 DemoRunner 可关闭。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
class DemoCleanupJobBehaviorTest {

    @Test
    void shouldExecuteSuccessfully() {
        DemoCleanupJob job = new DemoCleanupJob();
        JobContext context = new JobContext(
                "it-exec-1",
                "demoCleanupJob",
                "peach-scheduler-quickstart",
                "{}",
                1,
                "it-trace");

        JobResult result = job.execute(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(DemoCleanupJob.class.getAnnotation(PeachJob.class).value()).isEqualTo("demoCleanupJob");
    }

    @Test
    void shouldAllowDisablingDemoRunner() {
        ConditionalOnProperty condition = SchedulerDemoRunner.class.getAnnotation(ConditionalOnProperty.class);
        assertThat(condition).isNotNull();
        assertThat(condition.prefix()).isEqualTo("quickstart.scheduler.demo");
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.havingValue()).isEqualTo("true");
        assertThat(condition.matchIfMissing()).isTrue();
    }
}
