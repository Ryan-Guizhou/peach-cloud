package com.peach.openfeign.exception;

/**
 * PeachFeignRemote异常。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/8/12 15:30
 */
public class PeachFeignRemoteException extends PeachFeignException {

    private final int status;

    private final String reason;

    public PeachFeignRemoteException(String clientName, String methodKey, int status, String reason, String message, Throwable cause) {
        super(clientName, methodKey, message, cause);
        this.status = status;
        this.reason = reason;
    }

    public int getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }
}
