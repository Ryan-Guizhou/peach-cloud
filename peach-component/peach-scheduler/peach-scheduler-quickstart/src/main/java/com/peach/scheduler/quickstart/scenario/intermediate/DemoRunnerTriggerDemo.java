package com.peach.scheduler.quickstart.scenario.intermediate;

import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.quickstart.scenario.DemoLevel;
import com.peach.scheduler.quickstart.scenario.ProgressiveDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * INTERMEDIATE-01：模拟 DemoRunner 触发一次本地执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
@Slf4j
@Indexed
@Component
public class DemoRunnerTriggerDemo implements ProgressiveDemo {

    private final DemoCleanupJob demoCleanupJob;

    public DemoRunnerTriggerDemo(DemoCleanupJob demoCleanupJob) {
        this.demoCleanupJob = demoCleanupJob;
    }

    @Override
    public DemoLevel level() {
        return DemoLevel.INTERMEDIATE;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String title() {
        return "INTERMEDIATE-01 DemoRunner-style trigger";
    }

    @Override
    public void execute() {
        JobContext context = new JobContext(
                "qs-exec-runner",
                "demoCleanupJob",
                "peach-scheduler-quickstart",
                "{}",
                1,
                "qs-runner-trace");
        JobResult result = demoCleanupJob.execute(context);
        log.info("[{}] success={}, code={}", title(), result.isSuccess(), result.getCode());
        if (!result.isSuccess() || !"SUCCESS".equals(result.getCode())) {
            throw new IllegalStateException(title() + " self-check failed");
        }
    }
}
