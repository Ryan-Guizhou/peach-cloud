package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;

/**
 * 调度处理器外部客户端降级工厂。
 * <p>调度模块说明。
 * 调度模块说明。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/29 17:42
 */
@Indexed
public class SchedulerHandlerExternalClientFallbackFactory
        implements FallbackFactory<SchedulerHandlerExternalClient> {

    private static final Logger log = LoggerFactory.getLogger(SchedulerHandlerExternalClientFallbackFactory.class);

    /**
     * 接口实现。
     */
    @Override
    public SchedulerHandlerExternalClient create(Throwable cause) {
        final String errorType = cause == null ? "unknown" : cause.getClass().getName();
        log.warn("Scheduler handler registration client entered fallback, errorType={}", errorType);
        return request -> Response.fail("Scheduler handler registration is unavailable");
    }
}
