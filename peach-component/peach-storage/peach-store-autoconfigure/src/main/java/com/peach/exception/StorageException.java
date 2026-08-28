package com.peach.exception;

import com.peach.enums.StorageResultCode;

/**
 * 存储异常。
 * <p>provider 适配厂商 SDK 时，应将 SDK 异常转换为该异常或其子类，避免业务层
 * 直接依赖不同云厂商的异常类型。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public class StorageException extends RuntimeException {

    private final StorageResultCode code;

    public StorageException(StorageResultCode code, String message) {
        super(message);
        this.code = code;
    }

    public StorageException(StorageResultCode code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public StorageResultCode getCode() {
        return code;
    }
}
