package com.peach.code.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 业务编码生成器配置。
 *
 * <p>默认实现 Redis 优先发号，Redis 不可用时使用 MySQL 原子自增兜底；关闭 Redis 后可切换为
 * MySQL 独立事务发号模式。</p>
 */
@ConfigurationProperties(prefix = "peach.code")
public class CodeProperties {

    /** 是否启用业务编码自动配置。 */
    private boolean enabled = true;

    /** 是否启用 Redis 优先发号及故障回写。 */
    private boolean redisEnabled = true;

    /** Redis 序列 key 的命名空间前缀。 */
    private String redisKeyPrefix = "peach:code:generate:";

    /**
     * 判断是否启用业务编码自动配置。
     *
     * @return {@code true} 表示启用
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 设置是否启用业务编码自动配置。
     *
     * @param enabled {@code true} 表示启用
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 判断是否启用 Redis 主发号路径。
     *
     * @return {@code true} 表示启用 Redis 主路径和回写
     */
    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    /**
     * 设置是否启用 Redis 主发号路径。
     *
     * @param redisEnabled {@code true} 表示启用 Redis 主路径和回写
     */
    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    /**
     * 获取 Redis 序列键命名空间前缀。
     *
     * @return Redis 序列键命名空间前缀
     */
    public String getRedisKeyPrefix() {
        return redisKeyPrefix;
    }

    /**
     * 设置 Redis 序列键命名空间前缀。
     *
     * @param redisKeyPrefix Redis 序列键命名空间前缀
     */
    public void setRedisKeyPrefix(String redisKeyPrefix) {
        this.redisKeyPrefix = redisKeyPrefix;
    }
}
