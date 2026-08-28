package com.peach.rocket.exception;

/**
 * MQ异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/26
 */
public class MqException extends RuntimeException {

    private static final long serialVersionUID = -6166678259118358198L;

    /**
     * 创建 MQ 异常。
     *
     * @param message 异常信息
     */
    public MqException(String message) {
        super(message);
    }

    /**
     * 创建 MQ 异常。
     *
     * @param message 异常信息
     * @param cause 原始异常
     */
    public MqException(String message, Throwable cause) {
        super(message, cause);
    }
}
