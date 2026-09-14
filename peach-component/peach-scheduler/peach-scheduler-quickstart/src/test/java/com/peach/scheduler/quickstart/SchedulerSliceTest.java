package com.peach.scheduler.quickstart;

import com.peach.scheduler.annotation.PeachJob;
import com.peach.scheduler.core.JobContext;
import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.quickstart.config.DemoExecutionLeaseClient;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 无 Spring 容器的切片：直接证明 {@link com.peach.scheduler.core.JobHandler} 执行与 Claim 门闩开关。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
class SchedulerSliceTest {

    @Test
    void handlerShouldSucceedAndDeclarePeachJob() {
        DemoCleanupJob job = new DemoCleanupJob();
        JobContext context = new JobContext(
                "slice-exec-1",
                DemoCleanupJob.HANDLER_NAME,
                "peach-scheduler-quickstart",
                "{}",
                1,
                "slice-trace");

        JobResult result = job.execute(context);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getCode()).isEqualTo("SUCCESS");
        assertThat(job.executeCount()).isEqualTo(1);
        assertThat(job.lastExecutionId()).isEqualTo("slice-exec-1");
        assertThat(DemoCleanupJob.class.getAnnotation(PeachJob.class).value())
                .isEqualTo(DemoCleanupJob.HANDLER_NAME);
    }

    @Test
    void claimClientShouldAllowThenReject() {
        DemoExecutionLeaseClient leaseClient = new DemoExecutionLeaseClient();

        leaseClient.setAllow(true);
        boolean allowed = leaseClient.claim("slice-claim-allow", "slice-instance");
        leaseClient.setAllow(false);
        boolean rejected = leaseClient.claim("slice-claim-reject", "slice-instance");

        assertThat(allowed).isTrue();
        assertThat(rejected).isFalse();
        assertThat(leaseClient.claimAttempts()).isEqualTo(2);
    }
}
