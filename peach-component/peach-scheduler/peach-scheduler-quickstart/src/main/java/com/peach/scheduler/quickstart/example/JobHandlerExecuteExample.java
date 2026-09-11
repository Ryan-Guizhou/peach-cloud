package com.peach.scheduler.quickstart.example;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 直接调用 {@code @PeachJob} Handler，不经过 Claim 与执行器编排。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class JobHandlerExecuteExample {

    /**
     * Quickstart 本地 applicationName，须与执行命令一致。
     */
    public static final String APPLICATION_NAME = "peach-scheduler-quickstart";

    private final DemoCleanupJob demoCleanupJob;

    /**
     * 构造最小 {@link JobContext} 并执行示例 Handler。
     *
     * @param executionId 演示用执行标识
     * @return Handler 返回结果
     */
    public JobResult executeOnce(String executionId) {
        JobContext context = new JobContext(
                executionId,
                DemoCleanupJob.HANDLER_NAME,
                APPLICATION_NAME,
                "{}",
                1,
                "qs-handler-trace");
        JobResult result = demoCleanupJob.execute(context);
        log.info("jobHandler execute, executionId={}, handler={}, success={}",
                executionId, handlerName(), result.isSuccess());
        return result;
    }

    /**
     * @return {@link PeachJob#value()} 声明的 Handler 名称
     */
    public String handlerName() {
        return DemoCleanupJob.class.getAnnotation(PeachJob.class).value();
    }
}
