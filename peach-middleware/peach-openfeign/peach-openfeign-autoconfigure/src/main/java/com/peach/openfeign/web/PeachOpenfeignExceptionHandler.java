package com.peach.openfeign.web;

import com.peach.common.response.Response;
import com.peach.common.response.StatusEnum;
import com.peach.openfeign.config.PeachOpenfeignProperties;
import com.peach.openfeign.constant.PeachOpenfeignConstants;
import com.peach.openfeign.exception.PeachFeignCircuitOpenException;
import com.peach.openfeign.exception.PeachFeignException;
import com.peach.openfeign.exception.PeachFeignRateLimitedException;
import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignRetryExhaustedException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import com.peach.openfeign.exception.FeignSameTokenException;
import com.peach.openfeign.exception.FeignUploadSizeLimitException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.stereotype.Indexed;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * OpenFeign 异常统一响应处理器。
 *
 * <p>负责将 Feign 调用链路中的远端错误、限流熔断、重试耗尽与安全校验失败，
 * 统一转换为对前端可消费的标准响应，避免把内部调用细节直接暴露给调用方。</p>
 *
 * <p>该处理器只负责响应映射与日志分级，不改变 Feign 异常分类规则；
 * 异常分类由 error decoder、retryer 与 sentinel 处理链负责。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
@Slf4j
@Indexed
@RestControllerAdvice
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnClass(name = "feign.RequestInterceptor")
@ConditionalOnProperty(prefix = "peach.openfeign", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PeachOpenfeignExceptionHandler {

    private final PeachOpenfeignProperties properties;

    public PeachOpenfeignExceptionHandler(PeachOpenfeignProperties properties) {
        this.properties = properties;
    }

    /**
     * 处理远端 HTTP 错误异常。
     *
     * @param exception 远端错误异常
     * @return 统一响应
     */
    @ExceptionHandler(PeachFeignRemoteException.class)
    public Response handlePeachFeignRemoteException(PeachFeignRemoteException exception) {
        logRemoteException(exception);
        return responseByStatus(exception.getStatus(), exception.getReason());
    }

    /**
     * 处理限流异常并返回 429 语义响应。
     *
     * @param exception 限流异常
     * @return 统一响应
     */
    @ExceptionHandler(PeachFeignRateLimitedException.class)
    public Response handlePeachFeignRateLimitedException(PeachFeignRateLimitedException exception) {
        log.warn("[PeachFeign] remote call rate limited client={} method={}",
                exception.getClientName(), exception.getMethodKey());
        return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), StatusEnum.TOO_MANY_REQUESTS.getMessage());
    }

    /**
     * 处理远端不可用类异常（熔断、超时、重试耗尽）。
     *
     * @param exception 服务不可用异常
     * @return 统一响应
     */
    @ExceptionHandler({PeachFeignCircuitOpenException.class, PeachFeignTimeoutException.class, PeachFeignRetryExhaustedException.class})
    public Response handlePeachFeignUnavailableException(PeachFeignException exception) {
        if (properties.getException().isLogStacktraceFor5xx()) {
            log.error("[PeachFeign] remote call unavailable client={} method={} type={}",
                    exception.getClientName(), exception.getMethodKey(), exception.getClass().getSimpleName(), exception);
        } else {
            log.error("[PeachFeign] remote call unavailable client={} method={} type={}",
                    exception.getClientName(), exception.getMethodKey(), exception.getClass().getSimpleName());
        }
        return Response.fail();
    }

    /**
     * 处理 Same-Token 缺失异常。
     *
     * @param exception same-token 异常
     * @return 统一响应
     */
    @ExceptionHandler(FeignSameTokenException.class)
    public Response handleFeignSameTokenException(FeignSameTokenException exception) {
        log.error("[PeachFeign] same-token missing for internal call");
        return Response.businessResponse(StatusEnum.UNAUTHORIZED.getCode(), StatusEnum.UNAUTHORIZED.getMessage());
    }

    /**
     * 处理上传大小超限异常。
     *
     * @param exception 上传大小异常
     * @return 统一响应
     */
    @ExceptionHandler(FeignUploadSizeLimitException.class)
    public Response handleFeignUploadSizeLimitException(FeignUploadSizeLimitException exception) {
        log.warn("[PeachFeign] request rejected by upload size limit path={} contentLength={} maxBytes={}",
                exception.getRequestUrl(),
                exception.getContentLength(),
                exception.getMaxBytes());
        return Response.businessResponse(PeachOpenfeignConstants.MESSAGE_UPLOAD_TOO_LARGE);
    }

    private void logRemoteException(PeachFeignRemoteException exception) {
        if (exception.getStatus() >= 500 && properties.getException().isLogStacktraceFor5xx()) {
            log.error("[PeachFeign] remote server error status={} client={} method={}",
                    exception.getStatus(), exception.getClientName(), exception.getMethodKey(), exception);
            return;
        }
        if (exception.getStatus() >= 400 && properties.getException().isLogStacktraceFor4xx()) {
            log.warn("[PeachFeign] remote client error status={} client={} method={}",
                    exception.getStatus(), exception.getClientName(), exception.getMethodKey(), exception);
            return;
        }
        log.warn("[PeachFeign] remote call failed status={} client={} method={}",
                exception.getStatus(), exception.getClientName(), exception.getMethodKey());
    }

    private Response responseByStatus(int status, String remoteMessage) {
        if (status == 401) {
            return Response.businessResponse(StatusEnum.UNAUTHORIZED.getCode(), StatusEnum.UNAUTHORIZED.getMessage());
        }
        if (status == 403) {
            return Response.businessResponse(StatusEnum.FORBIDDEN.getCode(), StatusEnum.FORBIDDEN.getMessage());
        }
        if (status == 404) {
            return Response.businessResponse(StatusEnum.NOT_FOUND.getCode(), StatusEnum.NOT_FOUND.getMessage());
        }
        if (status == 408) {
            return Response.fail(StatusEnum.REQUEST_TIMEOUT);
        }
        if (status == 429) {
            return Response.businessResponse(StatusEnum.TOO_MANY_REQUESTS.getCode(), StatusEnum.TOO_MANY_REQUESTS.getMessage());
        }
        if (status >= 500) {
            return Response.fail();
        }
        if (properties.getException().isExposeRemoteMessage() && remoteMessage != null && !remoteMessage.isBlank()) {
            return Response.businessResponse(remoteMessage);
        }
        return Response.businessResponse(StatusEnum.BUSINESS_FAIL_CODE.getMessage());
    }
}
