package com.peach.scheduled.external;

import org.springframework.stereotype.Indexed;

import com.peach.common.response.Response;
import com.peach.scheduled.dto.HandlerRegistrationDTO;
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
public class SchedulerHandlerExternalClientFallbackFactory
        implements FallbackFactory<SchedulerHandlerExternalClient> {

    private static final Logger log = LoggerFactory.getLogger(SchedulerHandlerExternalClientFallbackFactory.class);

    /**
     * 创建相关对象。
     */
    public SchedulerHandlerExternalClientFallbackFactory() {
    }

    /**
     * 继承接口定义。
     */
    @Override
    public SchedulerHandlerExternalClient create(Throwable cause) {
        final String errorType = cause == null ? "unknown" : cause.getClass().getName();
        log.warn("Scheduler handler registration client entered fallback, errorType={}", errorType);
        return new SchedulerHandlerExternalClient() {
            /**
             * 继承接口定义。
             */
            @Override
            public Response register(HandlerRegistrationDTO request) {
                return Response.fail("Scheduler handler registration is unavailable");
            }
        };
    }
}
