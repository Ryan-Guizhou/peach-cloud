package com.peach.email.smtp;

import com.peach.common.util.StringUtil;
import com.peach.email.core.EmailContext;
import lombok.extern.slf4j.Slf4j;
import javax.mail.Session;
import javax.mail.Transport;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 线程安全连接提供者
 */
@Slf4j
public class ThreadSmtpConnectionProvider implements SmtpConnectionProvider{

    private final ThreadLocal<Holder> threadLocal = new ThreadLocal<Holder>();

    @Override
    public Transport acquire(Session session, EmailContext context) throws Exception {
        String key = buildKey(context);
        Holder h = threadLocal.get();
        if (h == null) {
            h = new Holder();
            threadLocal.set(h);
        }
        if (h.transport != null) {
            boolean same = key.equals(h.key);
            try {
                if (!same || !h.transport.isConnected()) {
                    try {
                        h.transport.close();
                    } catch (Exception ignored) {
                        log.error("Failed to close SMTP transport", ignored);
                    }
                    h.transport = null;
                }
            } catch (Exception ignored) {
                h.transport = null;
            }
        }
        if (h.transport == null) {
            Transport t = session.getTransport("smtp");
            t.connect(context.getHost(), context.getPort(), context.getUsername(), context.getPassword());
            h.transport = t;
            h.key = key;
        }
        return h.transport;
    }

    @Override
    public void release(Transport transport) {/* 保留连接，不立即关闭 */}

    private static class Holder {
        Transport transport;
        String key;
    }

    /**
     * 构建key，用于线程本地存储
     * @param context 邮件上下文
     * @return
     */
    private String buildKey(EmailContext context) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(context.getHost())
                .append(StringUtil.SEPARATOR_COLON)
                .append(context.getPort())
                .append(StringUtil.SEPARATOR_COLON)
                .append(context.getUsername());
        return String.valueOf(stringBuilder);
    }
}
