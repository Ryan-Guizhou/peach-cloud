package com.peach.openfeign.support;

import com.peach.openfeign.exception.PeachFeignCircuitOpenException;
import com.peach.openfeign.exception.PeachFeignRateLimitedException;
import com.peach.openfeign.exception.PeachFeignRemoteException;
import com.peach.openfeign.exception.PeachFeignTimeoutException;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.codec.ErrorDecoder;

/**
 * PeachOpenFeignErrorDecoder相关类。
 * <p>将下游 HTTP 错误转换为模块统一异常，并对配置允许重试的状态码抛出
 * Feign 标准 {@link feign.RetryableException} 交给重试器处理。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
public class PeachOpenFeignErrorDecoder implements ErrorDecoder {

    private final PeachOpenfeignRetryPolicy retryPolicy;

    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public PeachOpenFeignErrorDecoder(PeachOpenfeignRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        Request request = response.request();
        String method = request == null || request.httpMethod() == null ? null : request.httpMethod().name();
        Exception classified = classify(methodKey, response, status);
        if (retryPolicy.canRetryStatus(method, status)) {
            return new PeachFeignRetryableException(status, response.reason(),
                    request == null ? null : request.httpMethod(), request, classified);
        }
        return classified;
    }

    private Exception classify(String methodKey, Response response, int status) {
        if (status == 429) {
            return new PeachFeignRateLimitedException(resolveClientName(methodKey), methodKey,
                    "Feign remote call rate limited", FeignException.errorStatus(methodKey, response));
        }
        if (status == 408) {
            return new PeachFeignTimeoutException(resolveClientName(methodKey), methodKey,
                    "Feign remote call timeout", FeignException.errorStatus(methodKey, response));
        }
        if (status == 503 || status == 504) {
            return new PeachFeignCircuitOpenException(resolveClientName(methodKey), methodKey,
                    "Feign remote call unavailable", FeignException.errorStatus(methodKey, response));
        }
        Exception decoded = defaultDecoder.decode(methodKey, response);
        return new PeachFeignRemoteException(resolveClientName(methodKey), methodKey, status, response.reason(),
                "Feign remote call failed", decoded);
    }

    private String resolveClientName(String methodKey) {
        if (methodKey == null || methodKey.isBlank()) {
            return "unknown";
        }
        int index = methodKey.indexOf('#');
        return index < 0 ? methodKey : methodKey.substring(0, index);
    }
}
