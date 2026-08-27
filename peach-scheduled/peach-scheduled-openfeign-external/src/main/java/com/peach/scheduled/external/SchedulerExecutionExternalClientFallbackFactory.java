package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 调度模块相关说明。
 *
 * <p>调度模块相关说明。
 * 调度模块相关说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerExecutionExternalClientFallbackFactory
        implements FallbackFactory<SchedulerExecutionExternalClient> {

    private static final Logger log = LoggerFactory.getLogger(SchedulerExecutionExternalClientFallbackFactory.class);

    /**
     * 创建相关对象。
     */
    public SchedulerExecutionExternalClientFallbackFactory() {
        // Intentionally empty.
    }

    /**
     * 继承接口定义。
     */
    @Override
    public SchedulerExecutionExternalClient create(Throwable cause) {
        final String errorType = cause == null ? "unknown" : cause.getClass().getName();
        log.warn("Scheduler execution lease client entered fallback, errorType={}", errorType);
        return (executionId, request) -> Response.fail("Scheduler execution lease is unavailable");
    }
}
