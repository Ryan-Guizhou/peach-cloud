package com.peach.scheduler.quickstart.example;

import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.model.ExecutionResultStatus;
import com.peach.scheduler.quickstart.config.DemoExecutionLeaseClient;
import com.peach.scheduler.quickstart.config.DemoExecutionResultReporter;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.transport.JobExecutionCommand;
import com.peach.scheduler.transport.JobExecutionResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 走完整执行侧：{@code command -> PeachJobExecutor -> claim -> handler -> reporter}。
 *
 * <p>不投递真实 MQ；Consumer 仅在 {@code peach.rocket.enabled=true} 时作为生产接线样例存在。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ExecutorOrchestrationExample {

    private static final long TIMEOUT_MS = 5_000L;

    private final PeachJobExecutor peachJobExecutor;
    private final DemoCleanupJob demoCleanupJob;
    private final DemoExecutionLeaseClient leaseClient;
    private final DemoExecutionResultReporter resultReporter;

    /**
     * Claim 成功后执行 Handler 并上报结果。
     *
     * @param executionId 演示用执行标识
     * @return 编排观察结果
     */
    public OrchestrationObservation runWhenClaimed(String executionId) {
        return execute(executionId, true);
    }

    /**
     * Claim 失败时丢弃命令：Handler 不执行，也不上报结果。
     *
     * @param executionId 演示用执行标识
     * @return 编排观察结果
     */
    public OrchestrationObservation skipWhenClaimRejected(String executionId) {
        return execute(executionId, false);
    }

    private OrchestrationObservation execute(String executionId, boolean allowClaim) {
        leaseClient.setAllow(allowClaim);
        demoCleanupJob.reset();
        resultReporter.reset();
        peachJobExecutor.execute(command(executionId));
        JobExecutionResultEvent event = resultReporter.lastEvent();
        OrchestrationObservation observed = new OrchestrationObservation(
                demoCleanupJob.executeCount(),
                demoCleanupJob.lastExecutionId(),
                resultReporter.events().size(),
                event == null ? null : event.status(),
                event == null ? null : event.resultCode());
        log.info("executor orchestration, executionId={}, claimed={}, handlerRuns={}, reports={}, status={}",
                executionId, allowClaim, observed.getHandlerRuns(), observed.getReportCount(), observed.getStatus());
        return observed;
    }

    private static JobExecutionCommand command(String executionId) {
        return new JobExecutionCommand(
                executionId,
                DemoCleanupJob.HANDLER_NAME,
                JobHandlerExecuteExample.APPLICATION_NAME,
                DemoCleanupJob.HANDLER_NAME,
                "{}",
                TIMEOUT_MS,
                1,
                "qs-executor-trace");
    }

    /**
     * {@link PeachJobExecutor#execute} 后的 Handler / 上报观察结果。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/9/11 19:00
     */
    public static final class OrchestrationObservation {

        private final int handlerRuns;
        private final String lastExecutionId;
        private final int reportCount;
        private final ExecutionResultStatus status;
        private final String resultCode;

        public OrchestrationObservation(int handlerRuns, String lastExecutionId, int reportCount,
                                        ExecutionResultStatus status, String resultCode) {
            this.handlerRuns = handlerRuns;
            this.lastExecutionId = lastExecutionId;
            this.reportCount = reportCount;
            this.status = status;
            this.resultCode = resultCode;
        }

        public int getHandlerRuns() {
            return handlerRuns;
        }

        public String getLastExecutionId() {
            return lastExecutionId;
        }

        public int getReportCount() {
            return reportCount;
        }

        public ExecutionResultStatus getStatus() {
            return status;
        }

        public String getResultCode() {
            return resultCode;
        }
    }
}
