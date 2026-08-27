package com.peach.email.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:41
 */
public final class EmailConstant {

    private EmailConstant() {
        throw new IllegalStateException("Utility class");
    }

    public static final Integer DEFAULT_MAX_ATTEMPTS = 3;

    public static final Long DEFAULT_BASE_DELAY_MILLIS = 200L;

    public static final Long DEFAULT_DURATION_MILLIS = 0L;

    public static final String DEFAULT_FAILED_MESSAGE = "failed";

    public static final String MAIL_STMP_AUTH = "mail.smtp.auth";

    public static final String DEFAULT_MAIL_STMP_AUTH = "true";

    public static final String MAIL_STMP_HOST = "mail.smtp.host";

    public static final String MAIL_STMP_PORT = "mail.smtp.port";

    public static final String MAIL_STMP_SSL_ENABLE = "mail.smtp.ssl.enable";

    public static final String DEFAULT_MAIL_STMP_SSL_ENABLE = "true";

    public static final String MAIL_STMP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    public static final String DEFAULT_MAIL_STMP_STARTTLS_ENABLE = "true";

    public static final String MAIL_STMP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";

    public static final String DEFAULT_MAIL_STMP_CONNECTIONTIMEOUT = "10000";

    public static final String MAIL_STMP_TIMEOUT = "mail.smtp.timeout";

    public static final String DEFAULT_MAIL_STMP_TIMEOUT = "30000";

    public static final String MAIL_STMP_WRITETIMEOUE = "mail.smtp.writetimeout";

    public static final String DEFAULT_MAIL_STMP_WRITETIMEOUE = "30000";

    public static final String TEMPLATES_PATH = "templates";

    public static final String TEMPLATES_ENCODING = "UTF-8";


}
