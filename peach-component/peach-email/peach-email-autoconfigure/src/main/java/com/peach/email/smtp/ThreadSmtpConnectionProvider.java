package com.peach.email.smtp;

import com.peach.email.core.EmailContext;
import lombok.extern.slf4j.Slf4j;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;

/**
 * 线程安全连接提供者。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:31
 * @Description 线程安全连接提供者
 */
@Slf4j
public class ThreadSmtpConnectionProvider implements SmtpConnectionProvider{

    private final ThreadLocal<Holder> threadLocal = new ThreadLocal<Holder>();

    private static final String SEPARATOR_COLON = ":";

    @Override
    public Transport acquire(Session session, EmailContext context) throws MessagingException {
        String key = buildKey(context);
        Holder h = threadLocal.get();
        if (h == null) {
            h = new Holder();
            threadLocal.set(h);
        }
        if (h.transport != null) {
            resetTransportIfStale(h, key);
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

    /**
     * 清理当前线程持有的 SMTP 连接，避免 ThreadLocal 泄漏。
     */
    public void clearThreadState() {
        Holder holder = threadLocal.get();
        if (holder != null && holder.transport != null) {
            try {
                holder.transport.close();
            } catch (Exception ignored) {
                log.debug("Failed to close SMTP transport during thread cleanup", ignored);
            }
        }
        threadLocal.remove();
    }

    private void resetTransportIfStale(Holder holder, String key) {
        boolean same = key.equals(holder.key);
        try {
            if (!same || !holder.transport.isConnected()) {
                closeTransportQuietly(holder.transport);
                holder.transport = null;
            }
        } catch (Exception ignored) {
            holder.transport = null;
        }
    }

    private void closeTransportQuietly(Transport transport) {
        try {
            transport.close();
        } catch (Exception ignored) {
            log.error("Failed to close SMTP transport", ignored);
        }
    }

    /**
     * Holder。
     *
     * @Author Mr Shu
     * @Version 1.0.0
     * @CreateTime 2026/3/20 16:58
     */

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
                .append(SEPARATOR_COLON)
                .append(context.getPort())
                .append(SEPARATOR_COLON)
                .append(context.getUsername());
        return String.valueOf(stringBuilder);
    }
}
