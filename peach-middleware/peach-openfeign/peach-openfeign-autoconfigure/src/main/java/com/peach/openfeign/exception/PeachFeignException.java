package com.peach.openfeign.exception;

/**
 * Peach OpenFeign 统一异常基类。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignException extends RuntimeException {

    private final String clientName;

    private final String methodKey;

    public PeachFeignException(String clientName, String methodKey, String message) {
        super(message);
        this.clientName = clientName;
        this.methodKey = methodKey;
    }

    public PeachFeignException(String clientName, String methodKey, String message, Throwable cause) {
        super(message, cause);
        this.clientName = clientName;
        this.methodKey = methodKey;
    }

    public String getClientName() {
        return clientName;
    }

    public String getMethodKey() {
        return methodKey;
    }
}
