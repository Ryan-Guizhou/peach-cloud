package com.peach.scheduler.quickstart.config;

import com.peach.scheduler.transport.ExecutionLeaseClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Indexed;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 本地 Claim 桩：不访问生产控制面，可切换允许 / 拒绝以证明执行前租约门闩。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/9/11 19:00
 */
@Slf4j
@Indexed
public class DemoExecutionLeaseClient implements ExecutionLeaseClient {

    private final AtomicBoolean allow = new AtomicBoolean(true);
    private final AtomicInteger claimAttempts = new AtomicInteger();

    /**
     * 按当前开关返回是否抢占成功；拒绝时执行器不得进入 Handler。
     */
    @Override
    public boolean claim(String executionId, String executorInstance) {
        int attempts = claimAttempts.incrementAndGet();
        boolean granted = allow.get();
        log.info("demo claim, executionId={}, granted={}, attempts={}", executionId, granted, attempts);
        return granted;
    }

    /**
     * 设置下一次及之后 claim 是否允许。
     *
     * @param allow {@code true} 允许抢占
     */
    public void setAllow(boolean allow) {
        this.allow.set(allow);
    }

    /**
     * @return 已调用 claim 的次数
     */
    public int claimAttempts() {
        return claimAttempts.get();
    }

    /**
     * 恢复为允许并清空计数。
     */
    public void reset() {
        allow.set(true);
        claimAttempts.set(0);
    }
}
