package com.peach.scheduler.quickstart.example;

import com.peach.scheduler.quickstart.config.DemoExecutionLeaseClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;
import org.springframework.stereotype.Service;

/**
 * 证明 {@code ExecutionLeaseClient.claim} 可允许或拒绝，且拒绝与 Handler 执行解耦。
 *
 * <p>Claim 不能替代业务幂等；本样例只验证门闩开关本身。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
@Service
@RequiredArgsConstructor
public class ClaimGateExample {

    /**
     * 与 {@code peach.scheduler.executor.instance-id} 对齐的演示实例标识。
     */
    public static final String INSTANCE_ID = "qs-scheduler-instance";

    private final DemoExecutionLeaseClient leaseClient;

    /**
     * 先允许再拒绝同一套 Claim 桩，返回两次结果。
     *
     * @return 允许与拒绝两次 claim 的观察结果
     */
    public ClaimObservation evaluateAllowThenReject() {
        leaseClient.setAllow(true);
        boolean allowed = leaseClient.claim("qs-claim-allow", INSTANCE_ID);
        leaseClient.setAllow(false);
        boolean rejectedAttempt = leaseClient.claim("qs-claim-reject", INSTANCE_ID);
        log.info("claim gate, allowed={}, secondGranted={}", allowed, rejectedAttempt);
        if (!allowed || rejectedAttempt) {
            throw new IllegalStateException("claim gate demo failed");
        }
        return new ClaimObservation(allowed, rejectedAttempt);
    }

    /**
     * Claim 门闩两次调用的观察结果。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/9/11 19:00
     */
    public static final class ClaimObservation {

        private final boolean firstGranted;
        private final boolean secondGranted;

        public ClaimObservation(boolean firstGranted, boolean secondGranted) {
            this.firstGranted = firstGranted;
            this.secondGranted = secondGranted;
        }

        public boolean isFirstGranted() {
            return firstGranted;
        }

        public boolean isSecondGranted() {
            return secondGranted;
        }
    }
}
