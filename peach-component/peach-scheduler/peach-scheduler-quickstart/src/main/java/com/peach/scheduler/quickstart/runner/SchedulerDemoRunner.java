package com.peach.scheduler.quickstart.runner;

import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 启动后本地执行一次 DemoCleanupJob，便于无控制面时验证 Handler。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 16:40
 */
@Component
@ConditionalOnProperty(prefix = "quickstart.scheduler.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SchedulerDemoRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SchedulerDemoRunner.class);

    private final DemoCleanupJob demoCleanupJob;

    /**
     * @param demoCleanupJob 示例清理任务
     */
    public SchedulerDemoRunner(DemoCleanupJob demoCleanupJob) {
        this.demoCleanupJob = demoCleanupJob;
    }

    @Override
    public void run(ApplicationArguments args) {
        JobContext context = new JobContext(
                "qs-exec-1",
                "demoCleanupJob",
                "peach-scheduler-quickstart",
                "{}",
                1,
                "qs-trace");
        JobResult result = demoCleanupJob.execute(context);
        log.info("scheduler quickstart finished, success={}, code={}", result.isSuccess(), result.getCode());
    }
}
