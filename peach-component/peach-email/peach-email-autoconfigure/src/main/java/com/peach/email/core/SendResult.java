package com.peach.email.core;

/**
 * 发送结果。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:13
 */
public class SendResult {
    /**
     * 实际使用的提供商名称
     */
    private final String provider;

    /**
     * 邮件的 Message-ID（可能由服务器生成）
     */
    private final String messageId;

    /**
     * 整体耗时（毫秒）
     */
    private final long durationMillis;

    /**
     * 是否发送成功
     */
    private final boolean success;

    /**
     * 错误信息（失败时非空）
     */
    private final String error;

    /** 失败结果所处的外部投递边界；老构造器的失败默认为未知。 */
    private final EmailFailureKind failureKind;

    public SendResult(String provider, String messageId, long durationMillis, boolean success, String error) {
        this(provider, messageId, durationMillis, success, error,
                success ? null : EmailFailureKind.DELIVERY_UNKNOWN);
    }

    /**
     * 创建带明确失败阶段的结果，兼容原有五参数构造器。
     *
     * @param provider 发送 Provider
     * @param messageId 邮件 ID
     * @param durationMillis 耗时
     * @param success 是否确认成功
     * @param error 脱敏错误信息
     * @param failureKind 失败阶段；成功时允许为空
     */
    public SendResult(String provider, String messageId, long durationMillis, boolean success,
                      String error, EmailFailureKind failureKind) {
        this.provider = provider;
        this.messageId = messageId;
        this.durationMillis = durationMillis;
        this.success = success;
        this.error = error;
        this.failureKind = success ? null
                : failureKind == null ? EmailFailureKind.DELIVERY_UNKNOWN : failureKind;
    }

    public EmailFailureKind getFailureKind() {
        return failureKind;
    }

    public String getProvider() {
        return provider;
    }

    public String getMessageId() {
        return messageId;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getError() {
        return error;
    }
}
