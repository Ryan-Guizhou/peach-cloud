package com.peach.code.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务编码生成器配置。
 *
 * <p>唯一发号权威为 MySQL。保留已有 Redis 配置字段，以便现有配置文件平滑迁移；
 * 这些字段不再参与编码分配，不能用来切换发号权威。</p>
 *
 * @Author Mr Shu
 * @Version 1.0.0
 * @CreateTime 2026/3/20 16:58
 */
@ConfigurationProperties(prefix = "peach.code")
public class CodeProperties {

    /** 是否启用业务编码自动配置。 */
    private boolean enabled = true;

    /** 历史配置项：兼容现有配置文件，但不启用 Redis 发号。 */
    @Deprecated
    private boolean redisEnabled = true;

    /** 历史 Redis 序列前缀配置；不再用于发号。 */
    @Deprecated
    private String redisKeyPrefix = "peach:code:generate:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** @return 历史 Redis 开关；不影响实际分配策略。 */
    @Deprecated
    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    /** @param redisEnabled 历史配置值；不影响实际分配策略。 */
    @Deprecated
    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    /** @return 历史 Redis 前缀；不影响实际分配策略。 */
    @Deprecated
    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    /** @param redisKeyPrefix 历史 Redis 前缀；不影响实际分配策略。 */
    @Deprecated
    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
