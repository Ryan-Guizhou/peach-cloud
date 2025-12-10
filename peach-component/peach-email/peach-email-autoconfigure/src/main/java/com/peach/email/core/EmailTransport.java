package com.peach.email.core;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description:
 * 传输接口：定义具体提供商的发送实现（如 SMTP 或云API）
 */
public interface EmailTransport {

    /**
     * 获取邮件发送提供商名称
     * @return
     */
    String getName();

    /**
     * 发送邮件
     * @param emailMessage 业务邮件模型
     * @param emailContext 传输上下文
     * @return
     */
    SendResult send(EmailMessage emailMessage, EmailContext emailContext);

}
