package com.peach.email.smtp;

import com.peach.email.core.EmailContext;
import lombok.extern.slf4j.Slf4j;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:27
 * @Description 简单的连接提供者
 */
@Slf4j
public class SimpleSmtpConnectionProvider implements SmtpConnectionProvider{
    @Override
    public Transport acquire(Session session, EmailContext context) throws Exception {
        // 简单实现，直接返回新的连接
        Transport transport = session.getTransport("smtp");
        transport.connect(
                context.getHost(),
                context.getPort(),
                context.getUsername(),
                context.getPassword()
        );
        return transport;
    }

    @Override
    public void release(Transport transport) {
        if (transport == null){
            return;
        }
        try {
            transport.close();
        } catch (Exception e) {
            log.error("Failed to close SMTP transport", e);
        }
    }
}
