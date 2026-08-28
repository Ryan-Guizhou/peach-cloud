package com.peach.enums;

/**
 * 存储结果代码枚举。
 * <p>响应码用于业务层统一判断结果，也用于 {@code StorageException} 标记错误类型。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/6/18 14:15
 */
public enum StorageResultCode {

    /**
     * 操作成功。
     */
    SUCCESS("00000", "success"),

    /**
     * 请求参数不合法，例如 objectKey 为空或路径越界。
     */
    BAD_REQUEST("A0400", "bad request"),

    /**
     * 找不到指定存储 provider。
     */
    PROVIDER_NOT_FOUND("A0404", "storage provider not found"),

    /**
     * 存储桶不存在或不可访问。
     */
    BUCKET_NOT_FOUND("B0404", "bucket not found"),

    /**
     * 对象不存在。
     */
    OBJECT_NOT_FOUND("C0404", "object not found"),

    /**
     * 当前 provider 不支持该能力。
     */
    UNSUPPORTED_OPERATION("C0405", "unsupported operation"),

    /**
     * 访问被拒绝，通常由凭证或 ACL 配置导致。
     */
    ACCESS_DENIED("C0403", "access denied"),

    /**
     * 调用厂商 SDK 或本地文件系统失败。
     */
    STORAGE_ERROR("C0500", "storage error");

    private final String code;

    private final String message;

    StorageResultCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
