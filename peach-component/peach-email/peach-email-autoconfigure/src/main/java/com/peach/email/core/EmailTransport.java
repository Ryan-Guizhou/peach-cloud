package com.peach.email.core;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:44
 * @Description:
 * 传输接口：定义具体提供商的发送实现（如 SMTP 或云API）
 */
public interface EmailTransport {

    String getName();

    SendResult send(EmailMessage emailMessage, EmailContext emailContext);

}
