package com.peach.openfeign.exception;

/**
 * Feign 调用被 Sentinel 限流。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignRateLimitedException extends PeachFeignException {

    public PeachFeignRateLimitedException(String clientName, String methodKey, String message, Throwable cause) {
        super(clientName, methodKey, message, cause);
    }
}
