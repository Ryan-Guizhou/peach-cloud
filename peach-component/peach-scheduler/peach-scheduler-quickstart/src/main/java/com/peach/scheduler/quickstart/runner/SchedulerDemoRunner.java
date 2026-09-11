package com.peach.scheduler.quickstart.runner;

import com.peach.scheduler.core.JobResult;
import com.peach.scheduler.model.ExecutionResultStatus;
import com.peach.scheduler.quickstart.example.ClaimGateExample;
import com.peach.scheduler.quickstart.example.ExecutorOrchestrationExample;
import com.peach.scheduler.quickstart.example.JobHandlerExecuteExample;
import com.peach.scheduler.quickstart.job.DemoCleanupJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示 JobHandler、Claim 门闩与 {@code PeachJobExecutor} 编排。
 *
 * <p>默认关闭：装配虚拟线程 {@code scheduler} 组后启动成本高于纯 Handler 切片。
 * 需要本地日志演示时设置 {@code quickstart.scheduler.demo.enabled=true}。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.scheduler.demo", name = "enabled", havingValue = "true")
public class SchedulerDemoRunner implements ApplicationRunner {

    private final JobHandlerExecuteExample handlerExample;
    private final ClaimGateExample claimExample;
    private final ExecutorOrchestrationExample executorExample;

    @Override
    public void run(ApplicationArguments args) {
        runHandlerDemo();
        runClaimDemo();
        runExecutorDemo();
        log.info("scheduler demo finished");
    }

    private void runHandlerDemo() {
        log.info("=== JobHandler / @PeachJob demo ===");
        JobResult result = handlerExample.executeOnce("qs-demo-handler");
        if (!result.isSuccess() || !DemoCleanupJob.HANDLER_NAME.equals(handlerExample.handlerName())) {
            throw new IllegalStateException("jobHandler demo failed");
        }
        log.info("handler demo ok, handler={}, code={}", handlerExample.handlerName(), result.getCode());
    }

    private void runClaimDemo() {
        log.info("=== Claim gate demo ===");
        ClaimGateExample.ClaimObservation observed = claimExample.evaluateAllowThenReject();
        log.info("claim demo ok, firstGranted={}, secondGranted={}",
                observed.isFirstGranted(), observed.isSecondGranted());
    }

    private void runExecutorDemo() {
        log.info("=== PeachJobExecutor orchestration demo ===");
        ExecutorOrchestrationExample.OrchestrationObservation claimed =
                executorExample.runWhenClaimed("qs-demo-exec-claimed");
        if (claimed.getHandlerRuns() != 1 || claimed.getStatus() != ExecutionResultStatus.SUCCEEDED) {
            throw new IllegalStateException("executor claimed path failed");
        }
        ExecutorOrchestrationExample.OrchestrationObservation rejected =
                executorExample.skipWhenClaimRejected("qs-demo-exec-rejected");
        if (rejected.getHandlerRuns() != 0 || rejected.getReportCount() != 0) {
            throw new IllegalStateException("executor rejected path failed");
        }
        log.info("executor demo ok, claimedRuns={}, rejectedRuns={}",
                claimed.getHandlerRuns(), rejected.getHandlerRuns());
    }
}
