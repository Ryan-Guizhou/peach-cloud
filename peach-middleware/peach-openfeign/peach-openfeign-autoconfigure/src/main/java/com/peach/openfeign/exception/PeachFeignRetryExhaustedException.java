package com.peach.openfeign.exception;

/**
 * PeachFeign重试Exhausted异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignRetryExhaustedException extends PeachFeignException {

    public PeachFeignRetryExhaustedException(String clientName, String methodKey, String message, Throwable cause) {
        super(clientName, methodKey, message, cause);
    }
}
