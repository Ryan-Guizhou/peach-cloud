package com.peach.common.response;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/11 9:51
 */
public enum StatusEnum {


    SUCCESS("200", "成功"),

    FAIL("500", "服务器内部错误"),

    BUSINESS_FAIL_CODE("400", "业务处理失败"),

    NOT_FOUND("404", "未找到"),

    UNAUTHORIZED("401", "未授权"),

    FORBIDDEN("403", "禁止访问"),

    NOT_SUPPORTED("405", "不支持"),

    REQUEST_TIMEOUT("408", "请求超时"),

    STATUS_BIZ_NEED_CONFIRM("409", "需要前台再次确认"),

    GONE("410", "已删除"),

    LENGTH_REQUIRED("411", "长度不够"),

    PRECONDITION_FAILED("412", "条件不符合"),

    UNSUPPORTED_MEDIA_TYPE("415", "不支持的媒体类型"),

    TOO_MANY_REQUESTS("429", "请求过于频繁");

    private final String code;

    private final String message;

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    StatusEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

}
