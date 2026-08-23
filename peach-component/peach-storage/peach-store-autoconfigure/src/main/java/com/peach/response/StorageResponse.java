package com.peach.response;

import com.peach.enums.StorageResultCode;


/**
 * 存储操作统一响应对象。
 *
 * @param <T> 响应数据类型
 * @author Mr Shu
 * @version 1.0.0
 * @since 2026/6/16 14:01
 */
public class StorageResponse<T> {

    /**
     * 请求是否成功。
     */
    private final boolean success;

    /**
     * 业务响应码。
     */
    private final String code;

    /**
     * 响应消息。
     */
    private final String message;

    /**
     * 响应数据。
     */
    private final T data;

    private StorageResponse(boolean success, String code, String message, T data) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 响应数据类型
     * @return 成功响应
     */
    public static <T> StorageResponse<T> success(T data) {
        return new StorageResponse<>(true, StorageResultCode.SUCCESS.getCode(),
                StorageResultCode.SUCCESS.getMessage(), data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 业务响应码
     * @param message 响应消息；为空时使用响应码默认消息
     * @param <T> 响应数据类型
     * @return 失败响应
     */
    public static <T> StorageResponse<T> failure(StorageResultCode code, String message) {
        String responseMessage = message == null || message.isBlank() ? code.getMessage() : message;
        return new StorageResponse<>(false, code.getCode(), responseMessage, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
