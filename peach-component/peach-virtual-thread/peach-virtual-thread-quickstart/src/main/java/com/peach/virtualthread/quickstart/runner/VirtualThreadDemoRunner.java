package com.peach.virtualthread.quickstart.runner;

import com.peach.virtualthread.exception.VirtualTaskRejectReason;
import com.peach.virtualthread.quickstart.example.VirtualCancelExample;
import com.peach.virtualthread.quickstart.example.VirtualGroupSubmitExample;
import com.peach.virtualthread.quickstart.example.VirtualRejectExample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Indexed;

/**
 * 启动后演示分组 submit、受管 {@code cancel(true)} 与 REJECT 背压。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 18:50
 */
@Slf4j
@Indexed
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "quickstart.virtual-thread.demo", name = "enabled", havingValue = "true", matchIfMissing = true)
public class VirtualThreadDemoRunner implements ApplicationRunner {

    private static final String DEMO_PAYLOAD = "qs-virtual-thread";

    private final VirtualGroupSubmitExample submitExample;
    private final VirtualCancelExample cancelExample;
    private final VirtualRejectExample rejectExample;

    @Override
    public void run(ApplicationArguments args) {
        runSubmitDemo();
        runCancelDemo();
        runRejectDemo();
        log.info("virtual-thread demo finished");
    }

    private void runSubmitDemo() {
        log.info("=== VirtualExecutorService grouped submit demo ===");
        VirtualGroupSubmitExample.SubmitObservation observed = submitExample.submitCallable(DEMO_PAYLOAD);
        if (!DEMO_PAYLOAD.equals(observed.getPayload())
                || !isDatabaseVirtualThread(observed.getThreadName())
                || !VirtualGroupSubmitExample.GROUP.equals(observed.getGroupName())) {
            throw new IllegalStateException("grouped submit demo failed");
        }
        log.info("submit demo ok, payload={}, workerThread={}, group={}",
                observed.getPayload(), observed.getThreadName(), observed.getGroupName());
    }

    private void runCancelDemo() {
        log.info("=== VirtualExecutorService managed cancel demo ===");
        VirtualCancelExample.CancelObservation observed = cancelExample.cancelManagedSupply();
        if (!observed.isCancelRequested() || !observed.isFutureCancelled()) {
            throw new IllegalStateException("managed cancel demo failed");
        }
        log.info("cancel demo ok, requested={}, futureCancelled={}",
                observed.isCancelRequested(), observed.isFutureCancelled());
    }

    private void runRejectDemo() {
        log.info("=== VirtualExecutorService REJECT backpressure demo ===");
        VirtualRejectExample.RejectObservation observed = rejectExample.rejectWhenCapacityFull();
        if (!VirtualRejectExample.GROUP.equals(observed.getGroup())
                || observed.getReason() != VirtualTaskRejectReason.CAPACITY_FULL
                || observed.getRejectedDelta() < 1L) {
            throw new IllegalStateException("reject backpressure demo failed");
        }
        log.info("reject demo ok, group={}, reason={}, rejectedDelta={}",
                observed.getGroup(), observed.getReason(), observed.getRejectedDelta());
    }

    private static boolean isDatabaseVirtualThread(String threadName) {
        return threadName != null && threadName.startsWith("peach-vt-database-");
    }
}
