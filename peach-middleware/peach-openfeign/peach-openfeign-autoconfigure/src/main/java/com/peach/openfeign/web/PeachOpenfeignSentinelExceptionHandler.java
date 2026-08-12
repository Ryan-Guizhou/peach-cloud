package com.peach.openfeign.web;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Sentinel 阻断异常统一响应处理器。
 *
 * <p>负责将 Sentinel 抛出的 block/degrade 异常映射为统一前端响应：
 * 流控异常返回 429，其余阻断异常返回通用失败响应。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Slf4j
@Indexed
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(BlockException.class)
@ConditionalOnProperty(prefix = "peach.openfeign.sentinel", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachOpenfeignSentinelExceptionHandler {

    /**
     * 处理 Sentinel 阻断异常并映射标准响应。
     *
     * @param exception Sentinel 阻断异常
     * @return 统一响应
     */
    @ExceptionHandler(BlockException.class)
    public Response handleBlockException(BlockException exception) {
        String type = exception.getClass().getSimpleName();
        log.warn("[PeachFeign] sentinel blocked remote call type={}", type);
        if ("FlowException".equals(type)) {
            return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), StatusEnum.TOO_MANY_REQUESTS.getMessage());
        }
        if ("DegradeException".equals(type)) {
            return Response.fail("服务熔断降级，请稍后重试");
        }
        return Response.fail();
    }
}
