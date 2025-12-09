package com.peach.email.core;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:13
 * @Description: 发送结果：包含提供商名称、Message-ID、耗时与错误信息。
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

    public SendResult(String provider, String messageId, long durationMillis, boolean success, String error) {
        this.provider = provider;
        this.messageId = messageId;
        this.durationMillis = durationMillis;
        this.success = success;
        this.error = error;
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
