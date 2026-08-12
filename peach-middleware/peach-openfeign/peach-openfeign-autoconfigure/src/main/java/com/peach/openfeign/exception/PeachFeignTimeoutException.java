package com.peach.openfeign.exception;

/**
 * Feign 调用超时。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignTimeoutException extends PeachFeignException {

    public PeachFeignTimeoutException(String clientName, String methodKey, String message, Throwable cause) {
        super(clientName, methodKey, message, cause);
    }
}
