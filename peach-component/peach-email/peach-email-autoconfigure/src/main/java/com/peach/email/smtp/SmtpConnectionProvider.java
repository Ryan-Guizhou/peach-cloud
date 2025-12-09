package com.peach.email.smtp;

import com.peach.email.core.EmailContext;

import javax.mail.Session;
import javax.mail.Transport;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:25
 * @Description 连接提供者：决定获取与释放连接的策略
 */
public interface SmtpConnectionProvider {
    /** 获取已连接的 Transport（可能为新建或复用） */
    Transport acquire(Session session, EmailContext context) throws Exception;
    /** 释放 Transport（可选择关闭或保留） */
    void release(Transport transport);
}

