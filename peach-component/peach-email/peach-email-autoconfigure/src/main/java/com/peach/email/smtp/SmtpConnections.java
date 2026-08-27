package com.peach.email.smtp;

import java.util.concurrent.atomic.AtomicReference;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:25
 */
public final class SmtpConnections {

    private SmtpConnections() {
        throw new IllegalStateException("Utility class");
    }

    private static final AtomicReference<SmtpConnectionProvider> PROVIDER =
            new AtomicReference<>(new SimpleSmtpConnectionProvider());

    public static SmtpConnectionProvider getProvider() {
        return PROVIDER.get();
    }

    public static void setProvider(SmtpConnectionProvider provider) {
        PROVIDER.set(provider);
    }
}
