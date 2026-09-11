package com.peach.scheduler.quickstart.scenario.basic;

import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.quickstart.scenario.DemoLevel;
import com.peach.scheduler.quickstart.scenario.ProgressiveDemo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * BASIC-01：直接执行 JobHandler。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 17:30
 */
@Slf4j
@Indexed
@Component
public class JobHandlerExecuteDemo implements ProgressiveDemo {

    private final DemoCleanupJob demoCleanupJob;

    public JobHandlerExecuteDemo(DemoCleanupJob demoCleanupJob) {
        this.demoCleanupJob = demoCleanupJob;
    }

    @Override
    public DemoLevel level() {
        return DemoLevel.BASIC;
    }

    @Override
    public int order() {
        return 1;
    }

    @Override
    public String title() {
        return "BASIC-01 JobHandler execute";
    }

    @Override
    public void execute() {
        JobContext context = new JobContext(
                "qs-basic-exec",
                "demoCleanupJob",
                "peach-scheduler-quickstart",
                "{}",
                1,
                "qs-basic-trace");
        JobResult result = demoCleanupJob.execute(context);
        log.info("[{}] success={}, code={}", title(), result.isSuccess(), result.getCode());
        if (!result.isSuccess()) {
            throw new IllegalStateException(title() + " self-check failed");
        }
    }
}
