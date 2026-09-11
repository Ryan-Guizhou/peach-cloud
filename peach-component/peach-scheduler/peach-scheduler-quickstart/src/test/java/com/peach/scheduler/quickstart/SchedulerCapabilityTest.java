package com.peach.scheduler.quickstart;

import com.peach.scheduler.core.JobDescriptor;
import com.peach.scheduler.core.JobRegistry;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.core.PeachJobExecutor;
import com.peach.scheduler.model.ExecutionResultStatus;
import com.peach.scheduler.quickstart.example.ClaimGateExample;
import com.peach.scheduler.quickstart.example.ExecutorOrchestrationExample;
import com.peach.scheduler.quickstart.example.JobHandlerExecuteExample;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import com.peach.scheduler.runtime.DefaultPeachJobExecutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 验证 {@code @PeachJob} 注册、Claim 门闩，以及 {@link PeachJobExecutor} 在内存桩上的编排。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@SpringBootTest(properties = {
        "quickstart.scheduler.demo.enabled=false",
        "peach.rocket.enabled=false"
})
class SchedulerCapabilityTest {

    @Autowired
    private PeachJobExecutor peachJobExecutor;

    @Autowired
    private JobRegistry jobRegistry;

    @Autowired
    private DemoCleanupJob demoCleanupJob;

    @Autowired
    private JobHandlerExecuteExample handlerExample;

    @Autowired
    private ClaimGateExample claimExample;

    @Autowired
    private ExecutorOrchestrationExample executorExample;

    @BeforeEach
    void resetHandler() {
        demoCleanupJob.reset();
    }

    @Test
    void shouldRegisterPeachJobAndExecuteHandler() {
        assertThat(peachJobExecutor).isInstanceOf(DefaultPeachJobExecutor.class);
        assertThat(jobRegistry.descriptors())
                .extracting(JobDescriptor::handlerName)
                .contains(DemoCleanupJob.HANDLER_NAME);

        JobResult result = handlerExample.executeOnce("it-handler-1");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(handlerExample.handlerName()).isEqualTo(DemoCleanupJob.HANDLER_NAME);
        assertThat(demoCleanupJob.lastExecutionId()).isEqualTo("it-handler-1");
    }

    @Test
    void claimShouldAllowThenReject() {
        ClaimGateExample.ClaimObservation observed = claimExample.evaluateAllowThenReject();

        assertThat(observed.isFirstGranted()).isTrue();
        assertThat(observed.isSecondGranted()).isFalse();
    }

    @Test
    void executorShouldRunHandlerAfterClaimAndSkipWhenRejected() {
        ExecutorOrchestrationExample.OrchestrationObservation claimed =
                executorExample.runWhenClaimed("it-exec-claimed");
        assertThat(claimed.getHandlerRuns()).isEqualTo(1);
        assertThat(claimed.getLastExecutionId()).isEqualTo("it-exec-claimed");
        assertThat(claimed.getReportCount()).isEqualTo(1);
        assertThat(claimed.getStatus()).isEqualTo(ExecutionResultStatus.SUCCEEDED);
        assertThat(claimed.getResultCode()).isEqualTo("SUCCESS");

        ExecutorOrchestrationExample.OrchestrationObservation rejected =
                executorExample.skipWhenClaimRejected("it-exec-rejected");
        assertThat(rejected.getHandlerRuns()).isZero();
        assertThat(rejected.getLastExecutionId()).isNull();
        assertThat(rejected.getReportCount()).isZero();
        assertThat(rejected.getStatus()).isNull();
    }
}
