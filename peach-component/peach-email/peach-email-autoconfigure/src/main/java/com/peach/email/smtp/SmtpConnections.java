package com.peach.email.smtp;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 18:25
 */
public final class SmtpConnections {

    private static volatile SmtpConnectionProvider provider = new SimpleSmtpConnectionProvider();

    private SmtpConnections() {}

    public static SmtpConnectionProvider getProvider() {
        return provider;
    }

    public static void setProvider(SmtpConnectionProvider provider) {
        SmtpConnections.provider = provider;
    }
}
