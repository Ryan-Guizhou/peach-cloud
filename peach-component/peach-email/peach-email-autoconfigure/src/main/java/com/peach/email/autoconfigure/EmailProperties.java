package com.peach.email.autoconfigure;

import com.peach.email.constant.EmailConstant;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2025/12/9 15:13
 * @Description 邮件配置属性
 */
@ConfigurationProperties(prefix = "peach.email")
public class EmailProperties {

    /**
     * 默认提供商（选填，推荐）用于自动发送时的首选路由名称，例如：qq/gmail/ali/163
     */
    private String defaultProvider;

    /**
     * 提供商配置（必填至少一个）。key 为提供商名称（如 qq/gmail/ali/163 或自定义），value 为账号与连接参数
     */
    private Map<String, Provider> providers = new HashMap<String, Provider>();

    /**
     * 重试策略（选填），不配置则采用默认：最多3次、200ms指数退避
     */
    private Retry retry = new Retry();

    /** 默认提供商名称，自动发送时优先尝试该提供商 */
    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers;
    }

    public Retry getRetry() {
        return retry;
    }

    public void setRetry(Retry retry) {
        this.retry = retry;
    }

    public static class Provider {

        /**
         * 认证用户名（邮箱地址，必填）
         */
        private String username;

        /**
         * 邮件服务授权码或密码（必填）
         */
        private String password;

        /**
         * SMTP 主机名（选填，缺省为内置提供商默认 host）
         */
        private String host;

        /**
         * SMTP 端口（选填，缺省为 465/SSL 或 25）
         */
        private Integer port;

        /**
         * 是否启用 SSL（选填，缺省 true；为 false 时启用 STARTTLS）
        */
        private Boolean ssl;

        /**
         * 路由优先级（选填），值越小越优先
         */
        private Integer priority;

        public String getUsername() {
            return username; }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public Boolean getSsl() {
            return ssl;
        }

        public void setSsl(Boolean ssl) {
            this.ssl = ssl;
        }

        public Integer getPriority() {
            return priority;
        }

        public void setPriority(Integer priority) {
            this.priority = priority;
        }
    }

    public static class Retry {
        /**
         * 最大尝试次数（选填，默认3）
         */
        private Integer maxAttempts = EmailConstant.DEFAULT_MAX_ATTEMPTS;

        /**
         * 基准延迟毫秒（选填，默认200），指数退避：delay = base * 2^(attempt-1)
         */
        private Long baseDelayMillis = EmailConstant.DEFAULT_BASE_DELAY_MILLIS;

        public Integer getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public Long getBaseDelayMillis() {
            return baseDelayMillis;
        }

        public void setBaseDelayMillis(Long baseDelayMillis) {
            this.baseDelayMillis = baseDelayMillis;
        }
    }
}
