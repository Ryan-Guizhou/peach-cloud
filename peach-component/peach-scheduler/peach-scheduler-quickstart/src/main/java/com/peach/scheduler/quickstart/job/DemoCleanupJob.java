package com.peach.scheduler.quickstart.job;

import org.springframework.stereotype.Indexed;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * quickstart 示例清理任务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Component
@PeachJob(value = "demoCleanupJob", description = "清理过期示例数据")
@Indexed
public class DemoCleanupJob implements JobHandler {
    private static final Logger log = LoggerFactory.getLogger(DemoCleanupJob.class);
    /**
     * 创建示例清理任务。
     */
    public DemoCleanupJob() {
        // Intentionally empty.
    }
    /**
     * 执行示例清理任务并输出调度定位日志。
     */
    @Override
    public JobResult execute(JobContext context) {
        log.info("Demo scheduler handler executed, executionId={}, jobCode={}", context.executionId(), context.jobCode());
        return JobResult.success();
    }
}
