package com.peach.openfeign.support;

import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.openfeign.exception.PeachFeignCircuitOpenException;
import com.peach.openfeign.exception.PeachFeignRateLimitedException;
import com.peach.openfeign.exception.PeachFeignRetryExhaustedException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import lombok.extern.slf4j.Slf4j;

/**
 * Feign 降级工厂通用支持组件。
 *
 * <p>业务 fallbackFactory 可复用该组件统一记录降级日志，并按 Sentinel 限流、
 * 熔断降级、超时、重试耗尽等原因返回基础响应。日志只记录 client、method
 * 与异常类型，不记录请求体、token 或敏感字段。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Slf4j
public class PeachFeignFallbackSupport {

    /**
     * 记录降级并返回通用失败响应。
     *
     * @param client Feign 客户端名称
     * @param method Feign 方法名
     * @param cause 降级原因
     * @return 统一失败响应
     */
    public Response fail(String client, String method, Throwable cause) {
        String causeType = resolveCause(cause);
        log.warn("[PeachFeign] fallback triggered client={} method={} cause={}",
                client, method, causeType);
        if (isRateLimited(cause)) {
            return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(),
                    StatusEnum.TOO_MANY_REQUESTS.getMessage());
        }
        return Response.fail();
    }

    /**
     * 判断降级原因是否为 Sentinel 限流。
     *
     * @param cause 降级原因
     * @return 限流时返回 {@code true}
     */
    public boolean isRateLimited(Throwable cause) {
        return cause instanceof PeachFeignRateLimitedException
                || hasCauseType(cause, "FlowException");
    }

    /**
     * 判断降级原因是否为熔断、慢调用、超时或重试耗尽。
     *
     * @param cause 降级原因
     * @return 服务不可用类降级时返回 {@code true}
     */
    public boolean isUnavailable(Throwable cause) {
        return cause instanceof PeachFeignCircuitOpenException
                || cause instanceof PeachFeignTimeoutException
                || cause instanceof PeachFeignRetryExhaustedException
                || hasCauseType(cause, "DegradeException")
                || hasCauseType(cause, "AuthorityException")
                || hasCauseType(cause, "SystemBlockException");
    }

    private String resolveCause(Throwable cause) {
        return cause == null ? "unknown" : cause.getClass().getSimpleName();
    }

    private boolean hasCauseType(Throwable cause, String simpleName) {
        Throwable current = cause;
        while (current != null) {
            if (simpleName.equals(current.getClass().getSimpleName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
