package com.peach.sample.email.smtp;

import com.peach.email.smtp.AbstractTransport;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:54
 * @Description 无限邮箱实现
 */
public class InfiniteSmtpTransport extends AbstractTransport {

    public InfiniteSmtpTransport() {
        super("inf");
    }

}