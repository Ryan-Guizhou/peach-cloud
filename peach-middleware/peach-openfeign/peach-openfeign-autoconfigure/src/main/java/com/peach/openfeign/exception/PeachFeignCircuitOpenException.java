package com.peach.openfeign.exception;

/**
 * Feign 调用被 Sentinel 熔断降级。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignCircuitOpenException extends PeachFeignException {

    public PeachFeignCircuitOpenException(String clientName, String methodKey, String message, Throwable cause) {
        super(clientName, methodKey, message, cause);
    }
}
