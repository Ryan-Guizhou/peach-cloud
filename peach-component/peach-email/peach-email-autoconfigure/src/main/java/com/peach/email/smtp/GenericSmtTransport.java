package com.peach.email.smtp;

import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.SendResult;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:55
 */
public class GenericSmtTransport extends AbstractTransport{

    public GenericSmtTransport() {
        super("GenericSmtpTransport");
    }

    @Override
    public SendResult send(EmailMessage emailMessage, EmailContext emailContext) {
        return super.send(emailMessage, emailContext);
    }
}
