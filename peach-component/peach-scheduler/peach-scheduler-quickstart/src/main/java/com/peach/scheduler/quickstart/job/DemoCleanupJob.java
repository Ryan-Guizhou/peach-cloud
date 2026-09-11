package com.peach.scheduler.quickstart.job;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobHandler;
import com.peach.scheduler.core.JobResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Quickstart 示例清理任务，用于证明 {@code @PeachJob} / {@link JobHandler} 本地可执行。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@PeachJob(value = DemoCleanupJob.HANDLER_NAME, description = "清理过期示例数据")
public class DemoCleanupJob implements JobHandler {

    /**
     * 与控制面任务定义对齐的 Handler 名称。
     */
    public static final String HANDLER_NAME = "demoCleanupJob";

    private final AtomicInteger executeCount = new AtomicInteger();
    private volatile String lastExecutionId;

    /**
     * 执行示例清理并记录次数，供 Claim 门闩与执行器编排断言。
     */
    @Override
    public JobResult execute(JobContext context) {
        lastExecutionId = context.executionId();
        int count = executeCount.incrementAndGet();
        log.info("demo handler executed, executionId={}, jobCode={}, count={}",
                context.executionId(), context.jobCode(), count);
        return JobResult.success();
    }

    /**
     * @return 已执行次数
     */
    public int executeCount() {
        return executeCount.get();
    }

    /**
     * @return 最近一次执行的 executionId，未执行时为 {@code null}
     */
    public String lastExecutionId() {
        return lastExecutionId;
    }

    /**
     * 清空执行计数，保证演示与测试互相隔离。
     */
    public void reset() {
        executeCount.set(0);
        lastExecutionId = null;
    }
}
