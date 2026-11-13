package com.peach.transaction.config;

import com.peach.transaction.api.TransactionMode;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Peach 全局事务接入配置。默认关闭，不影响既有业务服务。
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/11/13 11:30
 */
@ConfigurationProperties(prefix = "peach.transaction")
public class PeachTransactionProperties {

    private boolean enabled = false;
    private String provider = "seata";
    private TransactionMode mode = TransactionMode.AT;
    private int timeoutMillis = 60_000;
    private boolean allowAtOutboxCoexistence = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public TransactionMode getMode() {
        return mode;
    }

    public void setMode(TransactionMode mode) {
        this.mode = mode;
    }

    public int getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(int timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public boolean isAllowAtOutboxCoexistence() {
        return allowAtOutboxCoexistence;
    }

    public void setAllowAtOutboxCoexistence(boolean allowAtOutboxCoexistence) {
        this.allowAtOutboxCoexistence = allowAtOutboxCoexistence;
    }
}
