package com.peach.email.core;

/**
 * SMTP 发送失败所处的投递阶段，用于控制是否允许重试和 Provider 故障转移。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/11 13:31
 */
public enum EmailFailureKind {

    /** 已明确尚未提交邮件，连接类临时故障允许重新尝试。 */
    RETRYABLE_BEFORE_SEND,

    /** 已明确尚未提交邮件，但参数或认证失败不应重试。 */
    PERMANENT_BEFORE_SEND,

    /** 已开始 SMTP 投递，异常无法证明服务器是否接收，必须停止自动重复发送。 */
    DELIVERY_UNKNOWN
}
