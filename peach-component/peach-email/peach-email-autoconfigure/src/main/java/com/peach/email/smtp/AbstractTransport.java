package com.peach.email.smtp;

import com.peach.email.build.MimeMessageBuilder;
import com.peach.email.constant.EmailConstant;
import com.peach.email.core.EmailContext;
import com.peach.email.core.EmailMessage;
import com.peach.email.core.EmailTransport;
import com.peach.email.core.SendResult;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:47
 */
public abstract class AbstractTransport implements EmailTransport {

    private final String name;

    protected AbstractTransport(String name) {
        this.name = name;
    }

    public String getName() { return name; }


    public SendResult send(EmailMessage emailMessage, EmailContext context){
        long start = System.currentTimeMillis();
        try {
            Properties props = new Properties();
            props.put(EmailConstant.MAIL_STMP_AUTH, EmailConstant.DEFAULT_MAIL_STMP_AUTH);
            props.put(EmailConstant.MAIL_STMP_HOST, context.getHost());
            props.put(EmailConstant.MAIL_STMP_PORT, Integer.toString(context.getPort()));
            if (context.isSsl()){
                props.put(EmailConstant.MAIL_STMP_SSL_ENABLE,EmailConstant.DEFAULT_MAIL_STMP_SSL_ENABLE);
            }else {
                props.put(EmailConstant.MAIL_STMP_STARTTLS_ENABLE, EmailConstant.DEFAULT_MAIL_STMP_STARTTLS_ENABLE);
            }
            if (!props.containsKey(EmailConstant.MAIL_STMP_CONNECTIONTIMEOUT)) {
                props.put(EmailConstant.MAIL_STMP_CONNECTIONTIMEOUT, EmailConstant.DEFAULT_MAIL_STMP_CONNECTIONTIMEOUT);
            }
            if (!props.containsKey(EmailConstant.MAIL_STMP_TIMEOUT)) {
                props.put(EmailConstant.MAIL_STMP_TIMEOUT, EmailConstant.DEFAULT_MAIL_STMP_TIMEOUT);
            }
            if (!props.containsKey(EmailConstant.MAIL_STMP_WRITETIMEOUE)) {
                props.put(EmailConstant.MAIL_STMP_WRITETIMEOUE, EmailConstant.DEFAULT_MAIL_STMP_WRITETIMEOUE);
            }
            if (context.getExtra() != null) {
                props.putAll(context.getExtra());
            }
            Session session = Session.getInstance(props);
            MimeMessage mime = MimeMessageBuilder.build(session, emailMessage);
            Transport transport = SmtpConnections.getProvider().acquire(session, context);
            transport.sendMessage(mime, mime.getAllRecipients());
            SmtpConnections.getProvider().release(transport);
            long dur = System.currentTimeMillis() - start;
            String mid = mime.getMessageID();
            return new SendResult(name, mid, dur, true, null);
        } catch (Exception e) {
            long dur = System.currentTimeMillis() - start;
            return new SendResult(name, null, dur, false, e.getMessage());
        }
    }
}
