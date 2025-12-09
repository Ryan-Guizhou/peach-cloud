package com.peach.email.constant;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:41
 */
public interface EmailConstant {

    Integer DEFAULT_MAX_ATTEMPTS = 3;

    Long DEFAULT_BASE_DELAY_MILLIS = 200L;

    Long DEFAULT_DURATION_MILLIS = 0L;

    String DEFAULT_FAILED_MESSAGE = "failed";

    String MAIL_STMP_AUTH = "mail.smtp.auth";

    String DEFAULT_MAIL_STMP_AUTH = "true";

    String MAIL_STMP_HOST = "mail.smtp.host";

    String MAIL_STMP_PORT = "mail.smtp.port";

    String MAIL_STMP_SSL_ENABLE = "mail.smtp.ssl.enable";

    String DEFAULT_MAIL_STMP_SSL_ENABLE = "true";

    String MAIL_STMP_STARTTLS_ENABLE = "mail.smtp.starttls.enable";

    String DEFAULT_MAIL_STMP_STARTTLS_ENABLE = "true";

    String MAIL_STMP_CONNECTIONTIMEOUT = "mail.smtp.connectiontimeout";

    String DEFAULT_MAIL_STMP_CONNECTIONTIMEOUT = "10000";

    String MAIL_STMP_TIMEOUT = "mail.smtp.timeout";

    String DEFAULT_MAIL_STMP_TIMEOUT = "30000";

    String MAIL_STMP_WRITETIMEOUE = "mail.smtp.writetimeout";

    String DEFAULT_MAIL_STMP_WRITETIMEOUE = "30000";


}
